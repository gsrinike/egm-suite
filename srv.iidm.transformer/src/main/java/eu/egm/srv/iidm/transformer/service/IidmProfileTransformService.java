package eu.egm.srv.iidm.transformer.service;

import com.infra.InfrastructureUtils;
import com.infra.event.EventPublisherService;
import com.infra.storage.document.DocumentFilter;
import com.infra.storage.document.DocumentPage;
import com.infra.storage.document.DocumentRepositoryService;
import com.infra.storage.document.DocumentSearchRequest;
import com.utils.restservice.RestServiceSupport;
import com.utils.profile.ProfileDefaults;
import com.utils.profile.ProfileDefaultsService;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import eu.egm.data.cnm.common.DynamicTableColumn;
import eu.egm.data.cnm.common.DynamicTableDefinition;
import eu.egm.data.cnm.common.DynamicTableRow;
import eu.egm.data.cnm.common.ProfilePayload;
import eu.egm.data.iidm.common.IidmProfileTransformCompleted;
import eu.egm.data.iidm.common.IidmProfileTransformFailed;
import eu.egm.data.iidm.common.IidmProfileTransformRequested;
import eu.egm.data.iidm.common.IidmTransformState;
import eu.egm.data.iidm.network.IidmNetworkModel;
import eu.egm.data.iidm.network.IidmNetworkSummary;
import eu.egm.data.iidm.network.IidmNetworkXiidm;
import eu.egm.map.cnm.iidm.CnmToIidmMappingConfiguration;
import eu.egm.map.cnm.iidm.CnmToIidmTransformer;
import eu.egm.mapping.JsonMappingService;
import eu.egm.mapping.ReflectionMappingService;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadReadDocument;
import eu.egm.srv.iidm.transformer.domain.CnmProfilePayloadReadDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocument;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocument.IidmElementCountDocument;
import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocumentAdapter;
import eu.egm.srv.iidm.transformer.domain.IidmProfileTransformDocument;
import eu.egm.srv.iidm.transformer.domain.IidmProfileTransformDocumentAdapter;
import eu.egm.srv.iidm.transformer.api.IidmElementCountResponse;
import eu.egm.srv.iidm.transformer.api.IidmNetworkSummaryResponse;
import eu.egm.srv.iidm.transformer.api.IidmPage;
import eu.egm.srv.iidm.transformer.api.IidmTableBundle;
import eu.egm.srv.iidm.transformer.api.IidmTransformSummaryResponse;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class IidmProfileTransformService extends RestServiceSupport {
    private static final int NETWORK_XIIDM_CHUNK_SIZE = 1_000_000;

    private final DocumentRepositoryService<CnmProfilePayloadReadDocument> sourcePayloadRepository;
    private final DocumentRepositoryService<IidmProfileTransformDocument> transformRepository;
    private final DocumentRepositoryService<IidmNetworkDocument> networkRepository;
    private final EventPublisherService eventPublisher;
    private final JsonMappingService jsonMappingService;
    private final CnmToIidmTransformer transformer;
    private final String eventExchange;
    private final String completedRoutingKey;
    private final String failedRoutingKey;

    public IidmProfileTransformService(
            Environment environment,
            ObservationRegistry observationRegistry,
            InfrastructureUtils infrastructureUtils,
            JsonMappingService jsonMappingService,
            @Value("${iidm.transform.event.exchange:iidm.events}") String eventExchange,
            @Value("${iidm.transform.event.completed-routing-key:iidm.profile.transform.completed}") String completedRoutingKey,
            @Value("${iidm.transform.event.failed-routing-key:iidm.profile.transform.failed}") String failedRoutingKey) {
        super(environment, observationRegistry);
        this.sourcePayloadRepository = infrastructureUtils.documentRepository(new CnmProfilePayloadReadDocumentAdapter());
        this.transformRepository = infrastructureUtils.documentRepository(new IidmProfileTransformDocumentAdapter());
        this.networkRepository = infrastructureUtils.documentRepository(new IidmNetworkDocumentAdapter());
        this.eventPublisher = infrastructureUtils.eventPublisher();
        this.jsonMappingService = jsonMappingService;
        this.transformer = new CnmToIidmTransformer(new ReflectionMappingService(), iidmMappingConfiguration());
        this.eventExchange = eventExchange;
        this.completedRoutingKey = completedRoutingKey;
        this.failedRoutingKey = failedRoutingKey;
    }

    private CnmToIidmMappingConfiguration iidmMappingConfiguration() {
        ProfileDefaults defaults = new ProfileDefaultsService().load("iidm", "defaults");
        return new CnmToIidmMappingConfiguration(
                defaults.doubleValue("iidm.defaults.nominal-voltage", 400.0),
                defaults.stringValue("iidm.defaults.substation-id", "EGM_DEFAULT_SUBSTATION"),
                defaults.stringValue("iidm.defaults.substation-name", "Default Substation"),
                defaults.stringValue("iidm.defaults.voltage-level-id", "EGM_DEFAULT_VL"),
                defaults.stringValue("iidm.defaults.voltage-level-name", "Default Voltage Level"),
                defaults.stringValue("iidm.defaults.bus-id", "EGM_DEFAULT_BUS"),
                defaults.stringValue("iidm.defaults.bus-name", "Default Bus"),
                defaults.doubleValue("iidm.defaults.line-x", 0.0001));
    }

    public void transform(IidmProfileTransformRequested request) {
        if (request == null) {
            throw new IllegalArgumentException("IIDM transform request is required");
        }
        long startedAt = Instant.now().toEpochMilli();
        String networkId = networkId(request.importId(), request.fileId());
        transformRepository.save(new IidmProfileTransformDocument(
                request.fileId(),
                request.importId(),
                request.fileId(),
                request.profileType(),
                request.profileFamily(),
                request.sourceProfilePayloadId(),
                IidmTransformState.STARTED,
                "IIDM transformation started",
                List.of(),
                networkId,
                startedAt,
                null,
                null));
        try {
            CnmProfilePayloadReadDocument sourcePayload = sourcePayload(request.sourceProfilePayloadId(), request.importId());
            ProfilePayload<?> profilePayload = jsonMappingService.fromJson(profileJson(sourcePayload), ProfilePayload.class);
            IidmNetworkModel network = transformer.transform(
                    profilePayload,
                    request.importId(),
                    request.businessDay(),
                    request.businessTime(),
                    request.timeFrame(),
                    request.tsoName());
            networkRepository.save(toNetworkDocument(network));
            long completedAt = Instant.now().toEpochMilli();
            transformRepository.save(new IidmProfileTransformDocument(
                    request.fileId(),
                    request.importId(),
                    request.fileId(),
                    request.profileType(),
                    request.profileFamily(),
                    request.sourceProfilePayloadId(),
                    IidmTransformState.DONE,
                    "IIDM transformation completed",
                    network.diagnostics(),
                    network.id(),
                    startedAt,
                    completedAt,
                    null));
            eventPublisher.publish(eventExchange, completedRoutingKey,
                    new IidmProfileTransformCompleted(request.importId(), request.fileId(), network.id(), "DONE"));
        } catch (Exception exception) {
            long failedAt = Instant.now().toEpochMilli();
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            transformRepository.save(new IidmProfileTransformDocument(
                    request.fileId(),
                    request.importId(),
                    request.fileId(),
                    request.profileType(),
                    request.profileFamily(),
                    request.sourceProfilePayloadId(),
                    IidmTransformState.FAILED,
                    message,
                    List.of(),
                    networkId,
                    startedAt,
                    null,
                    failedAt));
            eventPublisher.publish(eventExchange, failedRoutingKey,
                    new IidmProfileTransformFailed(request.importId(), request.fileId(), message));
        }
    }

    public List<IidmProfileTransformDocument> transforms(String importId, int maxResults) {
        if (importId == null || importId.isBlank()) {
            return transformRepository.findAll(maxResults, new com.infra.storage.document.DocumentSort(
                    "startedAt",
                    com.infra.storage.document.DocumentSort.Direction.DESC));
        }
        return transformRepository.findByField("importId", importId, maxResults);
    }

    public IidmPage<IidmTransformSummaryResponse> transformSummaries(String importId, String search, int page, int size) {
        DocumentPage<IidmProfileTransformDocument> documents = transformRepository.search(new DocumentSearchRequest(
                filters(importId),
                iidmTransformSearchFilters(search),
                page,
                size));
        return new IidmPage<>(
                documents.content().stream().map(this::toTransformSummary).toList(),
                documents.total(),
                documents.page(),
                documents.size());
    }

    public IidmProfileTransformDocument transformByFileId(String fileId) {
        return transformRepository.findByField("id", fileId, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("IIDM transform not found: " + fileId));
    }

    public List<IidmNetworkDocument> networks(String importId, int maxResults) {
        if (importId == null || importId.isBlank()) {
            return networkRepository.findAll(maxResults, new com.infra.storage.document.DocumentSort(
                    "updatedAt",
                    com.infra.storage.document.DocumentSort.Direction.DESC));
        }
        return networkRepository.findByField("importId", importId, maxResults);
    }

    public IidmPage<IidmNetworkSummaryResponse> networkSummaries(String importId, int page, int size) {
        DocumentPage<IidmNetworkDocument> documents = networkRepository.search(new DocumentSearchRequest(
                filters(importId),
                List.of(),
                List.of(),
                List.of("networkXiidm", "networkXiidmChunks"),
                page,
                size));
        return new IidmPage<>(
                documents.content().stream().map(this::toNetworkSummary).toList(),
                documents.total(),
                documents.page(),
                documents.size());
    }

    public IidmNetworkDocument network(String networkId) {
        return networkRepository.findByField("id", networkId, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("IIDM network not found: " + networkId));
    }

    public IidmTableBundle networkTableMetadata(String networkId) {
        IidmNetworkDocument document = network(networkId);
        return tableBundle(document, "", 0, 0, true);
    }

    public IidmTableBundle networkTablesForFile(String importId, String fileId) {
        IidmProfileTransformDocument transform = transformByFileId(fileId);
        if (!transform.importId().equals(importId)) {
            throw new IllegalArgumentException("IIDM transform not found for import/file: " + importId + "/" + fileId);
        }
        return networkTableMetadata(transform.iidmNetworkId());
    }

    public IidmTableBundle networkTable(String networkId, String tableId, int page, int size, String search) {
        IidmNetworkDocument document = network(networkId);
        return tableBundle(document, tableId, page, size, false, search);
    }

    private CnmProfilePayloadReadDocument sourcePayload(String payloadId, String importId) {
        return sourcePayloadRepository.findByField("id", payloadId, 1)
                .stream()
                .filter(payload -> payload.importId().equals(importId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CNM profile payload not found: " + payloadId));
    }

    private IidmNetworkDocument toNetworkDocument(IidmNetworkModel network) {
        String networkXiidm = IidmNetworkXiidm.write(network.network());
        IidmNetworkSummary summary = network.summary();
        long now = Instant.now().toEpochMilli();
        return new IidmNetworkDocument(
                network.id(),
                network.importId(),
                network.sourceFileIds(),
                network.businessDay(),
                network.businessTime(),
                network.timeFrame(),
                network.tsoName(),
                IidmNetworkXiidm.FORMAT,
                networkXiidm.length() <= NETWORK_XIIDM_CHUNK_SIZE ? networkXiidm : "",
                chunks(networkXiidm),
                List.of(
                        new IidmElementCountDocument("substations", summary.substationCount()),
                        new IidmElementCountDocument("voltageLevels", summary.voltageLevelCount()),
                        new IidmElementCountDocument("buses", summary.busCount()),
                        new IidmElementCountDocument("lines", summary.lineCount()),
                        new IidmElementCountDocument("generators", summary.generatorCount()),
                        new IidmElementCountDocument("loads", summary.loadCount()),
                        new IidmElementCountDocument("switches", summary.switchCount()),
                        new IidmElementCountDocument("busbarSections", summary.busbarSectionCount())),
                now,
                now);
    }

    private String profileJson(CnmProfilePayloadReadDocument payload) {
        if (payload.profileJson() != null && !payload.profileJson().isBlank()) {
            return payload.profileJson();
        }
        return String.join("", payload.profileJsonChunks());
    }

    private List<String> chunks(String value) {
        if (value == null || value.length() <= NETWORK_XIIDM_CHUNK_SIZE) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int index = 0; index < value.length(); index += NETWORK_XIIDM_CHUNK_SIZE) {
            chunks.add(value.substring(index, Math.min(index + NETWORK_XIIDM_CHUNK_SIZE, value.length())));
        }
        return chunks;
    }

    private String networkId(String importId, String fileId) {
        return importId + ":" + fileId;
    }

    private List<DocumentFilter> filters(String importId) {
        if (importId == null || importId.isBlank()) {
            return List.of();
        }
        return List.of(DocumentFilter.exact("importId", importId));
    }

    private List<DocumentFilter> iidmTransformSearchFilters(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        String query = search.trim();
        return List.of(
                DocumentFilter.contains("fileId", query),
                DocumentFilter.contains("profileType", query),
                DocumentFilter.contains("profileFamily", query),
                DocumentFilter.contains("transformState", query),
                DocumentFilter.contains("transformMessage", query),
                DocumentFilter.contains("iidmNetworkId", query));
    }

    private IidmTransformSummaryResponse toTransformSummary(IidmProfileTransformDocument document) {
        return new IidmTransformSummaryResponse(
                document.id(),
                document.importId(),
                document.fileId(),
                document.profileType(),
                document.profileFamily(),
                document.transformState(),
                document.transformMessage(),
                document.diagnostics().size(),
                document.iidmNetworkId(),
                document.startedAt(),
                document.completedAt(),
                document.failedAt());
    }

    private IidmNetworkSummaryResponse toNetworkSummary(IidmNetworkDocument document) {
        return new IidmNetworkSummaryResponse(
                document.id(),
                document.importId(),
                document.sourceFileIds(),
                document.businessDay(),
                document.businessTime(),
                document.timeFrame(),
                document.tsoName(),
                document.networkFormat(),
                document.elementCounts().stream()
                        .map(count -> new IidmElementCountResponse(count.elementType(), count.count()))
                        .toList(),
                document.createdAt(),
                document.updatedAt());
    }

    private IidmTableBundle tableBundle(IidmNetworkDocument document, String selectedTableId, int page, int size, boolean metadataOnly) {
        return tableBundle(document, selectedTableId, page, size, metadataOnly, "");
    }

    private IidmTableBundle tableBundle(
            IidmNetworkDocument document,
            String selectedTableId,
            int page,
            int size,
            boolean metadataOnly,
            String search) {
        int resolvedSize = metadataOnly ? 0 : Math.min(Math.max(size, 1), 500);
        int resolvedPage = Math.max(page, 0);
        List<IidmTableSpec> specs = metadataOnly
                ? iidmTableMetadataSpecs(document)
                : iidmTableSpecs(document, selectedTableId);
        List<DynamicTableDefinition> tables = specs.stream()
                .map(spec -> {
                    if (metadataOnly || !spec.tableId().equals(selectedTableId)) {
                        return new DynamicTableDefinition(
                                spec.tableId(),
                                spec.label(),
                                spec.columns(),
                                List.of(),
                                spec.totalRows(),
                                spec.defaultSort());
                    }
                    List<Map<String, Object>> rows = spec.rows().stream()
                            .filter(row -> matches(row, search))
                            .toList();
                    return table(spec.tableId(), spec.label(), spec.columns(), rows, resolvedPage, resolvedSize, spec.defaultSort());
                })
                .toList();
        return new IidmTableBundle(
                document.importId(),
                document.id(),
                document.sourceFileIds().isEmpty() ? "" : document.sourceFileIds().getFirst(),
                selectedTableId,
                resolvedPage,
                resolvedSize,
                tables);
    }

    private List<IidmTableSpec> iidmTableSpecs(IidmNetworkDocument document, String selectedTableId) {
        IidmTableSpec selectedSpec = iidmSelectedTableSpec(document, selectedTableId);
        return iidmTableMetadataSpecs(document).stream()
                .map(spec -> spec.tableId().equals(selectedTableId) ? selectedSpec : spec)
                .toList();
    }

    private IidmTableSpec iidmSelectedTableSpec(IidmNetworkDocument document, String selectedTableId) {
        return switch (selectedTableId) {
            case "network-summary", "element-counts", "source-files" -> iidmTableDataSpecs(document).stream()
                    .filter(spec -> spec.tableId().equals(selectedTableId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown IIDM table: " + selectedTableId));
            case "substations" -> withNetwork(document, "substations", "Substations", substationColumns(),
                    network -> stream(network.getSubstations()).stream().map(this::substationRow).toList());
            case "voltage-levels" -> withNetwork(document, "voltage-levels", "Voltage levels", voltageLevelColumns(),
                    network -> stream(network.getVoltageLevels()).stream().map(this::voltageLevelRow).toList());
            case "buses" -> withNetwork(document, "buses", "Buses", busColumns(),
                    network -> stream(network.getBusBreakerView().getBuses()).stream().map(this::busRow).toList());
            case "lines" -> withNetwork(document, "lines", "Lines", lineColumns(),
                    network -> stream(network.getLines()).stream().map(this::lineRow).toList());
            case "generators" -> withNetwork(document, "generators", "Generators", generatorColumns(),
                    network -> stream(network.getGenerators()).stream().map(this::generatorRow).toList());
            case "loads" -> withNetwork(document, "loads", "Loads", loadColumns(),
                    network -> stream(network.getLoads()).stream().map(this::loadRow).toList());
            case "switches" -> withNetwork(document, "switches", "Switches", switchColumns(),
                    network -> stream(network.getSwitches()).stream().map(this::switchRow).toList());
            case "busbar-sections" -> withNetwork(document, "busbar-sections", "Busbar sections", busbarSectionColumns(),
                    network -> stream(network.getBusbarSections()).stream().map(this::busbarSectionRow).toList());
            default -> throw new IllegalArgumentException("Unknown IIDM table: " + selectedTableId);
        };
    }

    private IidmTableSpec withNetwork(
            IidmNetworkDocument document,
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            Function<Network, List<Map<String, Object>>> rows) {
        return new IidmTableSpec(tableId, label, columns, rows.apply(IidmNetworkXiidm.read(networkXiidm(document))), "id");
    }

    private List<IidmTableSpec> iidmTableMetadataSpecs(IidmNetworkDocument document) {
        List<IidmTableSpec> specs = iidmTableDataSpecs(document).stream()
                .map(spec -> spec.withoutRows())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        specs.add(new IidmTableSpec("substations", "Substations", substationColumns(), List.of(), "id", count(document, "substations")));
        specs.add(new IidmTableSpec("voltage-levels", "Voltage levels", voltageLevelColumns(), List.of(), "id", count(document, "voltageLevels")));
        specs.add(new IidmTableSpec("buses", "Buses", busColumns(), List.of(), "id", count(document, "buses")));
        specs.add(new IidmTableSpec("lines", "Lines", lineColumns(), List.of(), "id", count(document, "lines")));
        specs.add(new IidmTableSpec("generators", "Generators", generatorColumns(), List.of(), "id", count(document, "generators")));
        specs.add(new IidmTableSpec("loads", "Loads", loadColumns(), List.of(), "id", count(document, "loads")));
        specs.add(new IidmTableSpec("switches", "Switches", switchColumns(), List.of(), "id", count(document, "switches")));
        specs.add(new IidmTableSpec("busbar-sections", "Busbar sections", busbarSectionColumns(), List.of(), "id", count(document, "busbarSections")));
        return specs;
    }

    private List<IidmTableSpec> iidmTableDataSpecs(IidmNetworkDocument document) {
        List<IidmTableSpec> specs = new ArrayList<>();
        specs.add(new IidmTableSpec(
                "network-summary",
                "Network summary",
                columns("field", "Field", "value", "Value"),
                List.of(
                        row("Network ID", document.id()),
                        row("Import ID", document.importId()),
                        row("Source files", String.join(", ", document.sourceFileIds())),
                        row("TSO", document.tsoName()),
                        row("Business day", document.businessDay()),
                        row("Business time", document.businessTime()),
                        row("Timeframe", document.timeFrame()),
                        row("Format", document.networkFormat())),
                "field"));
        specs.add(new IidmTableSpec(
                "element-counts",
                "Element counts",
                columns("elementType", "Element type", "count", "Count"),
                document.elementCounts().stream()
                        .map(count -> map(
                                "elementType", count.elementType(),
                                "count", count.count()))
                        .toList(),
                "elementType"));
        specs.add(new IidmTableSpec(
                "source-files",
                "Source files",
                columns("fileId", "File ID"),
                document.sourceFileIds().stream().map(fileId -> map("fileId", fileId)).toList(),
                "fileId"));
        return specs;
    }

    private int count(IidmNetworkDocument document, String elementType) {
        return document.elementCounts().stream()
                .filter(count -> elementType.equals(count.elementType()))
                .mapToInt(count -> Math.toIntExact(count.count()))
                .findFirst()
                .orElse(0);
    }

    private String networkXiidm(IidmNetworkDocument document) {
        if (document.networkXiidm() != null && !document.networkXiidm().isBlank()) {
            return document.networkXiidm();
        }
        return String.join("", document.networkXiidmChunks());
    }

    private DynamicTableDefinition table(
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            List<Map<String, Object>> sourceRows,
            int page,
            int size,
            String defaultSort) {
        int from = Math.min(page * size, sourceRows.size());
        int to = Math.min(from + size, sourceRows.size());
        List<DynamicTableRow> rows = new ArrayList<>();
        for (int index = from; index < to; index++) {
            Map<String, Object> row = sourceRows.get(index);
            rows.add(new DynamicTableRow(String.valueOf(row.getOrDefault("id", tableId + "-" + index)), row));
        }
        return new DynamicTableDefinition(tableId, label, columns, rows, sourceRows.size(), defaultSort);
    }

    private boolean matches(Map<String, Object> row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase(Locale.ROOT);
        return row.values().stream().anyMatch(value -> String.valueOf(value).toLowerCase(Locale.ROOT).contains(query));
    }

    private Map<String, Object> substationRow(Substation substation) {
        return identifiableRow(substation,
                "country", substation.getCountry().map(Enum::name).orElse(""),
                "tso", substation.getTso());
    }

    private Map<String, Object> voltageLevelRow(VoltageLevel voltageLevel) {
        return identifiableRow(voltageLevel,
                "substationId", voltageLevel.getSubstation().map(Identifiable::getId).orElse(""),
                "nominalV", voltageLevel.getNominalV(),
                "lowVoltageLimit", voltageLevel.getLowVoltageLimit(),
                "highVoltageLimit", voltageLevel.getHighVoltageLimit(),
                "topologyKind", voltageLevel.getTopologyKind().name());
    }

    private Map<String, Object> busRow(Bus bus) {
        return identifiableRow(bus,
                "voltageLevelId", bus.getVoltageLevel().getId(),
                "v", bus.getV(),
                "angle", bus.getAngle(),
                "p", bus.getP(),
                "q", bus.getQ(),
                "connectedTerminalCount", bus.getConnectedTerminalCount());
    }

    private Map<String, Object> lineRow(Line line) {
        return identifiableRow(line,
                "voltageLevel1", voltageLevelId(line.getTerminal1()),
                "voltageLevel2", voltageLevelId(line.getTerminal2()),
                "r", line.getR(),
                "x", line.getX(),
                "g1", line.getG1(),
                "g2", line.getG2(),
                "b1", line.getB1(),
                "b2", line.getB2());
    }

    private Map<String, Object> generatorRow(Generator generator) {
        return identifiableRow(generator,
                "voltageLevelId", voltageLevelId(generator.getTerminal()),
                "energySource", generator.getEnergySource().name(),
                "minP", generator.getMinP(),
                "maxP", generator.getMaxP(),
                "targetP", generator.getTargetP(),
                "targetQ", generator.getTargetQ(),
                "targetV", generator.getTargetV(),
                "voltageRegulatorOn", generator.isVoltageRegulatorOn());
    }

    private Map<String, Object> loadRow(Load load) {
        return identifiableRow(load,
                "voltageLevelId", voltageLevelId(load.getTerminal()),
                "loadType", load.getLoadType().name(),
                "p0", load.getP0(),
                "q0", load.getQ0());
    }

    private Map<String, Object> switchRow(Switch networkSwitch) {
        return identifiableRow(networkSwitch,
                "voltageLevelId", networkSwitch.getVoltageLevel().getId(),
                "kind", networkSwitch.getKind().name(),
                "open", networkSwitch.isOpen(),
                "retained", networkSwitch.isRetained());
    }

    private Map<String, Object> busbarSectionRow(BusbarSection busbarSection) {
        return identifiableRow(busbarSection,
                "voltageLevelId", voltageLevelId(busbarSection.getTerminal()),
                "v", busbarSection.getV(),
                "angle", busbarSection.getAngle());
    }

    private Map<String, Object> identifiableRow(Identifiable<?> identifiable, Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", identifiable.getId());
        row.put("name", identifiable.getOptionalName().orElse(""));
        row.put("type", identifiable.getType().name());
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), values[index + 1]);
        }
        return row;
    }

    private String voltageLevelId(Terminal terminal) {
        return terminal == null || terminal.getVoltageLevel() == null ? "" : terminal.getVoltageLevel().getId();
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put(String.valueOf(values[index]), displayValue(values[index + 1]));
        }
        return row;
    }

    private Object displayValue(Object value) {
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "";
        }
        if (value instanceof Float number && !Float.isFinite(number)) {
            return "";
        }
        return value;
    }

    private Map<String, Object> row(String field, Object value) {
        return map("field", field, "value", value);
    }

    private List<DynamicTableColumn> substationColumns() {
        return columns("id", "ID", "name", "Name", "country", "Country", "tso", "TSO");
    }

    private List<DynamicTableColumn> voltageLevelColumns() {
        return columns("id", "ID", "name", "Name", "substationId", "Substation", "nominalV", "Nominal V",
                "lowVoltageLimit", "Low V", "highVoltageLimit", "High V", "topologyKind", "Topology");
    }

    private List<DynamicTableColumn> busColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "v", "V",
                "angle", "Angle", "p", "P", "q", "Q", "connectedTerminalCount", "Terminals");
    }

    private List<DynamicTableColumn> lineColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevel1", "Voltage level 1", "voltageLevel2", "Voltage level 2",
                "r", "R", "x", "X", "g1", "G1", "g2", "G2", "b1", "B1", "b2", "B2");
    }

    private List<DynamicTableColumn> generatorColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "energySource", "Energy source",
                "minP", "Min P", "maxP", "Max P", "targetP", "Target P", "targetQ", "Target Q",
                "targetV", "Target V", "voltageRegulatorOn", "Regulator");
    }

    private List<DynamicTableColumn> loadColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "loadType", "Load type",
                "p0", "P0", "q0", "Q0");
    }

    private List<DynamicTableColumn> switchColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "kind", "Kind",
                "open", "Open", "retained", "Retained");
    }

    private List<DynamicTableColumn> busbarSectionColumns() {
        return columns("id", "ID", "name", "Name", "voltageLevelId", "Voltage level", "v", "V", "angle", "Angle");
    }

    private List<DynamicTableColumn> columns(String... keyLabels) {
        List<DynamicTableColumn> columns = new ArrayList<>();
        for (int index = 0; index < keyLabels.length; index += 2) {
            columns.add(new DynamicTableColumn(keyLabels[index], keyLabels[index + 1], "string", true, true, ""));
        }
        return columns;
    }

    private <T> List<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }

    private record IidmTableSpec(
            String tableId,
            String label,
            List<DynamicTableColumn> columns,
            List<Map<String, Object>> rows,
            String defaultSort,
            int totalRows) {
        IidmTableSpec(String tableId, String label, List<DynamicTableColumn> columns, List<Map<String, Object>> rows, String defaultSort) {
            this(tableId, label, columns, rows, defaultSort, rows.size());
        }

        IidmTableSpec withoutRows() {
            return new IidmTableSpec(tableId, label, columns, List.of(), defaultSort, totalRows);
        }
    }
}

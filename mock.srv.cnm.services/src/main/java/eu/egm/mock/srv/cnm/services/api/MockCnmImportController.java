package eu.egm.mock.srv.cnm.services.api;

import eu.egm.data.cnm.common.CnmPage;
import eu.egm.data.cnm.common.ChunkUploadCompleteRequest;
import eu.egm.data.cnm.common.CnmProfileMetadata;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.DynamicTableBundle;
import eu.egm.data.cnm.common.DynamicTableColumn;
import eu.egm.data.cnm.common.DynamicTableDefinition;
import eu.egm.data.cnm.common.DynamicTableRow;
import eu.egm.data.cnm.common.ImportFailureRequest;
import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ImportFileStatus;
import eu.egm.data.cnm.common.ImportFileStatusUpdateRequest;
import eu.egm.data.cnm.common.ImportState;
import eu.egm.data.cnm.common.ImportStatus;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.RdfProfileReference;
import eu.egm.data.cnm.common.TimeFrame;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cnm/imports")
public class MockCnmImportController {
    private final List<ImportStatus> imports = new ArrayList<>();

    public MockCnmImportController() {
        imports.add(sample("sample-cgm", CnmServiceType.CGM, TimeFrame.DAY_AHEAD, ProfileFamily.CGMES, "Day-ahead CGM"));
        imports.add(sample("sample-ncp", CnmServiceType.CSA, TimeFrame.ID, ProfileFamily.NCP));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportStatus importModel(
            @RequestParam CnmServiceType serviceType,
            @RequestParam TimeFrame timeFrame,
            @RequestParam(required = false) String importId,
            @RequestParam(required = false) String message,
            @RequestPart("file") List<MultipartFile> file) {
        ImportStatus status = sample(
                importId == null || importId.isBlank() ? UUID.randomUUID().toString() : importId,
                serviceType,
                timeFrame,
                ProfileFamily.CGMES,
                message);
        imports.removeIf(item -> item.importId().equals(status.importId()));
        imports.add(0, status);
        return status;
    }

    @PostMapping(value = "/failures", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus reportFailure(@RequestBody ImportFailureRequest request) {
        Instant now = Instant.now();
        List<ImportFileStatus> files = request.fileNames().stream()
                .map(fileName -> new ImportFileStatus(
                        UUID.randomUUID().toString(),
                        fileName,
                        "",
                        ImportFileState.FAILED,
                        ProfileFamily.Unknown,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        List.of(),
                        request.message(),
                        now))
                .toList();
        ImportStatus status = new ImportStatus(
                request.importId(),
                request.serviceType(),
                request.timeFrame(),
                ImportState.FAILED,
                files,
                now,
                request.message());
        imports.removeIf(item -> item.importId().equals(status.importId()));
        imports.add(0, status);
        return status;
    }

    @PostMapping(value = "/chunks", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadChunk(@RequestBody byte[] chunk) {
        // The mock acknowledges bounded chunks; completion creates the sample import.
    }

    @PostMapping(value = "/chunks/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus completeChunkUpload(@RequestBody ChunkUploadCompleteRequest request) {
        ImportStatus status = sample(
                request.importId(),
                request.serviceType(),
                request.timeFrame(),
                ProfileFamily.CGMES,
                request.message());
        imports.removeIf(item -> item.importId().equals(status.importId()));
        imports.add(0, status);
        return status;
    }

    @GetMapping("/profiles")
    public CnmPage<CnmProfileMetadata> profiles() {
        List<CnmProfileMetadata> profiles = imports.stream()
                .flatMap(status -> status.files().stream()
                        .map(file -> new CnmProfileMetadata(
                                file.fileId(),
                                file.fileId(),
                                status.importId(),
                                file.fileName(),
                                file.objectId(),
                                file.state(),
                                file.profileFamily(),
                                file.profileType(),
                                file.tsoName(),
                                file.businessDay(),
                                file.businessTime(),
                                file.modelTimeFrame(),
                                file.modelVersion(),
                                file.uploadedAt())))
                .toList();
        return new CnmPage<>(profiles, profiles.size(), 0, profiles.size());
    }

    @GetMapping
    public CnmPage<ImportStatus> imports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        int from = Math.min(page * size, imports.size());
        int to = Math.min(from + size, imports.size());
        return new CnmPage<>(imports.subList(from, to), imports.size(), page, size);
    }

    @GetMapping("/{importId}")
    public ImportStatus importById(@PathVariable String importId) {
        return imports.stream()
                .filter(status -> status.importId().equals(importId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mock import not found: " + importId));
    }

    @GetMapping("/{importId}/files/{fileId}/profile/payload")
    public Map<String, Object> profilePayload(@PathVariable String importId, @PathVariable String fileId) {
        ImportFileStatus file = file(importId, fileId);
        return Map.of(
                "profileFamily", file.profileFamily(),
                "profileType", file.profileType(),
                "fileId", file.fileId(),
                "objectId", file.objectId(),
                "topologyObjects", List.of(row("mRID", "VL-1", "name", "Voltage Level 1", "objectType", "VoltageLevel")),
                "topologyRelations", List.of(row("sourceMRID", "T-1", "targetMRID", "CN-1", "relationType", "Terminal.ConnectivityNode")),
                "profile", Map.of(
                        "voltages", List.of(row("mRID", "SV-1", "v", 400.0, "angle", -1.2)),
                        "flows", List.of(row("mRID", "PF-1", "p", 120.0, "q", 18.5))));
    }

    @GetMapping("/{importId}/files/{fileId}/profile/tables")
    public DynamicTableBundle profileTables(@PathVariable String importId, @PathVariable String fileId) {
        ImportFileStatus file = file(importId, fileId);
        Map<String, Object> payload = profilePayload(importId, fileId);
        return new DynamicTableBundle(
                importId,
                fileId,
                file.profileType(),
                file.profileFamily(),
                payload,
                List.of(
                        table("topologyObjects", "Topology objects", List.of(row("mRID", "VL-1", "name", "Voltage Level 1", "objectType", "VoltageLevel"))),
                        table("voltages", "Voltages", List.of(row("mRID", "SV-1", "v", 400.0, "angle", -1.2))),
                        table("flows", "Flows", List.of(row("mRID", "PF-1", "p", 120.0, "q", 18.5)))));
    }

    @GetMapping("/{importId}/files/{fileId}/profile/tables/{tableId}")
    public DynamicTableDefinition profileTable(
            @PathVariable String importId,
            @PathVariable String fileId,
            @PathVariable String tableId) {
        return profileTables(importId, fileId).tables().stream()
                .filter(table -> table.tableId().equals(tableId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mock profile table not found: " + tableId));
    }

    @PutMapping(value = "/{importId}/files/{fileId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus updateFileStatus(
            @PathVariable String importId,
            @PathVariable String fileId,
            @RequestBody ImportFileStatusUpdateRequest request) {
        ImportStatus current = importById(importId);
        List<ImportFileStatus> files = current.files().stream()
                .map(file -> file.fileId().equals(fileId)
                        ? new ImportFileStatus(
                                file.fileId(),
                                file.fileName(),
                                file.objectId(),
                                request.state(),
                                file.profileFamily(),
                                file.businessDay(),
                                file.businessTime(),
                                file.modelTimeFrame(),
                                file.tsoName(),
                                file.profileType(),
                                file.modelVersion(),
                                file.profiles(),
                                request.message() == null || request.message().isBlank()
                                        ? file.message()
                                        : request.message(),
                                file.uploadedAt())
                        : file)
                .toList();
        ImportState state = files.stream().anyMatch(file -> file.state() == ImportFileState.FAILED)
                ? ImportState.FAILED
                : files.stream().anyMatch(file -> file.state() == ImportFileState.INIT)
                        ? ImportState.INIT
                        : files.stream().allMatch(file -> file.state() == ImportFileState.PARSED)
                                ? ImportState.SUCCESS
                                : ImportState.STORED;
        ImportStatus updated = new ImportStatus(
                current.importId(),
                current.serviceType(),
                current.timeFrame(),
                state,
                files,
                current.createdAt(),
                current.message());
        imports.removeIf(item -> item.importId().equals(importId));
        imports.add(0, updated);
        return updated;
    }

    private ImportStatus sample(String id, CnmServiceType serviceType, TimeFrame timeFrame, ProfileFamily family) {
        return sample(id, serviceType, timeFrame, family, "Mock import ready");
    }

    private ImportStatus sample(
            String id,
            CnmServiceType serviceType,
            TimeFrame timeFrame,
            ProfileFamily family,
            String message) {
        Instant now = Instant.now();
        RdfProfileReference profile = new RdfProfileReference(family, family == ProfileFamily.NCP
                ? "https://ap.cim4.eu/AssessedElement/2.4"
                : "https://ap-con.cim4.eu/Equipment/3.0", family == ProfileFamily.NCP ? "2.4" : "3.0");
        ImportFileStatus file = new ImportFileStatus(
                id + "-file",
                id + ".rdf",
                id + "/model.rdf",
                ImportFileState.PARSED,
                family,
                "2024-12-02",
                "23:30",
                "1D",
                "TSO-XYZ",
                "SV",
                "002",
                List.of(profile),
                "RDF metadata parsed and raw model stored",
                now);
        String statusMessage = message == null || message.isBlank() ? "Mock import ready" : message.trim();
        return new ImportStatus(id, serviceType, timeFrame, ImportState.SUCCESS, List.of(file), now, statusMessage);
    }

    private ImportFileStatus file(String importId, String fileId) {
        return importById(importId).files().stream()
                .filter(file -> file.fileId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mock import file not found: " + fileId));
    }

    private DynamicTableDefinition table(String tableId, String label, List<Map<String, Object>> rows) {
        List<DynamicTableColumn> columns = rows.stream()
                .flatMap(row -> row.keySet().stream())
                .distinct()
                .map(key -> new DynamicTableColumn(key, key, "string", true, true, ""))
                .toList();
        List<DynamicTableRow> tableRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            tableRows.add(new DynamicTableRow(String.valueOf(row.getOrDefault("mRID", tableId + "-" + index)), row));
        }
        return new DynamicTableDefinition(tableId, label, columns, tableRows, tableRows.size(), columns.isEmpty() ? "" : columns.get(0).key());
    }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            row.put(String.valueOf(values[index]), values[index + 1]);
        }
        return row;
    }
}

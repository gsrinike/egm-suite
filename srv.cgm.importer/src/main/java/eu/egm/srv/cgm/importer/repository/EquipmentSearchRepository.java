package eu.egm.srv.cgm.importer.repository;

import com.infra.document.DocumentAdapter;
import com.infra.document.DocumentFilter;
import com.infra.document.DocumentPage;
import com.infra.document.DocumentRepositoryService;
import com.infra.document.DocumentSearchRequest;
import com.infra.InfrastructureAdapterFactory;
import eu.egm.com.data.cgm.CgmesConstants;
import eu.egm.com.data.cgm.EquipmentType;
import eu.egm.com.data.cgm.SearchRequest;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class EquipmentSearchRepository {
    private static final int MAX_NETWORK_EQUIPMENT_RESULTS = 10_000;

    private final DocumentRepositoryService<EquipmentDocument> documentRepository;

    public EquipmentSearchRepository(InfrastructureAdapterFactory adapterFactory) {
        this.documentRepository = adapterFactory.documentRepository(new EquipmentDocumentAdapter());
    }

    public void saveAll(List<EquipmentDocument> documents) {
        documentRepository.saveAll(documents);
    }

    public List<EquipmentDocument> findByNetworkId(String networkId) {
        return documentRepository.findByField("networkId", networkId, MAX_NETWORK_EQUIPMENT_RESULTS);
    }

    public DocumentPage<EquipmentDocument> search(String networkId, SearchRequest request) {
        List<DocumentFilter> filters = new ArrayList<>();
        filters.add(DocumentFilter.exact("networkId", networkId));
        if (request.type() != null && request.type() != EquipmentType.UNKNOWN) {
            filters.add(DocumentFilter.exact("type", request.type()));
        }
        filters.add(DocumentFilter.exact("containerId", request.containerId()));
        filters.add(DocumentFilter.exact("region", request.region()));
        filters.add(DocumentFilter.exact("process", request.process()));
        filters.add(DocumentFilter.exact("businessDay", request.businessDay()));
        filters.add(DocumentFilter.exact("timestamp", normalizeTime(request.timestamp())));
        filters.add(DocumentFilter.exact("timeFrame", request.timeFrame()));
        filters.add(DocumentFilter.exact("tsoName", request.tsoName()));
        filters.add(DocumentFilter.exact("cgmesProfileType", request.cgmesProfileType()));
        filters.add(DocumentFilter.exact("versionNumber", request.versionNumber()));
        filters.add(DocumentFilter.exact("extension", request.extension()));

        // Free-text search is OR-ed across the fields users naturally search from the GUI.
        List<DocumentFilter> anyFilters = List.of(
                DocumentFilter.contains("equipmentId", request.query()),
                DocumentFilter.contains("name", request.query()),
                DocumentFilter.contains("containerId", request.query()));
        // Execute filtering in Elasticsearch; do not fetch a capped network slice and filter in memory.
        return documentRepository.search(new DocumentSearchRequest(filters, anyFilters, request.page(), request.size()));
    }

    private String normalizeTime(String value) {
        return value == null || value.isBlank() ? null : LocalTime.parse(value).toString();
    }

    private static class EquipmentDocumentAdapter implements DocumentAdapter<EquipmentDocument> {
        @Override
        public String indexName() {
            return CgmesConstants.NETWORK_INDEX;
        }

        @Override
        public String documentId(EquipmentDocument document) {
            return document.getDocumentId();
        }

        @Override
        public Class<EquipmentDocument> documentType() {
            return EquipmentDocument.class;
        }
    }
}

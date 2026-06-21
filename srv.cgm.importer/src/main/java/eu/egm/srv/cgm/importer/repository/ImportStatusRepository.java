package eu.egm.srv.cgm.importer.repository;

import com.infra.document.DocumentAdapter;
import com.infra.document.DocumentRepositoryService;
import com.infra.document.DocumentSort;
import com.infra.InfrastructureAdapterFactory;
import eu.egm.data.cgm.dto.cgmes.CgmesConstants;
import eu.egm.srv.cgm.importer.domain.ImportStatusDocument;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ImportStatusRepository {
    private static final int MAX_IMPORTS = 1_000;

    private final DocumentRepositoryService<ImportStatusDocument> documentRepository;

    public ImportStatusRepository(InfrastructureAdapterFactory adapterFactory) {
        this.documentRepository = adapterFactory.documentRepository(new ImportStatusDocumentAdapter());
    }

    public void save(ImportStatusDocument document) {
        documentRepository.save(document);
    }

    public List<ImportStatusDocument> findAll() {
        return documentRepository.findAll(MAX_IMPORTS, DocumentSort.descending("createdAt"));
    }

    private static class ImportStatusDocumentAdapter implements DocumentAdapter<ImportStatusDocument> {
        @Override
        public String indexName() {
            return CgmesConstants.IMPORT_STATUS_INDEX;
        }

        @Override
        public String documentId(ImportStatusDocument document) {
            return document.getNetworkId();
        }

        @Override
        public Class<ImportStatusDocument> documentType() {
            return ImportStatusDocument.class;
        }
    }
}

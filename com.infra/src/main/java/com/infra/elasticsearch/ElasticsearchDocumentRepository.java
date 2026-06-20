package com.infra.elasticsearch;

import com.infra.document.DocumentAdapter;
import com.infra.document.DocumentFilter;
import com.infra.document.DocumentPage;
import com.infra.document.DocumentRepositoryService;
import com.infra.document.DocumentSearchRequest;
import com.infra.document.DocumentSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

import java.util.List;

/**
 * Elasticsearch-backed implementation of the generic document repository.
 *
 * Application modules supply document metadata through {@link DocumentAdapter};
 * this class owns the Spring Data Elasticsearch interaction.
 */
public class ElasticsearchDocumentRepository<T> implements DocumentRepositoryService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchDocumentRepository.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentAdapter<T> adapter;
    private final IndexCoordinates indexCoordinates;

    public ElasticsearchDocumentRepository(ElasticsearchOperations elasticsearchOperations, DocumentAdapter<T> adapter) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.adapter = adapter;
        this.indexCoordinates = IndexCoordinates.of(adapter.indexName());
    }

    @Override
    public void save(T document) {
        saveAll(List.of(document));
    }

    @Override
    public void saveAll(List<T> documents) {
        if (documents.isEmpty()) {
            return;
        }
        ensureIndex();
        // Use explicit ids from the adapter so callers get deterministic upsert behavior.
        List<IndexQuery> queries = documents.stream()
                .map(document -> new IndexQueryBuilder()
                        .withId(adapter.documentId(document))
                        .withObject(document)
                        .build())
                .toList();
        elasticsearchOperations.bulkIndex(queries, indexCoordinates);
        elasticsearchOperations.indexOps(indexCoordinates).refresh();
        LOGGER.info("Stored {} documents in Elasticsearch index {}", documents.size(), adapter.indexName());
    }

    @Override
    public List<T> findByField(String fieldName, Object value, int maxResults) {
        if (!indexExists()) {
            return List.of();
        }
        CriteriaQuery query = new CriteriaQuery(new Criteria(fieldName).is(value));
        query.setPageable(PageRequest.of(0, maxResults));
        return search(query);
    }

    @Override
    public List<T> findAll(int maxResults, DocumentSort sort) {
        if (!indexExists()) {
            return List.of();
        }
        CriteriaQuery query = new CriteriaQuery(new Criteria());
        query.setPageable(PageRequest.of(0, maxResults, toSpringSort(sort)));
        return search(query);
    }

    @Override
    public DocumentPage<T> search(DocumentSearchRequest request) {
        if (!indexExists()) {
            return new DocumentPage<>(List.of(), 0, request.page(), request.size());
        }
        CriteriaQuery query = new CriteriaQuery(toCriteria(request));
        query.setPageable(PageRequest.of(request.page(), request.size()));
        var hits = elasticsearchOperations.search(query, adapter.documentType(), indexCoordinates);
        return new DocumentPage<>(
                hits.stream().map(SearchHit::getContent).toList(),
                hits.getTotalHits(),
                request.page(),
                request.size());
    }

    private Sort toSpringSort(DocumentSort sort) {
        Sort.Direction direction = sort.direction() == DocumentSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sort.fieldName());
    }

    private List<T> search(CriteriaQuery query) {
        return elasticsearchOperations.search(query, adapter.documentType(), indexCoordinates).stream()
                .map(SearchHit::getContent)
                .toList();
    }

    private Criteria toCriteria(DocumentSearchRequest request) {
        Criteria criteria = Criteria.and();
        // Required filters are AND-ed so narrow queries are evaluated in Elasticsearch.
        request.filters().stream()
                .filter(this::hasValue)
                .map(this::toCriterion)
                .forEach(criteria::subCriteria);
        if (!request.anyFilters().isEmpty()) {
            Criteria anyCriteria = Criteria.or();
            // Any filters are OR-ed for free-text style search boxes.
            request.anyFilters().stream()
                    .filter(this::hasValue)
                    .map(this::toCriterion)
                    .forEach(anyCriteria::subCriteria);
            if (!anyCriteria.getSubCriteria().isEmpty()) {
                criteria.subCriteria(anyCriteria);
            }
        }
        return criteria;
    }

    private Criteria toCriterion(DocumentFilter filter) {
        Criteria criteria = Criteria.where(filter.fieldName());
        return filter.matchMode() == DocumentFilter.MatchMode.CONTAINS
                ? criteria.contains(filter.value().toString())
                : criteria.is(filter.value());
    }

    private boolean hasValue(DocumentFilter filter) {
        if (filter == null || filter.value() == null) {
            return false;
        }
        return !(filter.value() instanceof String text) || !text.isBlank();
    }

    private boolean indexExists() {
        return elasticsearchOperations.indexOps(indexCoordinates).exists();
    }

    private void ensureIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(indexCoordinates);
        if (!indexOperations.exists()) {
            // Index creation is lazy to keep consuming services simple in local/docker setups.
            indexOperations.create();
            LOGGER.info("Created Elasticsearch index {}", adapter.indexName());
        }
    }
}

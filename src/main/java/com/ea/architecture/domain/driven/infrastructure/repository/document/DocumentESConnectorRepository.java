package com.ea.architecture.domain.driven.infrastructure.repository.document;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import com.ea.architecture.domain.driven.domain.exception.MessageCode;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.ea.architecture.domain.driven.infrastructure.repository.document.QueryBuilderUtils.prepareQueryList;


@Repository
public class DocumentESConnectorRepository {

    @Value("${elastic.index.name}")
    private String index;

    private final ElasticsearchClient elasticsearchClient;

    public DocumentESConnectorRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public String addDocument(DocumentEntity document) throws IOException {
        IndexRequest<DocumentEntity> request = IndexRequest.of(i ->
                i.index(index)
                        .id(String.valueOf(document.documentId()))
                        .document(document));
        IndexResponse response = elasticsearchClient.index(request);

        return switch (response.result()) {
            case Created -> "Document with id "+response.id()+" added successfully!";
            case Updated -> "Document with id "+response.id()+" updated successfully!";
            case NotFound -> "Document Not found!";
            case NoOp -> "No operation performed!";
            case Deleted -> "Document with id "+response.id()+"  deleted successfully!";
        };
    }

    public boolean bulkInsertDocuments(List<DocumentEntity> documentList) throws IOException {
        BulkRequest.Builder builder = new BulkRequest.Builder();
        documentList.stream().forEach(document ->
                builder.operations(op ->
                        op.index(i ->
                                i.index(index)
                                        .id(String.valueOf(document.documentId()))
                                        .document(document)))
        );
        BulkResponse bulkResponse = elasticsearchClient.bulk(builder.build());
        return !bulkResponse.errors();
    }

    public DocumentEntity getDocumentById(String id) throws FunctionalException, IOException {
        GetResponse<DocumentEntity> response = elasticsearchClient.get(req ->
                req.index(index)
                        .id(id), DocumentEntity.class);
        if (!response.found())
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document with ID " + id + " not found!");

        return response.source();
    }

    public List<DocumentEntity> getDocumentsWithMustQuery(DocumentEntity document) throws IOException {
        List<Query> queries = prepareQueryList(document);
        SearchResponse<DocumentEntity> documentSearchResponse = elasticsearchClient.search(req ->
                        req.index(index)
                                .size(100)
                                .query(query ->
                                        query.bool(bool ->
                                                bool.must(queries))),
                DocumentEntity.class);

        return documentSearchResponse.hits().hits().stream()
                .map(Hit::source).collect(Collectors.toList());
    }

    public List<DocumentEntity> getDocumentsWithShouldQuery(DocumentEntity document) throws IOException {
        List<Query> queries = prepareQueryList(document);
        SearchResponse<DocumentEntity> documentSearchResponse = elasticsearchClient.search(req ->
                        req.index(index)
                                .size(100)
                                .query(query ->
                                        query.bool(bool ->
                                                bool.should(queries))),
                DocumentEntity.class);

        return documentSearchResponse.hits().hits().stream()
                .map(Hit::source).collect(Collectors.toList());
    }

    public String deleteDocumentById(Long id) throws IOException {
        DeleteRequest request = DeleteRequest.of(req ->
                req.index(index).id(String.valueOf(id)));
        DeleteResponse response = elasticsearchClient.delete(request);
        return response.result().toString();
    }

    public String updateDocument(DocumentEntity document) throws IOException {
        UpdateRequest<DocumentEntity, DocumentEntity> updateRequest = UpdateRequest.of(req ->
                req.index(index)
                        .id(String.valueOf(document.documentId()))
                        .doc(document));
        UpdateResponse<DocumentEntity> response = elasticsearchClient.update(updateRequest, DocumentEntity.class);
        return response.result().toString();
    }
}

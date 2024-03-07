package com.ea.architecture.domain.driven.infrastructure.repository.document;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DocumentAiRepository {
    VectorStore vectorStore;

    public DocumentAiRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocumentToVectorStore(DocumentAggregate documentAggregate) {
        ByteArrayResource byteArrayResource = new ByteArrayResource(documentAggregate.getFile(), documentAggregate.getDocumentName());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(byteArrayResource);
        List<Document> documents = tikaDocumentReader.get();

/*        Document document = new Document("Spring AI rocks!! Spring AI rocks!!" +
                " Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1"));

        List<Document> documents = List.of(document);*/

        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);
        vectorStore.add(splitDocuments);
    }
}

package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.factory;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl.FolioReaderImpl;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl.tika.TikaExtractorImpl;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;

@Setter
public class DocumentExtractorFactoryBean implements FactoryBean<DocumentExtractor> {
    private String extractionType;

    @Override
    public DocumentExtractor getObject() throws Exception {
        return switch (extractionType.toLowerCase()) {
            case "tika" -> new TikaExtractorImpl();
            case "folio" -> new FolioReaderImpl();
            default -> throw new IllegalArgumentException("Unsupported document extractor type: " + extractionType);
        };
    }

    @Override
    public Class<?> getObjectType() {
        return DocumentExtractor.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

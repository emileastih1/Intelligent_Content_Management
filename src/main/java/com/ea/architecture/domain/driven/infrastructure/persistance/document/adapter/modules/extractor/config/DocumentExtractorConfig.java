package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.config;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.factory.DocumentExtractorFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentExtractorConfig {
    @Value("${document.extraction.engine}")
    private String extractionType;

    @Bean
    DocumentExtractor documentExtractorFactoryBean() throws Exception {
        DocumentExtractorFactoryBean documentExtractorFactoryBean = new DocumentExtractorFactoryBean();
        documentExtractorFactoryBean.setExtractionType(extractionType);
        return documentExtractorFactoryBean.getObject();
    }
}

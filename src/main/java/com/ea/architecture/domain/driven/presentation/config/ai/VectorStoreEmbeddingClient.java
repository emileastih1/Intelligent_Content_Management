package com.ea.architecture.domain.driven.presentation.config.ai;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.PgVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE;

@Configuration
public class VectorStoreEmbeddingClient {

    @Value("classpath:/ai/promptTemplate/rag-document-prompt-template.st")
    private Resource ragPromptTemplate;

    /**
     * description: It is very important to pass the desired dimension to the pgVectorStore in our case OPENAI_EMBEDDING_DIMENSION_SIZE
     * Because if you don't pass in the dimension, a call will be made to the openAI API to get the dimension size.
     * If the generative name (dimensions) is unknown uses the EmbeddingClient to perform a dummy EmbeddingClient#embed
     * and count the response dimensions!
     *
     * @param jdbcTemplate
     * @param embeddingClient
     * @return
     */
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        return new PgVectorStore(jdbcTemplate, embeddingClient, OPENAI_EMBEDDING_DIMENSION_SIZE);
    }

    @Bean
    public PromptTemplate promptTemplate() {
        return new PromptTemplate(ragPromptTemplate);
    }
}

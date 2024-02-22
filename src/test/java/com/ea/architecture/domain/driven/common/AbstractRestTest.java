package com.ea.architecture.domain.driven.common;

import com.ea.architecture.domain.driven.presentation.common.api.BaseRestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public abstract class AbstractRestTest<T extends BaseRestController> {

    protected final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AbstractRestTest(){
        try (AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this)) {
            this.mockMvc = MockMvcBuilders.standaloneSetup(getController()).build();
            this.objectMapper = new ObjectMapper();
            this.objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract T getController();

    protected String getObjectAsJsonContent(Object obj) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(obj);
    }
}

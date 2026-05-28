package com.ea.icm.common;

import com.ea.icm.presentation.common.api.BaseRestController;
import com.ea.icm.presentation.exception.BackendExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public abstract class AbstractRestTest<T extends BaseRestController> {

    protected final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AbstractRestTest() {
        try (AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this)) {
            this.mockMvc = MockMvcBuilders.standaloneSetup(getController())
                    .setControllerAdvice(BackendExceptionHandler.class)
                    .build();
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

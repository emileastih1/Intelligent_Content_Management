package com.ea.architecture.domain.driven.presentation.document.api.query;

import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.application.document.port.query.DocumentManagementQueryService;
import com.ea.architecture.domain.driven.common.AbstractRestTest;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.presentation.document.mapper.DocumentPresentationMapper;
import com.ea.architecture.domain.driven.presentation.exception.ErrorMessageConstants;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class DocumentQueryRestControllerTest extends AbstractRestTest<DocumentQueryRestController> {
    @Mock
    DocumentManagementQueryService documentManagementQueryService;
    @Mock
    DocumentPresentationMapper documentPresentationMapper;
    @InjectMocks
    private DocumentQueryRestController documentQueryRestController;

    @Override
    protected DocumentQueryRestController getController() {
        return documentQueryRestController;
    }

    @Test
    @DisplayName("Should return document given valid id")
    void should_return_document_given_valid_id() throws Exception {
        //Given
        DocumentDto document = new DocumentDto(new UniqueId("1"),"1212121212",
                "Legal Document", "98785", "25 MB", "/home/documents");

        DocumentAggregate documentAggregate = DocumentAggregate.builder()
                .id(new UniqueId("1"))
                .documentName("Legal Document")
                .owner("98785")
                .location("/home/documents")
                .build();

        //When
        Mockito.when(documentManagementQueryService.findDocumentById("1")).thenReturn(documentAggregate);
        Mockito.when(documentPresentationMapper.domainToDto(documentAggregate)).thenReturn(document);

        //Then
        String jsonReturned = getObjectAsJsonContent(document);

        mockMvc.perform(MockMvcRequestBuilders.
                        get("/api/v1/document/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonReturned));
    }

    @Test
    @DisplayName("Should throw given invalid id")
    void should_throw_given_invalid_id() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.
                        get("/api/v1/document/")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
//                .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_TYPE_MISMATCH))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_TYPE_MISMATCH))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value(Matchers.startsWith("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'")));

    }
}

package com.ea.architecture.domain.driven.presentation.document.api;

import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.application.document.port.query.DocumentManagementQueryService;
import com.ea.architecture.domain.driven.common.AbstractRestTest;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.presentation.document.mapper.DocumentPresentationMapper;
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

    @Test
    void should_return_document_given_valid_id() throws Exception {
        //Given
        DocumentDto document = new DocumentDto(new UniqueId(1L),
                "Legal Document", "98785", "25 MB", "/home/documents");

        DocumentAggregate documentAggregate = DocumentAggregate.builder()
                .id(new UniqueId(1L))
                .name("Legal Document")
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

    //TODO finish this implementation
    void should_throw_given_invalid_id() throws Exception {
        DocumentDto document = new DocumentDto(new UniqueId(1L),
                "Legal Document", "98785", "25 MB", "/home/documents");

        mockMvc.perform(MockMvcRequestBuilders.
                        get("/api/v1/document/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Override
    protected DocumentQueryRestController getController() {
        return documentQueryRestController;
    }
}

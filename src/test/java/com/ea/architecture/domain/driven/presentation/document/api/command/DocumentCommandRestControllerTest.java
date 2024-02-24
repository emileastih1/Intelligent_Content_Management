package com.ea.architecture.domain.driven.presentation.document.api.command;

import com.ea.architecture.domain.driven.application.document.dto.AddDocumentDto;
import com.ea.architecture.domain.driven.application.document.dto.DocumentResult;
import com.ea.architecture.domain.driven.application.document.port.command.DocumentManagementCommandService;
import com.ea.architecture.domain.driven.common.AbstractRestTest;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentTypes;
import com.ea.architecture.domain.driven.domain.document.vo.FileSize;
import com.ea.architecture.domain.driven.domain.document.vo.UnitOfMeasurement;
import com.ea.architecture.domain.driven.presentation.document.mapper.DocumentPresentationMapper;
import com.ea.architecture.domain.driven.presentation.exception.ErrorMessageConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.Charset;
class DocumentCommandRestControllerTest extends AbstractRestTest<DocumentCommandRestController> {
    @InjectMocks
    DocumentCommandRestController documentCommandRestController;
    @Mock
    DocumentManagementCommandService documentManagementCommandService;
    @Mock
    DocumentPresentationMapper documentPresentationMapper;
    @Override
    protected DocumentCommandRestController getController() {
        return this.documentCommandRestController;
    }

    @Nested
    @DisplayName("Arguments validation: when a post request is made to add a document")
    class ArgumentsValidation{
        @Test
        @DisplayName("With valid data then add document")
        void whenPostRequestToAddDocumentWithValidData_thenAddDocument() throws Exception {
            //Given
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument" , "base64File", "15 MB", "JPG");

            //Returned mocks
            String elasticResult = "Document with id 1 added successfully!";
            DocumentResult documentResult = new DocumentResult(0L,"","","","", elasticResult);
            DocumentAggregate documentAggregate = DocumentAggregate.builder()
                    .id(new UniqueId(0L))
                    .documentName("TestDocument")
                    .documentType(DocumentTypes.JPG)
                    .fileSize(new FileSize("15", UnitOfMeasurement.MB))
                    .file(null)
                    .build();
            //When
            Mockito.doReturn(documentAggregate).when(documentPresentationMapper).dtoAddDocumentToDomain(addDocumentDto);
            Mockito.doReturn(elasticResult).when(documentManagementCommandService).addDocument(documentAggregate);
            Mockito.doReturn(documentResult).when(documentPresentationMapper).toDocumentResult(elasticResult);

            //Then
            String jsonRequest = getObjectAsJsonContent(addDocumentDto);
            String jsonResult = getObjectAsJsonContent(documentPresentationMapper.toDocumentResult(elasticResult));

            MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(SecurityMockMvcRequestPostProcessors.httpBasic("UserWrite", "password2"))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json(jsonResult))
                    .andReturn();

//        String jsonResponse = result.getResponse().getContentAsString();
//        System.out.println("Response JSON: " + jsonResponse);
        }
        @Test
        @DisplayName("With empty document name then bad request")
        void whenPostRequestToAddDocumentWithEmptyName_thenThrowMethodArgumentNotValidException () throws Exception {
            //Given
            AddDocumentDto addDocumentDto = new AddDocumentDto("" , "base64File", "15 MB", "JPG");

            //Then
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(getObjectAsJsonContent(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(SecurityMockMvcRequestPostProcessors.httpBasic("UserWrite", "password2"))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("name: Document name is mandatory "))
                    .andReturn();

        }
        @Test
        @DisplayName("With empty file then bad request")
        void whenPostRequestToAddDocumentWithEmptyFile_thenThrowMethodArgumentNotValidException () throws Exception {
            //Given
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument" , "", "15 MB", "JPG");

            //Then
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(getObjectAsJsonContent(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(SecurityMockMvcRequestPostProcessors.httpBasic("UserWrite", "password2"))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("base64File: File is mandatory "))
                    .andReturn();

        }
        @Test
        @DisplayName("With empty document type then bad request")
        void whenPostRequestToAddDocumentWithEmptyDocumentType_thenThrowMethodArgumentNotValidException () throws Exception {
            //Given
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument" , "base64File", "15 MB", "");

            //Then
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(getObjectAsJsonContent(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(SecurityMockMvcRequestPostProcessors.httpBasic("UserWrite", "password2"))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("fileType: Document type is mandatory "))
                    .andReturn();

        }
    }
}
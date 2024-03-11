package com.ea.architecture.domain.driven.presentation.document.api.query;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.application.document.port.query.DocumentManagementQueryService;
import com.ea.architecture.domain.driven.presentation.common.api.BaseRestController;
import com.ea.architecture.domain.driven.presentation.document.mapper.DocumentPresentationMapper;
import com.ea.architecture.domain.driven.presentation.user.api.UserQueryRestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document", description = "API gateway to query the document domain")
@RestController
public class DocumentQueryRestController extends BaseRestController {

    public static final Logger logger = LoggerFactory.getLogger(UserQueryRestController.class);
    DocumentManagementQueryService documentManagementQueryService;
    DocumentPresentationMapper documentPresentationMapper;

    public DocumentQueryRestController(DocumentManagementQueryService documentManagementQueryService, DocumentPresentationMapper documentPresentationMapper) {
        this.documentManagementQueryService = documentManagementQueryService;
        this.documentPresentationMapper = documentPresentationMapper;
    }

    @Operation(
            summary = "Find a document by Id",
            description = "Find a document by Id",
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
            responses = {
                    @ApiResponse(responseCode = "200", description = "ok", content = @Content(
                            schema = @Schema(implementation = DocumentDto.class)
                    ))
            }
    )
    @GetMapping(value = "/v1/document/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentDto> findDocument(@PathVariable String id) throws Exception {
        return new ResponseEntity<>((documentPresentationMapper.domainToDto(documentManagementQueryService.findDocumentById(id))), HttpStatus.OK);
    }

}

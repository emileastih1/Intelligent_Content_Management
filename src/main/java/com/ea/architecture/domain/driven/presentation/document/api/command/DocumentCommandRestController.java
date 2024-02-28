package com.ea.architecture.domain.driven.presentation.document.api.command;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import com.ea.architecture.domain.driven.application.document.dto.AddDocumentDto;
import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.application.document.port.command.DocumentManagementCommandService;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.presentation.common.api.BaseRestController;
import com.ea.architecture.domain.driven.presentation.document.mapper.DocumentPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document", description = "API gateway to mutate the document domain")
@RestController
public class DocumentCommandRestController extends BaseRestController {

    private final DocumentManagementCommandService documentManagementCommandService;

    private final DocumentPresentationMapper documentPresentationMapper;

    public DocumentCommandRestController(DocumentManagementCommandService documentManagementCommandService, DocumentPresentationMapper documentPresentationMapper) {
        this.documentManagementCommandService = documentManagementCommandService;
        this.documentPresentationMapper = documentPresentationMapper;
    }

    @Operation(
            summary = "Add document",
            description = "Add document",
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_WRITE})},
            responses = {
                    @ApiResponse(responseCode = "200", description = "ok", content = @Content(
                            schema = @Schema(implementation = DocumentDto.class)
                    ))
            }
    )
    @PostMapping(value = "/v1/document", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResult> addOrUpdateDocument(@Valid @RequestBody AddDocumentDto documentDto) {
        DocumentAggregate documentAggregate = documentPresentationMapper.dtoAddDocumentToDomain(documentDto);
        DocumentResult result = documentManagementCommandService.addOrUpdateDocument(documentAggregate);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

/*    @Operation(
            summary = "Upload document",
            description = "Upload document",
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_WRITE})},
            responses = {
                    @ApiResponse(responseCode = "200", description = "ok", content = @Content(
                            schema = @Schema(implementation = DocumentDto.class)
                    ))
            }
    )
    @PutMapping(value = "/v1/document", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResult> uploadDocument(@Valid @RequestBody AddDocumentDto documentDto) {
        DocumentAggregate documentAggregate = documentPresentationMapper.dtoAddDocumentToDomain(documentDto);
        String documentResult = documentManagementCommandService.uploadDocument(documentAggregate);
        DocumentResult result = documentPresentationMapper.toDocumentResult(documentResult);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }*/
}

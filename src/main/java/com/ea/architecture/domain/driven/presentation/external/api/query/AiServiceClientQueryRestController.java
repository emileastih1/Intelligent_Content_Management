package com.ea.architecture.domain.driven.presentation.external.api.query;

import com.ea.architecture.domain.driven.application.external.port.query.AiServiceClientQuery;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Answer;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Question;
import com.ea.architecture.domain.driven.presentation.common.api.BaseRestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "AiServiceClient", description = "API gateway to communicate with the external AI Service Client")
public class AiServiceClientQueryRestController extends BaseRestController {
    private final AiServiceClientQuery aiServiceClientQuery;

    public AiServiceClientQueryRestController(AiServiceClientQuery aiServiceClientQuery) {
        this.aiServiceClientQuery = aiServiceClientQuery;
    }

    @Operation(
            summary = "Ask a relevant question",
            description = "Ask a relevant question",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ok",
                            content = @Content(
                                    schema = @Schema(implementation = Answer.class)
                            ))
            }
    )
    @PostMapping(value = "/v1/document/ask", produces = "application/json")
    public ResponseEntity<Answer> askQuestion(@RequestBody @Valid Question question) {
        return new ResponseEntity<Answer>(aiServiceClientQuery.askQuestion(question), HttpStatus.OK);
    }
}

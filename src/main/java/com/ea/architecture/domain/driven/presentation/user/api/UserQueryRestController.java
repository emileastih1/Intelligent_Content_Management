package com.ea.architecture.domain.driven.presentation.user.api;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.application.user.port.UserManagementService;
import com.ea.architecture.domain.driven.presentation.BaseQueryRestController;
import com.ea.architecture.domain.driven.presentation.user.mapper.UserPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "User", description = "API gateway to query the user domain")
@RestController
public class UserQueryRestController extends BaseQueryRestController {

    UserManagementService userManagementService;

    UserPresentationMapper userPresentationMapper;

    public UserQueryRestController(UserManagementService userManagementService, UserPresentationMapper userExpositionMapper) {
        this.userManagementService = userManagementService;
        this.userPresentationMapper = userExpositionMapper;
    }

    @Operation(
            summary = "Find a user",
            description = "Find a user by supplying different filters",
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
            responses = {
                    @ApiResponse(responseCode = "200", description = "ok", content = @Content(
                            schema = @Schema(implementation = UserDto.class)
                    ))
            }
    )
    @GetMapping(value = "/v1/person/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDto> findUser(@RequestBody UserDto dto) throws Exception {
        return new ResponseEntity<>((userPresentationMapper.domainToDto(userManagementService.getUserByFilter(userPresentationMapper.dtoToDomain(dto)))), HttpStatus.OK);
    }
}

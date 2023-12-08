package com.ea.architecture.domain.driven.presentation.user.api;

import com.ea.architecture.domain.driven.application.user.port.UserManagementService;
import com.ea.architecture.domain.driven.presentation.BaseQueryRestController;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.presentation.user.mapper.UserPresentationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserQueryRestController extends BaseQueryRestController {

    UserManagementService userManagementService;

    UserPresentationMapper userPresentationMapper;

    public UserQueryRestController(UserManagementService userManagementService, UserPresentationMapper userExpositionMapper) {
        this.userManagementService = userManagementService;
        this.userPresentationMapper = userExpositionMapper;
    }

    @GetMapping("/v1/person/search")
    public ResponseEntity<UserDto> findUser(@RequestBody UserDto dto) {
        return new ResponseEntity<>((userPresentationMapper.domainToDto(userManagementService.getUserByFilter(userPresentationMapper.dtoToDomain(dto)))), HttpStatus.OK);
    }
}

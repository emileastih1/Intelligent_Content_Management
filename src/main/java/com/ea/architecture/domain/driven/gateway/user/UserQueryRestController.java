package com.ea.architecture.domain.driven.gateway.user;

import com.ea.architecture.domain.driven.application.user.UserManagementService;
import com.ea.architecture.domain.driven.gateway.BaseRestController;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.gateway.user.mapper.UserExpositionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/user")
public class UserQueryRestController extends BaseRestController {

    UserManagementService userManagementService;

    UserExpositionMapper userExpositionMapper;

    public UserQueryRestController(UserManagementService userManagementService, UserExpositionMapper userExpositionMapper) {
        this.userManagementService = userManagementService;
        this.userExpositionMapper = userExpositionMapper;
    }

    @GetMapping("/find")
    public ResponseEntity<UserDto> findUser(@RequestBody UserDto dto) {
        return new ResponseEntity<>((userExpositionMapper.domainToDto(userManagementService.findUser(dto))), HttpStatus.OK);
    }
}

package com.ea.architecture.domain.driven.presentation.user.api;

import com.ea.architecture.domain.driven.application.user.dto.AddressDto;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.application.user.port.UserManagementService;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.presentation.user.mapper.UserPresentationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.Charset;

public class UserQueryRestControllerTest {
    @Mock
    UserManagementService userManagementService;
    @Mock
    private UserPresentationMapper userPresentationMapper;

    @InjectMocks
    private UserQueryRestController userQueryRestController;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public UserQueryRestControllerTest() throws Exception {
        try (AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this)) {
            this.mockMvc = MockMvcBuilders.standaloneSetup(userQueryRestController).build();
            this.objectMapper = new ObjectMapper();
        }
    }

    @Test
    void testFindUserEndpoint() throws Exception {

        //Given
        UserDto inputDto = new UserDto(1L, "", "", "", null);
        UserAggregate userAggregateInput = new UserAggregate();
        userAggregateInput.setId(new UniqueId(inputDto.id()));

        //Returned mocks
        UserAggregate userAggregateReturned = new UserAggregate(new UniqueId(inputDto.id()), "Emile","Astih", "33", null);
        UserDto dtoReturned = new UserDto(1L, "Emile", "Astih", "33", null);

        //When
        Mockito.doReturn(userAggregateReturned).when(userManagementService).getUserByFilter(userAggregateInput);
        Mockito.doReturn(userAggregateInput).when(userPresentationMapper).dtoToDomain(inputDto);
        Mockito.doReturn(dtoReturned).when(userPresentationMapper).domainToDto(userAggregateReturned);

        //Then
        String jsonRequest = objectMapper.writeValueAsString(inputDto);
        String jsonResult = objectMapper.writeValueAsString(dtoReturned);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/person/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(Charset.defaultCharset())
                        //TODO: check why the security is ignored when you run unit tests
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("UserRead", "password1"))  // Provide your username and password)
                        .content(jsonRequest))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonResult))
                .andReturn();

        // Print the response content
        String jsonResponse = result.getResponse().getContentAsString();
        System.out.println("Response JSON: " + jsonResponse);
    }
}

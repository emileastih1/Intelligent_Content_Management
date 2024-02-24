package com.ea.architecture.domain.driven.presentation.user.api;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.application.user.port.query.UserManagementQueryService;
import com.ea.architecture.domain.driven.common.AbstractRestTest;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.presentation.user.mapper.UserPresentationMapper;
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

public class UserQueryRestControllerTest extends AbstractRestTest<UserQueryRestController> {
    @Mock
    UserManagementQueryService userManagementQueryService;
    @Mock
    private UserPresentationMapper userPresentationMapper;
    @InjectMocks
    private UserQueryRestController userQueryRestController;
    @Override
    protected UserQueryRestController getController() {
        return userQueryRestController;
    }

    @Test
    void testFindUserEndpoint() throws Exception {

        //Given
        UserDto inputDto = new UserDto(1L, "", "", "", null);
        UserAggregate userAggregateInput = new UserAggregate();
        userAggregateInput.setId(new UniqueId(inputDto.id()));

        //Returned mocks
        UserAggregate userAggregateReturned = new UserAggregate(new UniqueId(inputDto.id()), "Emile", "Astih", "33", null);
        UserDto dtoReturned = new UserDto(1L, "Emile", "Astih", "33", null);

        //When
        Mockito.doReturn(userAggregateReturned).when(userManagementQueryService).getUserByFilter(userAggregateInput);
        Mockito.doReturn(userAggregateInput).when(userPresentationMapper).dtoToDomain(inputDto);
        Mockito.doReturn(dtoReturned).when(userPresentationMapper).domainToDto(userAggregateReturned);

        //Then
        String jsonRequest = getObjectAsJsonContent(inputDto);
        String jsonResult = getObjectAsJsonContent(dtoReturned);


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

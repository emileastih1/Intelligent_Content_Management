package com.ea.architecture.domain.driven.presentation.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorDtoTest {
    @Test
    public void test_validation_title_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("","303", 1,LocalDateTime.now(), "error details"));
    }
    @Test
    public void test_validation_error_code_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("error title","", 1,LocalDateTime.now(),
                            "error details"));
    }
    @Test
    public void test_validation_detail_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("error title","303", 1,LocalDateTime.now(),
                            ""));
    }

}
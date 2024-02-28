package com.ea.architecture.domain.driven.presentation.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorDtoTest {
    @Test
    @DisplayName("Test validation with an empty title, should throw an exception")
    public void test_validation_title_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("","303", 1,LocalDateTime.now(), "error details"));
    }
    @Test
    @DisplayName("Test validation with an empty error code, should throw an exception")
    public void test_validation_error_code_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("title","", 1,LocalDateTime.now(),
                            "error details"));
    }
    @Test
    @DisplayName("Test validation with an empty detail message, should throw an exception")
    public void test_validation_detail_empty_throw_exception (){
            assertThrows(IllegalArgumentException.class,
                    ()-> new ApiErrorDto("title","303", 1,LocalDateTime.now(),
                            ""));
    }

}
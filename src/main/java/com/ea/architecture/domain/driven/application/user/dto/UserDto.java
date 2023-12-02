package com.ea.architecture.domain.driven.application.user.dto;

public record UserDto(long id, String firstName, String lastName, String age, AddressDto address) {
}

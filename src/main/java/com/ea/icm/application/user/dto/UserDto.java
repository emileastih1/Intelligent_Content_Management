package com.ea.icm.application.user.dto;

public record UserDto(long id, String firstName, String lastName, String age, AddressDto address) {
}

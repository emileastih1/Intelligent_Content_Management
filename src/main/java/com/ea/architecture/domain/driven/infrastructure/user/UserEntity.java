package com.ea.architecture.domain.driven.infrastructure.user;

import com.ea.architecture.domain.driven.application.user.dto.AddressDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class UserEntity {

    @GeneratedValue
    @Id
    private  long id;

    @Column(name = "FIRSTNAME")
    private String firstName;

    @Column(name = "LASTNAME")
    private String lastName;

    @Column(name = "AGE")
    private String age;

    private AddressDto address;
}

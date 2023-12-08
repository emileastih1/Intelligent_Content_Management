package com.ea.architecture.domain.driven.infrastructure.persistance.user.model;

import com.ea.architecture.domain.driven.application.user.dto.AddressDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name ="USERS")
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

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private AddressEntity address;
}

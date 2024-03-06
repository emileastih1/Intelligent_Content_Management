package com.ea.architecture.domain.driven.infrastructure.persistance.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "address")
public class AddressEntity {

    @GeneratedValue
    @Id
    private long id;

    @Column(name = "CITY")
    private String city;

    @Column(name = "FULL_ADDRESS")
    private String fullAddress;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;
}

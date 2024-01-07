package com.ea.architecture.domain.driven.domain.user.model;

import com.ea.architecture.domain.driven.application.user.dto.AddressDto;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.AbstractAggregateRoot;

/**
 * This class represent your DDD aggregate root
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAggregate extends AbstractAggregateRoot<UserAggregate> {

    private UniqueId id;
    private String firstName;
    private String lastName;
    private String age;
    //TODO: This should be a value object of the domain
    //because the domain layer should not be dependent on the application layer
    private AddressDto address;

}



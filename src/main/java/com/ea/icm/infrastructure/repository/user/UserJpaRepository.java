package com.ea.icm.infrastructure.repository.user;

import com.ea.icm.infrastructure.persistance.user.model.UserEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends ListCrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByFirstNameAndLastName(String firstName, String lastName);
}

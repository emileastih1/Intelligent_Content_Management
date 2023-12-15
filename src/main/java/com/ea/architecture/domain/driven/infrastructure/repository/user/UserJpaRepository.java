package com.ea.architecture.domain.driven.infrastructure.repository.user;

import com.ea.architecture.domain.driven.infrastructure.persistance.user.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByFirstNameAndLastName(String firstName, String lastName);
}

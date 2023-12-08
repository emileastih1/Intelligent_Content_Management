package com.ea.architecture.domain.driven.infrastructure.repository.user;

import com.ea.architecture.domain.driven.infrastructure.persistance.user.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}

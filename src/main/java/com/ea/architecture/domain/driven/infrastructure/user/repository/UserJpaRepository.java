package com.ea.architecture.domain.driven.infrastructure.user.repository;

import com.ea.architecture.domain.driven.infrastructure.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}

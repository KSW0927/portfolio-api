package com.seokwon.notiflow.userauth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // username으로 사용자 조회
    Optional<UserEntity> findByUsername(String username);

    // userId로 사용자 조회
    Optional<UserEntity> findByUserId(String userId);

    // username 중복 체크
    boolean existsByUsername(String username);

    // userId 중복 체크
    boolean existsByUserId(String userId);
}

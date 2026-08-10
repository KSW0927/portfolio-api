package com.seokwon.notiflow.notify;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyRepository extends JpaRepository<NotifyEntity, Long> {

    List<NotifyEntity> findTop20ByOrderByCreatedAtDesc();
}

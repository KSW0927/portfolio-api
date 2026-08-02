package com.seokwon.notiflow.notify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.notify.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findTop20ByOrderByCreatedAtDesc();
}

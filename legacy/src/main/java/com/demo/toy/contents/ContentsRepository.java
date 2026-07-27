package com.demo.toy.contents;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentsRepository extends JpaRepository<ContentsEntity, Long> {
    Optional<ContentsEntity> findByContentId(Long contentId);
    
	Page<ContentsEntity> findByTitleContaining(String title, Pageable pageable);
}

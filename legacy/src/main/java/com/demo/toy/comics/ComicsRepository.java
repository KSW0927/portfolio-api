package com.demo.toy.comics;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.toy.contents.ContentsEntity;

public interface ComicsRepository extends JpaRepository<ComicsEntity, Long> {

    /**
     * comicsId로 단건 조회
     */
    Optional<ComicsEntity> findByComicsId(Long comicsId);

    /**
     * 부모 콘텐츠(ApiEntity)로 권수 목록 조회
     */
    List<ComicsEntity> findByContent(ContentsEntity content);

    /**
     * 부모 콘텐츠(ApiEntity)로 페이징 조회
     */
    Page<ComicsEntity> findByContent(ContentsEntity content, Pageable pageable);

    /**
     * 삭제 시 존재 여부 확인용
     */
    boolean existsByComicsId(Long comicsId);
    
    Optional<ComicsEntity> findByComicsIdAndContent_ContentId(Long comicsId, Long contentId);
}

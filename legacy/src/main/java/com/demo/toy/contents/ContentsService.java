package com.demo.toy.contents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.demo.toy.common.exception.NotFoundException;

@Service
public class ContentsService {

    private final ContentsRepository contentsRepository;
    
    public ContentsService(ContentsRepository contentsRepository, RestTemplate restTemplate) {
        this.contentsRepository = contentsRepository;
    }

    /**
     * 목록
     */
    public Page<ContentsEntity> getContentsList(ContentsSearchParamsDTO searchDTO) {
        Pageable pageable = PageRequest.of(
            searchDTO.getPage(),
            searchDTO.getSize(),
            Sort.by(Sort.Direction.DESC, "regDate")
        );

        return contentsRepository.findByTitleContaining(searchDTO.getTitle(), pageable);
    }
    
    /**
     * 저장
     */
    @Transactional
    public ContentsEntity insertContent(ContentsEntity entity) {
        return contentsRepository.save(entity);
    }
    
    /**
     * 상세
     */
    public ContentsEntity getContentDetail(Long contentId) {
    	return contentsRepository.findById(contentId).orElseThrow(() -> new NotFoundException("contentId=" + contentId));
    }
    
    /**
     * 수정
     */
    @Transactional
    public ContentsEntity updateContent(Long contentId, ContentsDTO dto) {
        ContentsEntity entity = contentsRepository.findById(contentId).orElseThrow(() -> new IllegalArgumentException("contentId" + contentId));
        entity.update(dto);
        return entity;
    }
    
    /**
     * 삭제
     */
    public void deleteContent(Long contentId) {
        if (!contentsRepository.existsById(contentId)) {
            throw new IllegalArgumentException("contentId=" + contentId);
        }
        contentsRepository.deleteById(contentId);
    }
}
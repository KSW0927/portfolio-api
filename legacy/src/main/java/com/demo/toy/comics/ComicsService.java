package com.demo.toy.comics;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.toy.common.exception.NotFoundException;
import com.demo.toy.contents.ContentsEntity;
import com.demo.toy.contents.ContentsRepository;

@Service
public class ComicsService {

	private final ContentsRepository contentsRepository;
    private final ComicsRepository comicsRepository;

    public ComicsService(ContentsRepository contentsRepository, ComicsRepository comicsRepository) {
    	this.contentsRepository = contentsRepository;
        this.comicsRepository = comicsRepository;
    }

    /**
     * 만화 메타데이터 조회
     */
    public List<ComicsEntity> getComicsByContentId(Long contentId) {
        ContentsEntity content = contentsRepository.findByContentId(contentId)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));
        return comicsRepository.findByContent(content);
    }

    /**
     * 만화 메타데이터 조회(페이징)
     */
    public Page<ComicsEntity> getComicsByContentId(Long contentId, Pageable pageable) {
        ContentsEntity content = contentsRepository.findByContentId(contentId)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));
        return comicsRepository.findByContent(content, pageable);
    }

    /**
     * 만화 메타데이터 등록
     */
    public List<ComicsResponseDTO> insertComicsBatch(Long contentId, List<ComicsCreateDTO> createDTO) {
        ContentsEntity content = contentsRepository.findByContentId(contentId)
            .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));

        List<ComicsEntity> create = createDTO.stream()
            .map(dto -> ComicsEntity.create(
                content,
                dto.getVolume(),
                dto.getPage(),
                dto.getVolumePrice(),
                dto.getVolumeImageUrl(),
                dto.getVolumeFileSize()
            ))
            .toList();

        List<ComicsEntity> saved = comicsRepository.saveAll(create);
        return saved.stream().map(ComicsResponseDTO::from).toList();
    }
    
    /**
     * 만화 메타데이터 수정
     */
    @Transactional
    public void updateComicsBatch(Long contentId, List<ComicsUpdateDTO> dtoList) {
        dtoList.forEach(dto -> {
            ComicsEntity entity = comicsRepository
                .findByComicsIdAndContent_ContentId(dto.getComicsId(), contentId)
                .orElseThrow(() ->
                    new NotFoundException("만화를 찾을 수 없습니다. comicsId=" + dto.getComicsId())
                );

            entity.update(dto); // dirty checking
        });
    }


    /**
     * 만화 메타데이터 삭제
     */
    public void deleteComics(Long comicsId) {
        ComicsEntity entity = comicsRepository.findByComicsId(comicsId)
                .orElseThrow(() -> new NotFoundException("만화를 찾을 수 없습니다. comicsId=" + comicsId));
        comicsRepository.delete(entity);
    }
}

package com.demo.toy.comics;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.toy.common.response.ResponseResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/comics")
@Tag(name = "만화 API", description = "콘텐츠 별 만화 메타 정보 등록, 수정, 삭제, 조회 API")
public class ComicsController {

    private final ComicsService comicsService;

    public ComicsController(ComicsService comicsService) {
        this.comicsService = comicsService;
    }

    /**
     * 만화 메타데이터 조회
     */
    @GetMapping("/{contentId}/volumes")
    @Operation(description = "콘텐츠의 만화 메타 정보 조회.")
    public ResponseEntity<List<ComicsEntity>> getComicsList(@PathVariable("contentId") Long contentId) {
        List<ComicsEntity> response = comicsService.getComicsByContentId(contentId);
        return ResponseEntity.status(ResponseResult.SUCCESS_READ.getCode()).body(response);
    }

    /**
     * 만화 메타데이터 등록
     */
    @PostMapping("/{contentId}/volumes")
    @Operation(description = "콘텐츠의 만화 메타 정보 등록.")
    public ResponseEntity<List<ComicsResponseDTO>> insertComicsBatch(@PathVariable("contentId") Long contentId, @RequestBody List<ComicsCreateDTO> dtoList) {
        List<ComicsResponseDTO> response = comicsService.insertComicsBatch(contentId, dtoList);
        return ResponseEntity.status(ResponseResult.SUCCESS_SAVE.getCode()).body(response);
    }
    
    /**
     * 만화 메타데이터 수정
     */
    @PutMapping("/{contentId}/volumes")
    @Operation(description = "콘텐츠의 만화 메타 정보 수정.")
    public ResponseEntity<String> updateComicsBatch(@PathVariable("contentId") Long contentId, @RequestBody List<ComicsUpdateDTO> dtoList) {
    	comicsService.updateComicsBatch(contentId, dtoList);
    	return ResponseEntity.status(ResponseResult.SUCCESS_UPDATE.getCode()).body(ResponseResult.SUCCESS_UPDATE.getMessage());
    }

    /**
     * 만화 메타데이터 삭제
     */
    @DeleteMapping("/{contentId}/volumes/{comicsId}")
    @Operation(description = "콘텐츠의 만화 메타 정보 삭제.")
    public ResponseEntity<String> deleteComics(@PathVariable("comicsId") Long comicsId) {
        comicsService.deleteComics(comicsId);
        return ResponseEntity.status(ResponseResult.SUCCESS_DELETE.getCode()).body(ResponseResult.SUCCESS_DELETE.getMessage());
    }
}

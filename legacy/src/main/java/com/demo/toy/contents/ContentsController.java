package com.demo.toy.contents;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.toy.common.response.ApiResponse;
import com.demo.toy.common.response.ResponseResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "콘텐츠 API", description = "콘텐츠 등록, 수정, 삭제, 조회 API")
public class ContentsController {

    private final ContentsService contentsService;

    public ContentsController(ContentsService contentsService) {
        this.contentsService = contentsService;
    }

    /**
     * 콘텐츠 목록
     */
    @GetMapping("/contents")
    @Operation(summary = "콘텐츠 목록 조회", description = "콘텐츠 목록을 조회합니다.")
    public ResponseEntity<Object> getContentsList(ContentsSearchParamsDTO searchDTO) {
        Page<ContentsEntity> response = contentsService.getContentsList(searchDTO);
        return ResponseEntity.status(ResponseResult.SUCCESS_READ.getCode()).body(response);
    }
    
    /**
     * 콘텐츠 등록
     */
    @PostMapping("/contents")
    @Operation(summary = "콘텐츠 생성", description = "콘텐츠를 생성합니다.")
    public ResponseEntity<Object> insertContent(@RequestBody ContentsEntity apiEntity) {
        ContentsEntity save = contentsService.insertContent(apiEntity);
        ApiResponse<Long> response = new ApiResponse<>(ResponseResult.SUCCESS_SAVE, save.getContentId());
        return ResponseEntity.status(ResponseResult.SUCCESS_SAVE.getCode()).body(response);
    }
    
    /**
     * 콘텐츠 등록 반영
     */
    @PostMapping("/contents/{contentId}/finalize")
    @Operation(summary = "콘텐츠 등록", description = "최초 생성 후 DB에 반영합니다.")
    public ResponseEntity<Object> finalizeContentRegistration(@PathVariable("contentId") Long contentId, @RequestBody ContentsDTO dto) {
        ContentsEntity finalize = contentsService.updateContent(contentId, dto); 
        ApiResponse<ContentsEntity> response = new ApiResponse<>(ResponseResult.SUCCESS_SAVE, finalize);
        return ResponseEntity.status(ResponseResult.SUCCESS_SAVE.getCode()).body(response);
    }
    
    /**
     * 콘텐츠 상세
     */
    @GetMapping("/contents/{contentId}")
    @Operation(summary = "콘텐츠 상세", description = "ID로 콘텐츠 상세 정보를 조회합니다.")
    public ResponseEntity<Object> getContentDetail(@PathVariable("contentId") Long contentId) {
        ContentsEntity detail = contentsService.getContentDetail(contentId);
        ApiResponse<ContentsEntity> response = new ApiResponse<>(ResponseResult.SUCCESS_READ, detail);
        return ResponseEntity.status(ResponseResult.SUCCESS_READ.getCode()).body(response);
    }

    /**
     * 콘텐츠 수정
     */
    @PutMapping("/contents/{contentId}")
    @Operation(summary = "콘텐츠 수정", description = "ID로 콘텐츠 상세 정보를 수정합니다.")
    public ResponseEntity<Object> updateContent(@PathVariable("contentId") Long contentId, @RequestBody ContentsDTO dto) {
        ContentsEntity update = contentsService.updateContent(contentId, dto);
        ApiResponse<ContentsEntity> response = new ApiResponse<>(ResponseResult.SUCCESS_UPDATE, update);
        return ResponseEntity.status(ResponseResult.SUCCESS_UPDATE.getCode()).body(response);
    }
    
    /**
     * 콘텐츠 삭제
     */
    @DeleteMapping("/contents/{contentId}")
    @Operation(summary = "콘텐츠 삭제", description = "ID로 콘텐츠 상세 정보를 삭제합니다.")
    public ResponseEntity<String> deleteContent(@PathVariable("contentId") Long contentId) {
    	contentsService.deleteContent(contentId);
        return ResponseEntity.status(ResponseResult.SUCCESS_DELETE.getCode()).body(ResponseResult.SUCCESS_DELETE.getMessage());
    }
}
package com.demo.toy.common.util;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.demo.toy.common.response.ApiResponse;
import com.demo.toy.common.response.ResponseResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "공통 유틸 API", description = "공통 유틸 API")
public class CommonUtilController {

    private final CommonUtilService commonUtilService;

    public CommonUtilController(CommonUtilService commonUtilService) {
        this.commonUtilService = commonUtilService;
    }
    
    /**
     * 공통 이미지 업로드 API.
     * ContentId, ContentType, imageRole 등을 쿼리 파라미터로 받아 파일 경로를 분류.
     * URL 예: POST /common/upload?contentId=1234&contentType=COMICS&imageRole=VOLUME_COVER&volumeNumber=1
     * @param file 업로드 파일
     * @param contentId 해당 콘텐츠 ID
     * @param contentType 콘텐츠의 대분류 (예: COMICS, WEBTOON..)
     * @param imageRole 이미지의 역할 (예: COVER, VOLUME_COVER)
     * @param volumeNumber 권수/회차 번호 (옵션)
     * @return 업로드된 파일의 접근 URL
     */
    @PostMapping("/common/upload")
    @Operation(summary = "공통 이미지 업로드", description = "이미지를 등록/수정하며, 경로 관리를 위해 메타데이터를 받습니다.")
    public ResponseEntity<ApiResponse<String>> uploadContentImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("contentId") Long contentId,
            @RequestParam("contentType") String contentType,
            @RequestParam("imageRole") String imageRole,
            @RequestParam(value = "volumeNumber", required = false) Integer volumeNumber
    ) throws IOException {
        String filePath = commonUtilService.uploadContentImage(
            file, 
            contentId, 
            contentType, 
            imageRole, 
            volumeNumber
        );
        return ResponseEntity.status(ResponseResult.SUCCESS_SAVE.getCode()).body(new ApiResponse<>(ResponseResult.SUCCESS_SAVE, filePath));
    }
}
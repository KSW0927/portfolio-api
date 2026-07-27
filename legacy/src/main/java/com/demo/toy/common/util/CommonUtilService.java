package com.demo.toy.common.util;

import com.demo.toy.common.exception.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class CommonUtilService {

    @Value("${file.upload-dir}") 
    private String baseUploadDir;

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
    public String uploadContentImage(
            MultipartFile file, 
            Long contentId, 
            String contentType, 
            String imageRole, 
            Integer volumeNumber) {

        if (contentId == null || contentId <= 0 || contentType == null || imageRole == null) {
            throw new IllegalArgumentException("필수 업로드 정보(Content ID, Type, Role)가 누락되었습니다.");
        }
        
        // 1. 저장할 디렉토리 경로 구성
        // 경로 공식: {ContentType(소문자)}/{ContentID}/{ImageRole(소문자)}/[VolumeNumber]/
        StringBuilder relativePathBuilder = new StringBuilder();
        
        relativePathBuilder.append(contentType.toLowerCase()) 
                           .append(File.separator).append(contentId)   
                           .append(File.separator).append(imageRole.toLowerCase()); 
        
        // Volume Number가 유효하면 권수별 폴더 추가
        if (volumeNumber != null && volumeNumber > 0) {
            relativePathBuilder.append(File.separator).append(volumeNumber);
        }
        
        String relativePath = relativePathBuilder.toString();
        File uploadDir = new File(baseUploadDir, relativePath); 
        
        // 2. 파일명 결정
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new FileUploadException("파일 이름이 없습니다.");
        }

        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        
        try {
            // 3. 디렉토리 생성
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw new FileUploadException("업로드 폴더 생성 실패: " + uploadDir.getAbsolutePath());
            }

            // 4. 파일 저장
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);

            // 5. 클라이언트 접근 URL 반환 
            String urlPath = relativePath.replace(File.separator, "/");
            return "/uploads/" + urlPath + "/" + fileName;

        } catch (IOException e) {
            throw new FileUploadException("파일 업로드 실패", e);
        }
    }
}
package com.seokwon.notiflow.common.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.seokwon.notiflow.common.exception.BusinessException;
import com.seokwon.notiflow.common.exception.NotFoundException;
import com.seokwon.notiflow.common.response.ApiResponse;
import com.seokwon.notiflow.common.response.ResponseResult;

/**
 * 공통 예외 처리 핸들러
 */
@RestControllerAdvice(basePackages = "com.seokwon.notiflow")
public class RespExcpHandler {

    private static final Logger log = LoggerFactory.getLogger(RespExcpHandler.class);

    /**
     * 비즈니스 로직 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("비즈니스 예외 발생: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getResult().getCode());
        response.setMessage(ex.getResult().getMessage());
        return ResponseEntity.status(ex.getResult().getCode()).body(response);
    }

    /**
     * DB PK 위반 예외
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        // 이 예외는 회원가입 중복 아이디 케이스를 염두에 두고 만들어진 핸들러라 메시지가 그렇게 고정돼있는데,
        // 실제로는 PK/FK/NOT NULL 등 DB 제약조건 위반이면 전부 여기로 들어옴. 로그가 없으면 원인 파악이
        // 불가능해서 실제 원인(SQL 예외 원인 포함)을 남겨둠.
        log.error("DB 제약조건 위반 예외 발생", ex);
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ResponseResult.ERROR_DUPLICATE.getCode());
        response.setMessage(ResponseResult.ERROR_DUPLICATE.getMessage());
        return ResponseEntity.status(ResponseResult.ERROR_DUPLICATE.getCode()).body(response);
    }

    /**
     * 잘못된 요청
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("잘못된 요청 발생: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ResponseResult.ERROR_SERVER.getCode());
        response.setMessage(ResponseResult.ERROR_SERVER.getMessage());
        return ResponseEntity.status(ResponseResult.ERROR_SERVER.getCode()).body(response);
    }

    /**
     * 데이터 없음
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomNotFound(NotFoundException ex) {
        log.info("데이터 없음: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ResponseResult.ERROR_NOT_FOUND.getCode());
        response.setMessage(ex.getMessage());
        return ResponseEntity.status(ResponseResult.ERROR_NOT_FOUND.getCode()).body(response);
    }

    /**
     * 그 외 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("서버 내부 예외 발생", ex);
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ResponseResult.ERROR_SERVER.getCode());
        response.setMessage(ResponseResult.ERROR_SERVER.getMessage());
        return ResponseEntity.status(ResponseResult.ERROR_SERVER.getCode()).body(response);
    }
}

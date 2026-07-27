package com.seokwon.notiflow.common.response;

/**
 * 공통 응답 코드 Enum
 */
public enum ResponseResult {
    SUCCESS_SAVE            (201, "저장이 완료되었습니다."),
    SUCCESS_READ             (200, "조회가 완료되었습니다."),
    SUCCESS_UPDATE           (200, "수정이 완료되었습니다."),
    SUCCESS_DELETE           (200, "삭제가 완료되었습니다."),
    SUCCESS_SIGNUP           (200, "회원가입 성공"),
    SUCCESS_LOGIN            (200, "로그인 성공"),
    SUCCESS_LOGOUT           (200, "정상적으로 로그아웃 되었습니다."),
    ERROR_DUPLICATE          (400, "중복된 아이디 입니다."),
    ERROR_INVALID_PASSWORD   (401, "비밀번호가 일치하지 않습니다."),
    ERROR_NOT_AUTH           (403, "권한이 없습니다."),
    ERROR_NOT_FOUND          (404, "데이터를 찾을 수 없습니다."),
    ERROR_SERVER             (500, "서버 에러가 발생했습니다.");

    private final int code;
    private final String message;

    ResponseResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

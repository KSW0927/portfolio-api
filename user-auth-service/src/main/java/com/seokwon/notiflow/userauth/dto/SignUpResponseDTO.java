package com.seokwon.notiflow.userauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SignUpResponseDTO {
    private Long userNo;
    private String userId;
    private String message;

    public SignUpResponseDTO(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }
}

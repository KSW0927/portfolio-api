package com.seokwon.notiflow.userauth.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {
    private Long userNo;
    private String username;
    private String accessToken;
    private String refreshToken;
}

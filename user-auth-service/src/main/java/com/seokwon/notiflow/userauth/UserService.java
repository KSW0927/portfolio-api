package com.seokwon.notiflow.userauth;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seokwon.notiflow.common.exception.BusinessException;
import com.seokwon.notiflow.common.response.ResponseResult;
import com.seokwon.notiflow.common.security.jwt.JwtTokenProvider;
import com.seokwon.notiflow.userauth.dto.LoginResponseDTO;
import com.seokwon.notiflow.userauth.dto.SignUpRequestDTO;
import com.seokwon.notiflow.userauth.dto.SignUpResponseDTO;
import com.seokwon.notiflow.userauth.redis.RedisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO dto) {
        // 아이디 중복 체크
        if (userRepository.existsByUserId(dto.getUserId())) {
            throw new BusinessException(ResponseResult.ERROR_DUPLICATE);
        }

        // 비밀번호 암호화 및 엔티티 생성
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        UserEntity user = new UserEntity();
        user.SignUpEntity(dto);

        // DB 저장
        userRepository.save(user);

        // 응답 DTO 반환
        return new SignUpResponseDTO(
                user.getUserId(),
                user.getUsername() + "님 회원가입 되었습니다.");
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(String userId, String password) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ResponseResult.ERROR_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResponseResult.ERROR_INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Redis에 refreshToken 저장 (7일 만료)
        redisService.setRefreshToken(user.getUserId(), refreshToken, Duration.ofDays(7).toSeconds());

        // LoginResponseDTO 반환
        return new LoginResponseDTO(user.getUserNo(), user.getUsername(), accessToken, refreshToken);
    }

    // 로그아웃
    @Transactional(readOnly = true)
    public void logout(Long userNo) {
        UserEntity user = userRepository.findById(userNo)
                .orElseThrow(() -> new BusinessException(ResponseResult.ERROR_NOT_FOUND));

        // Redis에 저장된 Refresh Token 삭제
        redisService.deleteRefreshToken(user.getUserId());
    }
}

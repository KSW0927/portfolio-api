package com.seokwon.notiflow.userauth.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seokwon.notiflow.userauth.UserRepository;
import com.seokwon.notiflow.userauth.UserService;
import com.seokwon.notiflow.userauth.dto.SignUpRequestDTO;

import lombok.RequiredArgsConstructor;

/**
 * 게스트 체험(자동로그인)용 데모 계정 시딩
 * @description 프론트 로그인 페이지의 "게스트로 체험하기" 버튼이 사용하는 고정 계정.
 * 이미 존재하면 아무 작업도 하지 않으므로 로컬/운영 어디서 재기동해도 안전함.
 * 회원가입과 동일하게 UserService.signUp()을 그대로 태워서 비밀번호는 항상 BCrypt로 저장된다.
 */
@Component
@RequiredArgsConstructor
public class DemoUserSeeder implements CommandLineRunner {

    public static final String DEMO_USER_ID = "Guest";
    public static final String DEMO_PASSWORD = "guest1234!";

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUserId(DEMO_USER_ID)) {
            return;
        }

        SignUpRequestDTO demo = new SignUpRequestDTO();
        demo.setUserId(DEMO_USER_ID);
        demo.setPassword(DEMO_PASSWORD);
        demo.setUsername("게스트");
        demo.setEmail("guest@notiflow.demo");

        userService.signUp(demo);
    }
}

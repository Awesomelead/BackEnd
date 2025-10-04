package kr.co.awesomelead.groupware_backend.domain.user.service;

import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // loginId로 DB에서 사용자 정보를 조회
        User user =
            userRepository
                .findByEmail(email)
                .orElseThrow(
                    () ->
                        new UsernameNotFoundException(
                            email + " 에 해당하는 사용자를 찾을 수 없습니다."));

        // 조회된 사용자 정보를 바탕으로 UserDetails 객체를 생성하여 반환
        return new CustomUserDetails(user);
    }
}

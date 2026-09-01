package com.eatmate.security.provider;

import com.eatmate.security.dto.AccountContext;
import com.eatmate.security.dto.AuthenticateAccountDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 폼 로그인이 만들어 내는 인증 주체를 확인한다.
 *
 * <p>이 앱은 어디서나 {@code principal.getName()}을 <b>oauth2Id</b>로 취급한다.
 * 인가({@code TeamAccessService}), 채팅 내역 조회, 모임 생성이 모두 그 전제 위에 있다.
 *
 * <p>그런데 {@link CustomAuthenticationProvider}는 principal 자리에 {@link AccountContext}가
 * 아니라 그 안의 {@link AuthenticateAccountDto}를 넣는다. DTO는 {@code UserDetails}가 아니라서
 * {@code getName()}이 {@code toString()}으로 떨어진다.
 *
 * <p>부하 생성기가 폼 로그인으로 세션을 얻으려다 확인한 사실이다. → ISS-09
 */
@ExtendWith(MockitoExtension.class)
class FormLoginPrincipalTest {

    @Mock private UserDetailsService userDetailsService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private CustomAuthenticationProvider provider;

    @Test
    @DisplayName("폼 로그인의 principal.getName()은 oauth2Id도 이메일도 아니다")
    void formLoginPrincipalNameIsNotUsable() {
        AuthenticateAccountDto dto = AuthenticateAccountDto.builder()
                .id(1L).email("tester@eatmate.com").nickname("테스터")
                .password("encoded").roles("ROLE_USER").build();
        AccountContext context = AccountContext.builder()
                .authenticateAccountDto(dto)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        given(userDetailsService.loadUserByUsername("tester@eatmate.com")).willReturn(context);
        given(passwordEncoder.matches("pw", "encoded")).willReturn(true);

        Authentication result = provider.authenticate(
                new UsernamePasswordAuthenticationToken("tester@eatmate.com", "pw"));

        // principal 자리에 UserDetails(AccountContext)가 아니라 DTO가 들어간다.
        assertThat(result.getPrincipal()).isInstanceOf(AuthenticateAccountDto.class);

        // 그래서 getName()이 toString()으로 떨어진다. 이메일도 oauth2Id도 아니다.
        assertThat(result.getName())
                .isNotEqualTo("tester@eatmate.com")
                .startsWith("com.eatmate.security.dto.AuthenticateAccountDto@");
    }
}

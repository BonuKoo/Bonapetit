package com.eatmate.security.config;

import com.eatmate.security.handler.FormAuthenticationFailureHandler;
import com.eatmate.security.handler.FormAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@RequiredArgsConstructor
//@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final FormAuthenticationSuccessHandler successHandler;
    private final FormAuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        http
                .authorizeHttpRequests(auth-> auth
                        //정적 자원 설정
                        .requestMatchers("/css/**","/images/**","/js/**", "/favicon.*", "/*/icon-*").permitAll()
                        .requestMatchers("/", "/join").permitAll()
                        // 카카오 인증 콜백 경로 허용
                        .requestMatchers("/oauth/kakao/callback").permitAll()
                        // 카카오 로그아웃 경로 허용
                        .requestMatchers("/kakao/logout").permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email") //username->email
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .authenticationProvider(authenticationProvider)
        ;
        return http.build();
    }

}

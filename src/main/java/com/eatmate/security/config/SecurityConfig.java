package com.eatmate.security.config;

import com.eatmate.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService; // CustomOAuth2UserService 주입


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        http
                .authorizeHttpRequests(auth-> auth
                        //정적 자원 설정
                        .requestMatchers("/css/**","/img/**","/js/**", "/favicon.*", "/*/icon-*").permitAll()
                        .requestMatchers("/", "/join").permitAll()

                        .requestMatchers("/post/list").permitAll()
                        .requestMatchers("/post/detail/**").permitAll()
                        .requestMatchers("/notice").permitAll()
                        .requestMatchers("/notice/detail/**").permitAll()

                        // 카카오 인증 콜백 및 로그아웃
                        .requestMatchers("/login/oauth2/code/kakao").permitAll()
                        .requestMatchers("/login/oauth2/code/naver").permitAll()
                        .requestMatchers("/login/oauth2/code/google").permitAll()

                        .requestMatchers("/kakao/logout").permitAll()
                        .requestMatchers("/naver/logout").permitAll()
                        .requestMatchers("/google/logout").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email") //username->email
//                        .successHandler(successHandler)
//                        .failureHandler(failureHandler)
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // 성공 후 리디렉션될 URL 설정
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))) // OAuth2 로그인 설정

                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()))

                .authenticationProvider(authenticationProvider)
        ;
        return http.build();
    }
    
}

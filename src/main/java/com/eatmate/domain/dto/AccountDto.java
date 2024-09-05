package com.eatmate.domain.dto;

import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private Long account_id;
    private String email;    //로그인은 email로
    private String nick_name;
    private String password;
    private String roles;
    private String oauth2_id;
    private String access_token;
    private String provider;  // 로그인 제공자 (카카오, 네이버, 구글 등)

    //TODO : 추후 상세
    //private String city;
}

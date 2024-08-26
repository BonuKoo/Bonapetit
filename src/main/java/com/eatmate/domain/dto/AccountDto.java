package com.eatmate.domain.dto;

import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private Long account_id;
    private String email;    //로그인은 email로
    private String nickName;
    private String password;

    //TODO : 추후 상세
    //private String city;

}

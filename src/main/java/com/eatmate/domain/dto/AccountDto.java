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

    //TODO : 추후 상세
    //private String city;

}

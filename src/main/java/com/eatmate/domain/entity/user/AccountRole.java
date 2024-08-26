package com.eatmate.domain.entity.user;

import jakarta.persistence.*;


/*
    모임에 대한 권한
 */

@Entity
@Table(name = "account_role")
public class AccountRole {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_role_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}

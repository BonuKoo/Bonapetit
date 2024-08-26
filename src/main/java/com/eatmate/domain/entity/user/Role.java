package com.eatmate.domain.entity.user;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String roleName;

    @OneToMany(mappedBy = "role")
    private List<AccountRole> accountRoles = new ArrayList<>();

}

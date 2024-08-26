package com.eatmate.domain.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.*;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_team")
public class AccountTeam {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "account_team_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    //LEADER, PARTICIPANT
    @Column(name = "role")
    private String role;


}

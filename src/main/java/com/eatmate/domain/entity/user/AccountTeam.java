package com.eatmate.domain.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.*;

@NoArgsConstructor
@Entity
@Getter
@Table(name = "account_team")
public class AccountTeam {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "account_team_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id",nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "team_id",nullable = false)
    private Team team;

    //LEADER, PARTICIPANT
    @Column(name = "is_leader",nullable = false)
    private boolean isLeader;

    @Builder
    public AccountTeam(Long id, Account account, Team team, boolean isLeader) {
        this.id = id;
        this.account = account;
        this.team = team;
        this.isLeader = isLeader;
    }

    protected void updateTeam(Team team){
        this.team = team;
    }

    protected void updateAccount(Account account){
        this.account = account;
    }
}

package com.eatmate.domain.entity;

import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.*;

@Entity
public class Tag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tagName;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    public void attachPost(Team team) {
        this.team = team;
    }
}

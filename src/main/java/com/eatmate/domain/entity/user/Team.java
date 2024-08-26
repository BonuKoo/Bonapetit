package com.eatmate.domain.entity.user;

import com.eatmate.domain.entity.Post;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    private String teamName;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Account owner; //방장

    @OneToMany(mappedBy = "team")
    private List<AccountTeam> members = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    private List<Post> posts = new ArrayList<>();
}

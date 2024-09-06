package com.eatmate.domain.entity.map;

import com.eatmate.domain.entity.user.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class KakaoMap {
    @Id
    @Column(name = "map_id")
    private String id;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "place_url")
    private String placeUrl;

    @Column(name = "road_address_name")
    private String roadAddressName;

    @Column(name = "x")
    private String x;

    @Column(name = "y")
    private String y;

    // KakaoMap과 Team은 일대다 관계
    @OneToMany(mappedBy = "map", cascade = CascadeType.REMOVE)
    private List<Team> teamList;
}

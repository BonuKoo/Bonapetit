package com.eatmate.post.vo;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PostPageDto {

    private Long id;
    private String teamName;
    private String addressName;
    private String roadAddressName;
    //private String leaderName;
    private int memberSize;
    private LocalDateTime createdDate;

    @QueryProjection
    public PostPageDto(Long id, String teamName, String addressName, String roadAddressName,
                       //String leaderName,
                       int memberSize, LocalDateTime createdDate) {
        this.id = id;
        this.teamName = teamName;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        //this.leaderName = leaderName;
        this.memberSize = memberSize;
        this.createdDate = createdDate;
    }
}

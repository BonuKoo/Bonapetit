package com.eatmate.map.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MapVo {

    @JsonProperty("address_name")
    private String addressName;

//    private String category_group_code;
//    private String category_group_name;
//    private String category_name;
//    private String distance;

    @JsonProperty("id")
    private String id;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("place_url")
    private String placeUrl;

    @JsonProperty("road_address_name")
    private String roadAddressName;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

}

package com.eatmate.redis.config.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticePageFormCacheable {

    private Long id;
    private String title;
    private String author;
    private String content;

}

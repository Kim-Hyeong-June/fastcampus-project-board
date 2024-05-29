package com.fastcampus.projectboard.dto.response;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.UserAccountDto;

import java.time.LocalDateTime;

/**
 * DTO for {@link Article}
 */
public record ArticleResponse(
        Long id,
        String title,
        String content,
        String hashtag,
        LocalDateTime createdAt,
        String email ,
        String nickname
){
    public static ArticleResponse of(Long id, String title, String content, String hashtag, LocalDateTime createdAt, String email, String nickname) {
        return new ArticleResponse(id, title, content, hashtag, createdAt, email, nickname);
    }

    public static ArticleResponse from(ArticleDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()){
            nickname = dto.userAccountDto().userId();
        }
        return new ArticleResponse(dto.id(), dto.title(),
                dto.content(), dto.hashtag(), dto.createdAt(),
                dto.userAccountDto().email(), nickname);

    }
}

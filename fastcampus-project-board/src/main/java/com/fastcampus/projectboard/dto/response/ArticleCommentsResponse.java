package com.fastcampus.projectboard.dto.response;

import com.fastcampus.projectboard.dto.ArticleCommentDto;


import java.time.LocalDateTime;

/**
 * DTO for {@link com.fastcampus.projectboard.domain.ArticleComment}
 */
public record ArticleCommentsResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        String email ,
        String nickname
){
    public static ArticleCommentsResponse of (Long id, String content, LocalDateTime createdAt, String email, String nickname) {
        return new ArticleCommentsResponse(id, content, createdAt, email, nickname);
    }

    public static ArticleCommentsResponse from(ArticleCommentDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()){
            nickname = dto.userAccountDto().userId();
        }

        return new ArticleCommentsResponse(
                dto.id(),
                dto.content(),
                dto.createdAt(),
                dto.userAccountDto().email(),
                nickname
        );
    }

}

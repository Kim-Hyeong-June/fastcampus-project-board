package com.fastcampus.projectboard.dto.response;

import com.fastcampus.projectboard.dto.ArticleCommentDto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * DTO for {@link com.fastcampus.projectboard.domain.ArticleComment}
 */
public record ArticleCommentsResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        String email ,
        String nickname,
        String userId,
        Long parentCommentId,
        Set<ArticleCommentsResponse> childComments
        ){

    // 자식 댓글이 없는 경우
    public static ArticleCommentsResponse of(Long id, String content, LocalDateTime createdAt, String email, String nickname, String userId) {
        return ArticleCommentsResponse.of(id, content, createdAt, email, nickname, userId, null);
    }

    // 자식 댓글이 있는 경우
    public static ArticleCommentsResponse of(Long id, String content, LocalDateTime createdAt, String email, String nickname, String userId, Long parentCommentId) {
        Comparator<ArticleCommentsResponse> childCommentComparator =
                Comparator.comparing(ArticleCommentsResponse::createdAt)
                        .thenComparingLong(ArticleCommentsResponse::id);

        return new ArticleCommentsResponse(id, content, createdAt, email, nickname, userId, parentCommentId, new TreeSet<>(childCommentComparator));
    }

    public static ArticleCommentsResponse from(ArticleCommentDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()){
            nickname = dto.userAccountDto().userId();
        }

        return ArticleCommentsResponse.of(
                dto.id(),
                dto.content(),
                dto.createdAt(),
                dto.userAccountDto().email(),
                nickname,
                dto.userAccountDto().userId(),
                dto.parentCommentId()
        );
    }

    public boolean hasParentComment(){
        return parentCommentId != null;
    }
}

package com.fastcampus.projectboard.dto.response;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.dto.ArticleCommentDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.dto.HashtagDto;


import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DTO for {@link Article}
 * 게시글과 댓글이 같이 있는 response -> ArticleCommentsResponse 로 댓글들 응답
 * ArticleCommentsResponse 에 부모 아이디와 자식 댓글 객체들 담을 수 있게 설정
 */
public record ArticleWithCommentsResponse(
        Long id,
        String title,
        String content,
        Set<String> hashtags,
        LocalDateTime createdAt,
        String email ,
        String nickname,
        String userId,
        Set<ArticleCommentsResponse> articleCommentResponses
){
    public static ArticleWithCommentsResponse of(Long id,
                                                 String title,
                                                 String content,
                                                 Set<String> hashtags,
                                                 LocalDateTime createdAt,
                                                 String email,
                                                 String nickname,
                                                 String userId,
                                                 Set<ArticleCommentsResponse> articleCommentResponses ) {
        return new ArticleWithCommentsResponse(id, title, content, hashtags, createdAt, email, nickname, userId , articleCommentResponses);
    }

    public static ArticleWithCommentsResponse from(ArticleWithCommentsDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = dto.userAccountDto().userId();
        }
        return new ArticleWithCommentsResponse(
                dto.id(),
                dto.title(),
                dto.content(),
                dto.hashtagDtos().stream().map(HashtagDto::hashtagName).collect(Collectors.toUnmodifiableSet()),
                dto.createdAt(),
                dto.userAccountDto().email(),
                nickname,
                dto.userAccountDto().userId(),
                organizeChildComments(dto.articleCommentsDto())
        );
    }

    /*
    * 자식 comment 정렬
    * */ //DTO 에서 ArticleCommentsResponse 로 변환
    private static Set<ArticleCommentsResponse> organizeChildComments(Set<ArticleCommentDto> dtos){
        // DTO 에서 받은 댓글 response 로 변환하여 Map 으로 정렬

        Map<Long, ArticleCommentsResponse> map = dtos.stream()
                .map(ArticleCommentsResponse::from)
                .collect(Collectors.toMap(ArticleCommentsResponse::id,
                        Function.identity()));

        // Response 에서 부모가 있는 댓글 찾아서 자식 댓글 추가
        map.values().stream()
                .filter(ArticleCommentsResponse::hasParentComment)
                .forEach(comment -> {
                    ArticleCommentsResponse parentComment =
                            map.get(comment.parentCommentId());

                    parentComment.childComments().add(comment);
                });

        // 최상위 댓글들을 시간순 , id 순으로 정렬
        return map.values().stream()
                .filter(comment -> !comment.hasParentComment())
                .collect(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(ArticleCommentsResponse::createdAt).
                                reversed().thenComparingLong(ArticleCommentsResponse::id))));

    }
}

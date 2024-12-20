package com.fastcampus.projectboard.dto.response;

import com.fastcampus.projectboard.dto.ArticleCommentDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.dto.HashtagDto;
import com.fastcampus.projectboard.dto.UserAccountDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;

@DisplayName("DTO - 댓글들을 포함한 게시글 응답 테스트")
class ArticleWithCommentsResponseTest {
    @DisplayName("자식 댓글이 없는 게시글 + 댓글 dto를 api 응답으로 변환할 때, 댓글을 시간 내림차순 + ID 오름차순으로 정리한다.")
    @Test
    void givenArticleWithCommentsDtoWithoutChildComments_whenMapping_thenOrganizesCommentsWithCertainOrder() {
        LocalDateTime now = LocalDateTime.now();
        Set<ArticleCommentDto> articleCommentDtos =
                Set.of(createArticleCommentDto(1L, null, now),
                        createArticleCommentDto(2L, null, now.plusDays(1L)),
                        createArticleCommentDto(3L, null, now.plusDays(3L)),
                        createArticleCommentDto(4L, null, now),
                        createArticleCommentDto(5L, null, now.plusDays(5L)),
                        createArticleCommentDto(6L, null, now.plusDays(4L)),
                        createArticleCommentDto(7L, null, now.plusDays(2L)),
                        createArticleCommentDto(8L, null, now.plusDays(7L))
                );

        ArticleWithCommentsDto input = createArticleWithCommentsDto(articleCommentDtos);
        // 댓글 여러개 생성하고 게시물과 함께 있는 댓글 DTO 생성

        ArticleWithCommentsResponse actual = ArticleWithCommentsResponse.from(input);
        // 댓글 여러개 생성한 DTO를 Response 로 변환 (response 는 comment dto 객체를 받아
        //

        Assertions.assertThat(actual.articleCommentResponses())
                .containsExactly(
                        createArticleCommentResponse(8L, null, now.plusDays(7L)),
                        createArticleCommentResponse(5L, null, now.plusDays(5L)),
                        createArticleCommentResponse(6L, null, now.plusDays(4L)),
                        createArticleCommentResponse(3L, null, now.plusDays(3L)),
                        createArticleCommentResponse(7L, null, now.plusDays(2L)),
                        createArticleCommentResponse(2L, null, now.plusDays(1L)),
                        createArticleCommentResponse(1L, null, now),
                        createArticleCommentResponse(4L, null, now));
    }

    @DisplayName("게시글 + 댓글 dto를 api 응답으로 변환할 때, 댓글 부모 자식 관계를 각각의 규칙으로 정렬하여 정리한다.")
    @Test
    void givenArticleWithCommentsDto_whenMapping_thenOrganizesParentAndChildCommentsWithCertainOrders() {
        LocalDateTime now = LocalDateTime.now();
        Set<ArticleCommentDto> articleCommentDtos =
                Set.of(
                        createArticleCommentDto(1L, null, now),
                        createArticleCommentDto(2L, 1L, now.plusDays(1L)),
                        createArticleCommentDto(3L, 1L, now.plusDays(3L)),
                        createArticleCommentDto(4L, 1L, now),
                        createArticleCommentDto(5L, null, now.plusDays(5L)),
                        createArticleCommentDto(6L, null, now.plusDays(4L)),
                        createArticleCommentDto(7L, 6L, now.plusDays(2L)),
                        createArticleCommentDto(8L, 6L, now.plusDays(7L))
                        );
        ArticleWithCommentsDto input = createArticleWithCommentsDto(articleCommentDtos);

        ArticleWithCommentsResponse actual = ArticleWithCommentsResponse.from(input);
        Assertions.assertThat(actual.articleCommentResponses())
                .containsExactly(
                        createArticleCommentResponse(5L, null, now.plusDays(5)),
                        createArticleCommentResponse(6L, null, now.plusDays(4)),
                        createArticleCommentResponse(1L, null, now)
                        )
                .flatExtracting(ArticleCommentsResponse::childComments)
                .containsExactly(
                        createArticleCommentResponse(7L, 6L, now.plusDays(2L)),
                        createArticleCommentResponse(8L, 6L, now.plusDays(7L)),
                        createArticleCommentResponse(4L, 1L, now),
                        createArticleCommentResponse(2L, 1L, now.plusDays(1L)),
                        createArticleCommentResponse(3L, 1L, now.plusDays(3L))
                );
    }

    @DisplayName("게시글 + 댓글 dto를 api 응답으로 변환할 때, 부모 자식 관계 깊이(depth)는 제한이 없다.")
    @Test
    void givenArticleWithCommentsDto_whenMapping_thenOrganizesParentAndChildCommentsWithoutDepthLimit() {
        LocalDateTime now = LocalDateTime.now();
        Set<ArticleCommentDto> articleCommentDtos
                = Set.of(
                createArticleCommentDto(1L, null, now),
                createArticleCommentDto(2L, 1L, now.plusDays(1L)),
                createArticleCommentDto(3L, 2L, now.plusDays(2L)),
                createArticleCommentDto(4L, 3L, now.plusDays(3L)),
                createArticleCommentDto(5L, 4L, now.plusDays(4L)),
                createArticleCommentDto(6L, 5L, now.plusDays(5L)),
                createArticleCommentDto(7L, 6L, now.plusDays(6L)),
                createArticleCommentDto(8L, 7L, now.plusDays(7L))
        );

        ArticleWithCommentsDto input = createArticleWithCommentsDto(articleCommentDtos);

        ArticleWithCommentsResponse actual = ArticleWithCommentsResponse.from(input);

        Iterator<ArticleCommentsResponse> iterator =
                actual.articleCommentResponses().iterator();

        long i = 1L;
        while (iterator.hasNext()) {
            ArticleCommentsResponse articleCommentsResponse
                    = iterator.next();

            Assertions.assertThat(articleCommentsResponse)
                    .hasFieldOrPropertyWithValue("id", i)
                    .hasFieldOrPropertyWithValue("parentCommentId", i == 1L ? null : i - 1L)
                    .hasFieldOrPropertyWithValue("createdAt", now.plusDays(i - 1L));

            iterator = articleCommentsResponse.childComments().iterator();
            i++;
        }
    }


    private ArticleCommentsResponse createArticleCommentResponse(Long id ,
                                                                 Long parentId ,
                                                                 LocalDateTime createdAt) {
        return ArticleCommentsResponse.of(id,
                "test comment " + id,
                createdAt,
                "uno@mail.com",
                "Uno",
                "uno",
                parentId);
    }

    private ArticleWithCommentsDto createArticleWithCommentsDto(Set<ArticleCommentDto> articleCommentDtos) {
        return ArticleWithCommentsDto.of(
                1L,
                createUserAccountDto(),
                articleCommentDtos,
                "title",
                "content",
                Set.of(HashtagDto.of("java")),
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno");
    }

    private ArticleCommentDto createArticleCommentDto(long id, Long parentCommentId, LocalDateTime createdAt) {
        return ArticleCommentDto.of(
                id,
                1L,
                createUserAccountDto(),
                parentCommentId,
                "test comment " + id,
                createdAt,
                "uno",
                createdAt,
                "uno");
    }


    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of(
                "uno",
                "password",
                "uno@mail.com",
                "Uno",
                "This is memo",
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno");
    }

}

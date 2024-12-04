package com.fastcampus.projectboard.service;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.ArticleComment;
import com.fastcampus.projectboard.domain.Hashtag;
import com.fastcampus.projectboard.domain.UserAccount;
import com.fastcampus.projectboard.dto.ArticleCommentDto;
import com.fastcampus.projectboard.dto.UserAccountDto;
import com.fastcampus.projectboard.repository.ArticleCommentRepository;
import com.fastcampus.projectboard.repository.ArticleRepository;
import com.fastcampus.projectboard.repository.UserAccountRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.hibernate.boot.model.internal.EmbeddableBinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;


@DisplayName("비지니스 로직 - 댓글")
@ExtendWith(MockitoExtension.class)
class ArticleCommentServiceTest {
    @InjectMocks private ArticleCommentService sut;

    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleCommentRepository articleCommentRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @DisplayName("게시글 ID로 조회하면, 해당하는 댓글 리스트를 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticleComments_thenReturnsArticleComments() {

        Long articleId = 1L;

        ArticleComment expectedParentComment = createArticleComment(1L, "parent content");
        ArticleComment expectedChildComment = createArticleComment(2L, "child content");

        expectedChildComment.setParentCommentId(expectedParentComment.getId());

        BDDMockito.given(articleCommentRepository.findByArticle_Id(articleId))
                .willReturn(List.of(
                        expectedParentComment,
                        expectedChildComment
                ));

        List<ArticleCommentDto> actual = sut.searchArticleComments(articleId);

        Assertions.assertThat(actual).hasSize(2);
        Assertions.assertThat(actual)
                .extracting("id", "articleId", "parentCommentId", "content")
                .containsExactlyInAnyOrder(
                        Tuple.tuple(1L, 1L, null, "parent content"),
                        Tuple.tuple(2L, 1L, 1L, "child content")
                );

        then(articleCommentRepository).should().findByArticle_Id(articleId);

    }


    @DisplayName("댓글 ID를 입력하면, 댓글을 삭제한다.")
    @Test
    void givenArticleCommentId_whenDeletingArticleComment_thenDeletesArticleComment() {
        Long articleCommentId = 1L;
        String userId = "uno";


        willDoNothing().given(articleCommentRepository).deleteByIdAndUserAccount_UserId(articleCommentId,userId);

        sut.deleteArticleComment(articleCommentId, userId);

        then(articleCommentRepository).should()
                .deleteByIdAndUserAccount_UserId(articleCommentId,userId);
    }

    @DisplayName("부모 댓글 ID와 댓글 정보를 입력하면, 대댓글을 저장한다.")
    @Test
    void givenParentCommentIdAndArticleCommentInfo_whenSaving_thenSavesChildComment() {
        Long parentCommentId = 1L;
        ArticleComment parent = createArticleComment(parentCommentId, "댓글");
        ArticleCommentDto child = createArticleCommentDto(parentCommentId, "대댓글");

        BDDMockito.given(articleRepository.getReferenceById(child.articleId())).willReturn(createArticle());

        BDDMockito.given(userAccountRepository.getReferenceById(child.userAccountDto().userId()))
                .willReturn(createUserAccount());

        BDDMockito.given(articleCommentRepository.getReferenceById(child.parentCommentId()))
                .willReturn(parent);


        sut.saveArticleComment(child);

        Assertions.assertThat(child.parentCommentId()).isNotNull();
        then(articleRepository).should().getReferenceById(child.articleId());

        then(userAccountRepository).should().getReferenceById(child.userAccountDto().userId());
        then(articleCommentRepository).should().getReferenceById(child.parentCommentId());
    }



    private ArticleCommentDto createArticleCommentDto(String content){
        return createArticleCommentDto(null, content);
    }

    private ArticleCommentDto createArticleCommentDto(Long parentCommentId , String content){
        return createArticleCommentDto(1L, parentCommentId, content);
    }


    private ArticleCommentDto createArticleCommentDto(Long id , Long parentCommentId , String content){
        return ArticleCommentDto.of(
                id,
                1L,
                createUserAccountDto(),
                parentCommentId,
                content,
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno"
        );
    }


    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of("uno",
                "password",
                "uno@mail.com",
                "Uno",
                "This is memo",
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno");
    }

    private ArticleComment createArticleComment(Long id , String content) {
        ArticleComment articleComment = ArticleComment.of(createArticle(), createUserAccount(), content);
        ReflectionTestUtils.setField(articleComment, "id", id);
        return articleComment;
    }

    private UserAccount createUserAccount() {
        return UserAccount.of("uno", "password", "uno@gmail.com", "Uno", null);
    }

    private Article createArticle(){
        Article article = Article.of(createUserAccount(), "title", "content");
        ReflectionTestUtils.setField(article, "id", 1L);
        article.addHashtags(Set.of(createHashtag(article)));
        return article;

    }

    private Hashtag createHashtag(Article article) {
        return Hashtag.of("java");
    }


}

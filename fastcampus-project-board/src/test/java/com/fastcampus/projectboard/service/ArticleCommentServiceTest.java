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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.willDoNothing;


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
        ArticleComment expected = createArticleComment("content");
        BDDMockito.given(articleCommentRepository.findByArticle_Id(articleId)).willReturn(List.of(expected));

        List<ArticleCommentDto> actual = sut.searchArticleComments(articleId);
        Assertions.assertThat(actual).hasSize(1)
                .first().hasFieldOrPropertyWithValue("content", expected.getContent());

        BDDMockito.then(articleCommentRepository).should().findByArticle_Id(articleId);
    }


    @DisplayName("댓글 ID를 입력하면, 댓글을 삭제한다.")
    @Test
    void givenArticleCommentId_whenDeletingArticleComment_thenDeletesArticleComment() {
        Long articleCommentId = 1L;
        String userId = "uno";


        willDoNothing().given(articleCommentRepository).deleteByIdAndUserAccount_UserId(articleCommentId,userId);

        sut.deleteArticleComment(articleCommentId, userId);

        BDDMockito.then(articleCommentRepository).should()
                .deleteByIdAndUserAccount_UserId(articleCommentId,userId);
    }

    private ArticleCommentDto createArticleCommentDto(String content) {
        return ArticleCommentDto.of(1L, 1L, createUserAccountDto(), content, LocalDateTime.now(), "uno", LocalDateTime.now(), "uno");
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

    private ArticleComment createArticleComment(String content) {
        return ArticleComment.of(createArticle(), createUserAccount() , content);
    }

    private UserAccount createUserAccount() {
        return UserAccount.of("uno", "password", "uno@gmail.com", "Uno", null);
    }

    private Article createArticle(){
        Article article = Article.of(createUserAccount(), "title", "content");
        article.addHashtags(Set.of(createHashtag(article)));
        return article;

    }

    private Hashtag createHashtag(Article article) {
        return Hashtag.of("java");
    }


}

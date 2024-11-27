package com.fastcampus.projectboard.repository;


import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.Hashtag;
import com.fastcampus.projectboard.domain.UserAccount;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA연결 테스트")
//@Import(JpaRepositoryTest.TestJpaConfig.class)
@DataJpaTest
class JpaRepositoryTest {
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final UserAccountRepository userAccountRepository;
    private final HashtagRepository hashtagRepository;

    JpaRepositoryTest(@Autowired ArticleRepository articleRepository,
                      @Autowired ArticleCommentRepository articleCommentRepository,
                      @Autowired UserAccountRepository userAccountRepository,
                      @Autowired HashtagRepository hashtagRepository) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.userAccountRepository = userAccountRepository;
        this.hashtagRepository = hashtagRepository;
    }

    @Test
    @DisplayName("select Test")
    void given_when_then() {
        List<Article> articles =
                articleRepository.findAll();

        assertThat(articles).isNotNull().hasSize(123);
    }

    @Test
    @DisplayName("insert Test")
    void givenTestData_when_inserting_thenWorksFine() {
        long previousCount = articleRepository.count();

        UserAccount userAccount = userAccountRepository.save(UserAccount.of("newUno", "pw", null, null, null));

        Article article = Article.of(userAccount , "new article", "new content");

        article.addHashtags(Set.of(Hashtag.of("spring")));
        articleRepository.save(article);
        Assertions.assertThat(articleRepository.count()).isEqualTo(previousCount + 1);

    }

    @Test
    @DisplayName("updating Test")
    void givenTestData_whenUpdating() {

        Article article = articleRepository.findById(1L).orElseThrow();

        Hashtag updatedHashtag = Hashtag.of("spring boot");

        article.clearHashtags();
        article.addHashtags(Set.of(updatedHashtag));

        Article savedArticle = articleRepository.saveAndFlush(article);
        Assertions.assertThat(savedArticle.getHashtags()).
                hasSize(1).extracting("hashtagName", String.class)
                .containsExactly(updatedHashtag.getHashtagName());

    }

    @Test
    void givenTestData(){
        Article article = articleRepository.findById(1L).orElseThrow();
        long previousArticleCount = articleRepository.count();

        long previousArticleCommentCount = articleCommentRepository.count();
        int deletedCommentSize = article.getArticleComments().size();

        articleRepository.delete(article);
        Assertions.assertThat(articleRepository.count()).isEqualTo(previousArticleCount-1);
        Assertions.assertThat(articleCommentRepository.count()).isEqualTo(previousArticleCommentCount - deletedCommentSize);
    }

    @DisplayName("[Querydsl] 전체 hashtag 리스트에서 이름만 조회하기")
    @Test
    void givenNothing_whenQueryingHashtags_thenReturnsHashtagNames() {
        List<String> hashtagNames = hashtagRepository.findAllHashtagNames();
        Assertions.assertThat(hashtagNames).hasSize(19);
    }

    @DisplayName("[Querydsl] hashtag로 페이징된 게시글 검색하기")
    @Test
    void givenHashtagNamesAndPageable_whenQueryingArticles_thenReturnsArticlePage() {

        List<String> hashtagNames = List.of("blue", "crimson", "fuscia");
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Order.desc("hashtags.hashtagName"), Sort.Order.asc("title")));

        Page<Article> articlePage = articleRepository.findByHashtagNames(hashtagNames, pageable);

        Assertions.assertThat(articlePage.getContent()).hasSize(pageable.getPageSize());
        Assertions.assertThat(articlePage.getContent().get(0).getTitle())
                .isEqualTo("Fusce posuere felis sed lacus.");

        Assertions.assertThat(articlePage.getContent().get(0).getHashtags())
                .extracting("hashtagName", String.class)
                .containsExactly("fuscia");


    }

    @EnableJpaAuditing
    @TestConfiguration
    static class TestJpaConfig{
        @Bean
        AuditorAware<String> auditorAware(){
            return () -> Optional.of("uno");
        }
    }
}

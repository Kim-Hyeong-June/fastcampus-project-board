package com.fastcampus.projectboard.repository;


import com.fastcampus.projectboard.config.JpaConfig;
import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.UserAccount;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JPA연결 테스트")
@Import(JpaRepositoryTest.TestJpaConfig.class)
@DataJpaTest
class JpaRepositoryTest {
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final UserAccountRepository userAccountRepository;

    JpaRepositoryTest(@Autowired ArticleRepository articleRepository, @Autowired ArticleCommentRepository articleCommentRepository, @Autowired UserAccountRepository userAccountRepository) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.userAccountRepository = userAccountRepository;
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

        Article article = Article.of(userAccount , "new article", "new content", "#spring");

        articleRepository.save(article);
        Assertions.assertThat(articleRepository.count()).isEqualTo(previousCount + 1);

    }

    @Test
    @DisplayName("updating Test")
    void givenTestData_whenUpdating() {

        Article article = articleRepository.findById(1L).orElseThrow();
        String updatedHashtag = "#springboot";
        article.setHashtag(updatedHashtag);

        Article savedArticle = articleRepository.saveAndFlush(article);
        Assertions.assertThat(savedArticle).hasFieldOrPropertyWithValue("hashtag", updatedHashtag);
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

    @EnableJpaAuditing
    @TestConfiguration
    static class TestJpaConfig{
        @Bean
        AuditorAware<String> auditorAware(){
            return () -> Optional.of("Uno");
        }
    }
}

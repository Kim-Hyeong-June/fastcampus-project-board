package com.fastcampus.projectboard.service;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.Hashtag;
import com.fastcampus.projectboard.domain.UserAccount;
import com.fastcampus.projectboard.domain.type.SearchType;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.dto.HashtagDto;
import com.fastcampus.projectboard.dto.UserAccountDto;
import com.fastcampus.projectboard.repository.ArticleRepository;
import com.fastcampus.projectboard.repository.HashtagRepository;
import com.fastcampus.projectboard.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.boot.model.internal.EmbeddableBinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.as;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@DisplayName("비지니스 로직 - 게시글")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @InjectMocks
    private ArticleService sut;

    @Mock
    private HashtagService hashtagService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private HashtagRepository hashtagRepository;


    @DisplayName("검색어 없이 게시글을 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenNoSearchParameters_whenSearchingArticles_thenReturnsArticlePage() {
        Pageable pageable = Pageable.ofSize(20);
        given(articleRepository.findAll(pageable)).willReturn(Page.empty());

        Page<ArticleDto> articles = sut.searchArticles(null, null, pageable);
        Assertions.assertThat(articles).isEmpty();
        BDDMockito.then(articleRepository).should().findAll(pageable);
    }

    @DisplayName("검색어 없이 게시글을 해시태그 검색하면, 빈 페이지를 반환한다.")
    @Test
    void givenNoSearchParameters_whenSearchingArticlesViaHashtag_thenReturnsEmptyPage() {
        Pageable pageable = Pageable.ofSize(20);

        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(null, pageable);
        Assertions.assertThat(articles).isEqualTo(Page.empty(pageable));

        BDDMockito.then(hashtagRepository).shouldHaveNoInteractions();
        BDDMockito.then(articleRepository).shouldHaveNoInteractions();

    }

    @DisplayName("없는 해시태그를 검색하면, 빈 페이지를 반환한다.")
    @Test
    void givenNonexistentHashtag_whenSearchingArticlesViaHashtag_thenReturnsEmptyPage() {
        String hashtagName = "난 없지롱";
        Pageable pageable = Pageable.ofSize(20);

        BDDMockito.given(articleRepository.findByHashtagNames(List.of(hashtagName), pageable)).willReturn(new PageImpl<>(List.of(), pageable, 0));
        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(hashtagName, pageable);

        Assertions.assertThat(articles).isEqualTo(Page.empty(pageable));

        BDDMockito.then(articleRepository).should().findByHashtagNames(List.of(hashtagName), pageable);


    }


    @DisplayName("검색어와 함께 게시글을 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenSearchParameters_whenSearchingArticles_thenReturnsArticlePage() {
        SearchType searchType = SearchType.TITLE;
        String searchKeyword = "title";

        Pageable pageable = Pageable.ofSize(20);

        given(articleRepository.findByTitleContaining(searchKeyword, pageable)).willReturn(Page.empty());
        Page<ArticleDto> articles = sut.searchArticles(searchType, searchKeyword, pageable);

        Assertions.assertThat(articles).isEmpty();
        BDDMockito.then(articleRepository).should().findByTitleContaining(searchKeyword, pageable);
    }

    @DisplayName("게시글을 조회하면, 게시글을 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticle_thenReturnsArticle() {
        Long articleId = 1L;
        Article article = createArticle();

        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
        ArticleDto dto = sut.getArticle(articleId);
        Assertions.assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtagDtos", article.getHashtags().stream().map(HashtagDto::from)
                .collect(Collectors.toUnmodifiableSet()));


        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    
    @DisplayName("게시글 ID로 조회하면, 댓글 달긴 게시글을 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticleWithComments_thenReturnsArticleWithComments() {
        Long articleId = 1L;
        Article article = createArticle();
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        ArticleWithCommentsDto dto = sut.getArticleWithComments(articleId);
        Assertions.assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtagDtos", article.getHashtags().stream().map(HashtagDto::from)
                        .collect(Collectors.toUnmodifiableSet()));

        BDDMockito.then(articleRepository).should().findById(articleId);
    }


    @DisplayName("댓글 달린 게시글이 없으면, 예외를 던진다.")
    @Test
    void givenNonexistentArticleId_whenSearchingArticleWithComments_thenThrowsException() {
        Long articleId = 0L;
        BDDMockito.given(articleRepository.findById(articleId))
                .willReturn(Optional.empty());
        Throwable t = catchThrowable(() -> sut.getArticleWithComments(articleId));
        Assertions.assertThat(t)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("게시글이 없습니다 - articleId: " + articleId);
        BDDMockito.then(articleRepository).should()
                .findById(articleId);
    }

    @DisplayName("없는 게시글을 조회하면, 예외를 던진다.")
    @Test
    void givenNonexistentArticleId_whenSearchingArticle_thenThrowsException() {
        Long articleId = 0L;
        given(articleRepository.findById(articleId)).willReturn(Optional.empty());
        Throwable t = catchThrowable(() -> sut.getArticle(articleId));

        Assertions.assertThat(t)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("게시글이 없습니다 - articleId: " + articleId);
        BDDMockito.then(articleRepository).should().findById(articleId);
    }

    @DisplayName("게시글 정보를 입력하면, 본문에서 해시태그 정보를 추출하여 해시태그 정보가 포함된 게시글을 생성한다.")
    @Test
    void givenArticleInfo_whenSavingArticle_thenSavesArticle() {
        ArticleDto dto = createArticleDto();

        Set<String> expectedHashtagNames = Set.of("java", "spring");
        Set<Hashtag> expectedHashtags = new HashSet<>();
        expectedHashtags.add(createHashtag("java"));

        BDDMockito.given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(createUserAccount());
        given(articleRepository.save(any(Article.class))).willReturn(createArticle());
        given(hashtagService.parseHashtagNames(dto.content())).willReturn(expectedHashtagNames);
        given(hashtagService.findHashtagsByNames(expectedHashtagNames)).willReturn(expectedHashtags);

        sut.saveArticle(dto);

        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(hashtagService).should().parseHashtagNames(dto.content());
        BDDMockito.then(hashtagService).should().findHashtagsByNames(expectedHashtagNames);
        BDDMockito.then(articleRepository).should().save(any(Article.class));


    }


    //@WithMockUser
    @DisplayName("게시글의 수정 정보를 입력하면, 게시글을 수정한다.")
    @Test
    void givenModifiedArticleInfo_whenUpdatingArticle_thenUpdatesArticle() {
        Article article = createArticle();
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용");


        Set<String> expectedHashtagNames = Set.of("SpringBoot");
        Set<Hashtag> expectedHashtags = new HashSet<>();


        given(articleRepository.getReferenceById(dto.id())).willReturn(article);
        given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(dto.userAccountDto().toEntity());


        willDoNothing().given(articleRepository).flush();

        willDoNothing().given(hashtagService).deleteHashtagWithoutArticles(any());

        given(hashtagService.parseHashtagNames(dto.content())).willReturn(expectedHashtagNames);

        given(hashtagService.findHashtagsByNames(expectedHashtagNames)).willReturn(expectedHashtags);



        sut.updateArticle(dto.id() , dto);
        Assertions.assertThat(article)
                .hasFieldOrPropertyWithValue("title", dto.title())
                .hasFieldOrPropertyWithValue("content", dto.content())
                .extracting("hashtags", as(InstanceOfAssertFactories.COLLECTION))
                .hasSize(1)
                .extracting("hashtagName")
                .containsExactly("springboot");

        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());
        BDDMockito.then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        BDDMockito.then(hashtagService).should(times(2)).deleteHashtagWithoutArticles(any());
        BDDMockito.then(hashtagService).should().findHashtagsByNames(expectedHashtagNames);
        BDDMockito.then(hashtagService).should().parseHashtagNames(dto.content());


    }


    @DisplayName("없는 게시글의 수정 정보를 입력하면, 경고 로그를 찍고 아무 것도 하지 않는다.")
    @Test
    void givenNonexistentArticleInfo_whenUpdatingArticle_thenLogsWarningAndDoesNothing() {
        ArticleDto dto = createArticleDto("새 타이틀", "새 내용");
        given(articleRepository.getReferenceById(dto.id())).willThrow(EntityNotFoundException.class);
        sut.updateArticle(dto.id(), dto);
        BDDMockito.then(articleRepository).should().getReferenceById(dto.id());

        BDDMockito.then(userAccountRepository).shouldHaveNoInteractions();
        BDDMockito.then(hashtagService).shouldHaveNoInteractions();

    }

    @DisplayName("게시글 수를 조회하면, 게시글 수를 반환한다.")
    @Test
    void givenNothing_whenCountingArticles_thenReturnsArticleCount() {
        long expected = 0L;
        BDDMockito.given(articleRepository.count()).willReturn(expected);
        long actual = sut.getArticleCount();
        Assertions.assertThat(actual).isEqualTo(expected);
        BDDMockito.then(articleRepository).should().count();
    }



    @DisplayName("게시글의 ID를 입력하면, 게시글을 삭제한다")
    @Test
    void givenArticleId_whenDeletingArticle_thenDeletesArticle() {

        Long articleId = 1L;
        String userId = "uno";

        given(articleRepository.getReferenceById(articleId)).willReturn(createArticle());

        willDoNothing().given(articleRepository).deleteByIdAndUserAccount_UserId(articleId , userId);
        willDoNothing().given(articleRepository).flush();
        willDoNothing().given(hashtagService).deleteHashtagWithoutArticles(any());

        sut.deleteArticle(articleId , userId);

        BDDMockito.then(articleRepository).should().deleteByIdAndUserAccount_UserId(articleId, userId);
        BDDMockito.then(articleRepository).should().flush();

        BDDMockito.then(hashtagService).should(times(2)).deleteHashtagWithoutArticles(any());
    }

    @DisplayName("게시글을 해시태그 검색하면, 게시글 페이지를 반환한다.")
    @Test
    void givenHashtag_whenSearchingArticlesViaHashtag_thenReturnsArticlesPage() {
        String hashtagName = "java";
        Pageable pageable = Pageable.ofSize(20);

        Article expectedArticle = createArticle();

        BDDMockito.given(articleRepository.findByHashtagNames(List.of(hashtagName), pageable)).willReturn(new PageImpl<>(List.of(expectedArticle), pageable, 1));
        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(hashtagName, pageable);

        Assertions.assertThat(articles).isEqualTo(new PageImpl<>(List.of(ArticleDto.from(expectedArticle)), pageable, 1));
        BDDMockito.then(articleRepository).should().findByHashtagNames(List.of(hashtagName), pageable);


    }

    @DisplayName("해시태그를 조회하면, 유니크 해시태그 리스트를 반환한다")
    @Test
    void givenNothing_whenCalling_thenReturnsHashtags() {
        Article article = createArticle();

        List<String> expectedHashtags = List.of("java", "spring", "boot");

        given(hashtagRepository.findAllHashtagNames()).willReturn(expectedHashtags);

        List<String> actualHashtags = sut.getHashTags();
        Assertions.assertThat(actualHashtags).isEqualTo(expectedHashtags);
        BDDMockito.then(hashtagRepository).should().findAllHashtagNames();

    }


    private Article createArticle(){
        return createArticle(1L);
    }

    private Article createArticle(Long id) {
        Article article = Article.of(
                createUserAccount(),
                "title",
                "content");

        article.addHashtags(Set.of(
                createHashtag(1L, "java"),
                createHashtag(2L, "spring")
        ));

        ReflectionTestUtils.setField(article, "id", id);

        return article;
    }

    private UserAccount createUserAccount(){
        return createUserAccount("uno");
    }

    private Hashtag createHashtag(Long id , String hashtagName){
        Hashtag hashtag = Hashtag.of(hashtagName);
        ReflectionTestUtils.setField(hashtag , "id" , id);
        return hashtag;
    }

    private Hashtag createHashtag(String hashtagName){
        return createHashtag(hashtagName);
    }

    private HashtagDto createHashtagDto(){
        return HashtagDto.of("java");
    }

    private UserAccount createUserAccount(String userId) {
        return UserAccount.of(
                userId,
                "password",
                "uno@email.com",
                "Uno",
                null);
    }
    private UserAccountDto createUserAccountDto(){
        return UserAccountDto.of(
                "uno",
                "password",
                "uno@mail.com",
                "Uno",
                "This is memo",
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno"
        );
    }

    private ArticleDto createArticleDto(){
        return createArticleDto("title", "content");
    }

    private ArticleDto createArticleDto(String title, String content) {
        return ArticleDto.of(1L,
                createUserAccountDto(),
                title,
                content,
                null,
                LocalDateTime.now(),
                "Uno",
                LocalDateTime.now(),
                "Uno");
    }
}

package com.fastcampus.projectboard.controller;

import com.fastcampus.projectboard.config.TestSecurityConfig;
import com.fastcampus.projectboard.domain.constant.FormStatus;
import com.fastcampus.projectboard.domain.type.SearchType;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.dto.HashtagDto;
import com.fastcampus.projectboard.dto.UserAccountDto;
import com.fastcampus.projectboard.dto.request.ArticleRequest;
import com.fastcampus.projectboard.service.ArticleService;
import com.fastcampus.projectboard.service.PaginationService;
import com.fastcampus.projectboard.util.FormDataEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("View 컨트롤러 - 게시글")
@Import({TestSecurityConfig.class, FormDataEncoder.class})
@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

    private final MockMvc mvc;
    private final FormDataEncoder formDataEncoder;

    @MockBean
    private ArticleService articleService;

    @MockBean
    private PaginationService paginationService;

    ArticleControllerTest(@Autowired  MockMvc mvc, @Autowired FormDataEncoder formDataEncoder) {
        this.mvc = mvc;
        this.formDataEncoder = formDataEncoder;
    }

    //@Disabled("구현중")
    @DisplayName("[view][get] 게시글 리스트 (게시판) 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticlesView_thenReturnsArticlesView() throws Exception {
        BDDMockito.given(articleService.searchArticles(eq(null), eq(null), any(Pageable.class)))
                .willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(anyInt() , anyInt())).willReturn(List.of(0,1,2,3,4));

        mvc.perform(get("/articles"))
                .andExpect(status().isOk())
                .andExpect(result -> content().contentTypeCompatibleWith(MediaType.TEXT_HTML_VALUE))
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attributeExists("searchTypes"))
                .andExpect(model().attribute("searchTypeHashtag", SearchType.HASHTAG));


        BDDMockito.then(articleService).should().searchArticles(eq(null), eq(null), any(Pageable.class));
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(anyInt(), anyInt());
    }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 검색어와 함께 호출")
    @Test
    void givenSearchKeyword_whenSearchingArticlesView_thenReturnsArticlesView() throws Exception {
        SearchType searchType = SearchType.TITLE;
        String searchValue = "title";
        BDDMockito.given(articleService.searchArticles(eq(searchType), eq(searchValue), any(Pageable.class))).willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(anyInt(), anyInt())).willReturn(List.of(0, 1, 2, 3, 4));

        mvc.perform(get("/articles")
                        .queryParam("searchType", searchType.name())
                        .queryParam("searchValue", searchValue))
                .andExpect(status().isOk())
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("searchTypes"));
        BDDMockito.then(articleService).should().searchArticles(eq(searchType), eq(searchValue), any(Pageable.class));
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(anyInt(), anyInt());
    }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 페이징, 정렬 기능")
    @Test
    void givenPagingAndSortingParams_whenSearchingArticlesPage_thenReturnsArticlesPage() throws Exception {
        String sortName = "title";
        String direction = "desc";
        int pageNumber = 0;
        int pageSize = 5;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc(sortName)));
        List<Integer> barNumbers = List.of(1, 2, 3, 4, 5 );
        BDDMockito.given(articleService.searchArticles(null, null, pageable)).willReturn(Page.empty());
        BDDMockito.given(paginationService.getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages())).willReturn(barNumbers);

        mvc.perform(get("/articles")
                        .queryParam("page", String.valueOf(pageNumber))
                        .queryParam("size", String.valueOf(pageSize))
                        .queryParam("sort", sortName + "," + direction))
                .andExpect(status().isOk())
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attribute("paginationBarNumbers", barNumbers));

        BDDMockito.then(articleService).should().searchArticles(null, null, pageable);
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages());

    }


    //@Disabled("구현중")
    @WithMockUser
    @DisplayName("[view][get] 게시글 상세 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleView_thenReturnsArticleView() throws Exception {
        Long articleId = 1L;
        long totalCount = 1L;

        BDDMockito.given(articleService.getArticleWithComments(articleId)).willReturn(createArticleWithCommentsDto());
        BDDMockito.given(articleService.getArticleCount()).willReturn(totalCount);

        mvc.perform(get("/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(result -> content().contentTypeCompatibleWith(MediaType.TEXT_HTML_VALUE))
                .andExpect(view().name("articles/detail"))
                .andExpect(model().attributeExists("article"))
                .andExpect(model().attributeExists("articleComments"));

        BDDMockito.then(articleService).should().getArticleWithComments(articleId);
        BDDMockito.then(articleService).should().getArticleCount();

    }

    @DisplayName("[view][GET] 게시글 해시태그 검색 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleSearchHashtagView_thenReturnsArticleSearchHashtagView() throws Exception {
        List<String> hashtags = List.of("#java", "#spring", "#boot");
        BDDMockito.given(articleService.searchArticlesViaHashtag(eq(null), any(Pageable.class))).willReturn(Page.empty());
        BDDMockito.given(articleService.getHashTags()).willReturn(hashtags);
        BDDMockito.given(paginationService.getPaginationBarNumbers(anyInt(), anyInt())).willReturn(List.of(0, 1, 2, 3, 4));


        mvc.perform(get("/articles/search-hashtag"))
                .andExpect(status().isOk())
                .andExpect(result -> content().contentTypeCompatibleWith(MediaType.TEXT_HTML_VALUE))
                .andExpect(view().name("articles/search-hashtag"))
                .andExpect(model().attribute("articles", Page.empty()))
                .andExpect(model().attribute("hashtags", hashtags))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attribute("searchType" , SearchType.HASHTAG));

        BDDMockito.then(articleService).should().searchArticlesViaHashtag(eq(null), any(Pageable.class));
        BDDMockito.then(articleService).should().getHashTags();
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(anyInt(), anyInt());
    }

    @DisplayName("[view][GET] 게시글 해시태그 검색 페이지 - 정상 호출, 해시태그 입력")
    @Test
    void givenHashtag_whenRequestingArticleSearchHashtagView_thenReturnsArticleSearchHashtagView() throws Exception {
        String hashtag = "#java";
        List<String> hashtags = List.of("#java", "#spring", "#boot");

        BDDMockito.given(articleService.searchArticlesViaHashtag(eq(hashtag), any(Pageable.class))).willReturn(Page.empty());
        BDDMockito.given(articleService.getHashTags()).willReturn(hashtags);
        BDDMockito.given(paginationService.getPaginationBarNumbers(anyInt(), anyInt())).willReturn(List.of(1, 2, 3, 4,5));

        mvc.perform(get("/articles/search-hashtag")
                        .queryParam("searchValue", hashtag))
                .andExpect(status().isOk())
                .andExpect(result -> content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/search-hashtag"))
                .andExpect(model().attribute("articles", Page.empty()))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attribute("searchType", SearchType.HASHTAG))
                .andExpect(model().attribute("hashtags", hashtags));

        BDDMockito.then(articleService).should().searchArticlesViaHashtag(eq(hashtag), any(Pageable.class));
        BDDMockito.then(articleService).should().getHashTags();
        BDDMockito.then(paginationService).should().getPaginationBarNumbers(anyInt(), anyInt());

    }

    @WithMockUser
    @DisplayName("[view][GET] 새 게시글 작성 페이지")
    @Test
    void givenNothing_whenRequesting_thenReturnsNewArticlePage() throws Exception {
        mvc.perform(get("/articles/form"))
                .andExpect(status().isOk())
                .andExpect(result -> content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/form"))
                .andExpect(model().attribute("formStatus", FormStatus.CREATE));
    }

    @DisplayName("[view][GET] 게시글 페이지 - 인증 없을 땐 로그인 페이지로 이동")
    @Test
    void givenNothing_whenRequestingArticlePage_thenRedirectsToLoginPage() throws Exception {
        long articleId = 1L;
        mvc.perform(get("/articles/" + articleId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        then(articleService).shouldHaveNoInteractions();

    }

    @WithUserDetails(value = "unoTest" , userDetailsServiceBeanName = "userDetailsService" , setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("[view][POST] 새 게시글 등록 - 정상 호출")
    @Test
    void givenNewArticleInfo_whenRequesting_thenSavesNewArticle() throws Exception {
        ArticleRequest articleRequest = ArticleRequest.of("new title", "new content");

        willDoNothing().given(articleService).saveArticle(any(ArticleDto.class));

        mvc.perform(post("/articles/form")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(formDataEncoder.encode(articleRequest))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles"))
                .andExpect(redirectedUrl("/articles"));

        then(articleService).should().saveArticle(any(ArticleDto.class));

        System.out.println("articleRequest = " + formDataEncoder.encode(articleRequest));
    }

    @WithUserDetails(value = "unoTest" , setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("[view][POST] 게시글 삭제 - 정상 호출")
    @Test
    void givenArticleIdToDelete_whenRequesting_thenDeletesArticle() throws Exception {
        long articleId = 1L;
        String userId = "unoTest";


        willDoNothing().given(articleService).deleteArticle(articleId,userId);


        mvc.perform(post("/articles/" + articleId + "/delete")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles"))
                .andExpect(redirectedUrl("/articles"));

        BDDMockito.then(articleService).should().deleteArticle(articleId,userId);

    }


    @WithUserDetails(value = "unoTest", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("[view][POST] 게시글 수정 - 정상 호출")
    @Test
    void givenUpdatedArticleInfo_whenRequesting_thenUpdatesNewArticle() throws Exception {
        long articleId = 1L;
        ArticleRequest articleRequest = ArticleRequest.of("new title", "new content");

        willDoNothing().given(articleService).updateArticle(eq(articleId), any(ArticleDto.class));

        mvc.perform(post("/articles/" + articleId + "/form")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(formDataEncoder.encode(articleRequest))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles/" + articleId))
                .andExpect(redirectedUrl("/articles/" + articleId));

        BDDMockito.then(articleService).should().updateArticle(eq(articleId), any(ArticleDto.class));


    }




    private ArticleDto createArticleDto(){
        return ArticleDto.of(
                createUserAccountDto(),
                "title", "content",
                Set.of(HashtagDto.of("java"))
        );
    }

    private ArticleWithCommentsDto createArticleWithCommentsDto(){
        return ArticleWithCommentsDto.of(1L,
                createUserAccountDto(),
                Set.of(),
                "title",
                "content",
                Set.of(HashtagDto.of("java")),
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno");
    }

    private UserAccountDto createUserAccountDto(){
        return UserAccountDto.of
                ("uno", "pw",
                        "uno@mail.com", "Uno",
                        "memo", LocalDateTime.now(),
                        "uno", LocalDateTime.now(),
                        "uno");
    }
}

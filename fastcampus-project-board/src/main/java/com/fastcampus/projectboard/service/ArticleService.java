package com.fastcampus.projectboard.service;

import com.fastcampus.projectboard.domain.Article;
import com.fastcampus.projectboard.domain.Hashtag;
import com.fastcampus.projectboard.domain.UserAccount;
import com.fastcampus.projectboard.domain.type.SearchType;
import com.fastcampus.projectboard.dto.ArticleDto;
import com.fastcampus.projectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.projectboard.repository.ArticleRepository;
import com.fastcampus.projectboard.repository.HashtagRepository;
import com.fastcampus.projectboard.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ArticleService {
    private final UserAccountRepository userAccountRepository;
    private final ArticleRepository articleRepository;

    private final HashtagService hashtagService;
    private final HashtagRepository hashtagRepository;

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(SearchType searchType, String searchKeyword, Pageable pageable) {
        if (searchKeyword == null || searchKeyword.isBlank())
        {
            return articleRepository.findAll(pageable).map(ArticleDto::from);
        }
        return switch(searchType)
        {
            case TITLE -> articleRepository.findByTitleContaining(searchKeyword, pageable).map(ArticleDto::from);
            case CONTENT -> articleRepository.findByContentContaining(searchKeyword, pageable).map(ArticleDto::from);
            case ID -> articleRepository.findByUserAccount_UserIdContaining(searchKeyword, pageable).map(ArticleDto::from);
            case NICKNAME -> articleRepository.findByUserAccount_NicknameContaining(searchKeyword, pageable).map(ArticleDto::from);
            case HASHTAG -> articleRepository.findByHashtagNames(Arrays.stream(searchKeyword.split(" ")).toList(),
                            pageable)
                    .map(ArticleDto::from);
        };
    }
    @Transactional(readOnly = true)
    public ArticleWithCommentsDto getArticleWithComments(Long articleId){
        return articleRepository.findById(articleId)
                .map(ArticleWithCommentsDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다 - articleId: " + articleId));

    }

    @Transactional(readOnly = true)
    public ArticleDto getArticle(Long articleId){
        return articleRepository.findById(articleId)
                .map(ArticleDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다 - articleId: " + articleId));
    }

    public void saveArticle(ArticleDto dto) {
        UserAccount userAccount = userAccountRepository.getReferenceById(dto.userAccountDto().userId());

        Set<Hashtag> hashtags = renewHashtagsFromContent(dto.content());

        // 기존 게시글이 없으므로, 해시태그 삭제 과정이 필요없다.

        Article article = dto.toEntity(userAccount);

        article.addHashtags(hashtags);

        articleRepository.save(article);

    }

    public void updateArticle(Long articleId , ArticleDto dto) {
        try {
            Article article = articleRepository.getReferenceById(articleId);

            UserAccount userAccount = userAccountRepository.getReferenceById(dto.userAccountDto().userId());


            if(article.getUserAccount().equals(userAccount)){
                if (dto.title() != null) {
                    article.setTitle(dto.title());
                }

                if (dto.content() != null) {
                    article.setContent(dto.content());
                }

                Set<Long> hashtagIds = article.getHashtags().stream()
                        .map(Hashtag::getId)
                        .collect(Collectors.toUnmodifiableSet());

                article.clearHashtags();

                // 업데이트 에선 기존 연결된 해시태그 삭제 후, 작업

                articleRepository.save(article);

                hashtagIds.forEach(hashtagService::deleteHashtagWithoutArticles);

                Set<Hashtag> hashtags = renewHashtagsFromContent(dto.content());

                article.addHashtags(hashtags);
            }

        }
        catch (EntityNotFoundException e) {
            log.warn("게시글 업데이트 실패. 게시글을 수정 하는데 필요한 정보를 찾을 수 없습니다. - {}", e.getLocalizedMessage());
        }
    }


    public void deleteArticle(long articleId , String userId) {
        Article article = articleRepository.getReferenceById(articleId);

        Set<Long> hashtagIds = article.getHashtags().stream()
                .map(Hashtag::getId)
                .collect(Collectors.toUnmodifiableSet());



        articleRepository.deleteByIdAndUserAccount_UserId(articleId, userId);

        articleRepository.flush();

        hashtagIds.forEach(hashtagService::deleteHashtagWithoutArticles);

    }

    public long getArticleCount(){
        return articleRepository.count();
    }

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticlesViaHashtag(String hashtagName, Pageable pageable) {
        if (hashtagName == null || hashtagName.isBlank()) {
            return Page.empty(pageable);
        }
        return articleRepository.findByHashtagNames(List.of(hashtagName), pageable).map(ArticleDto::from);
    }

    public List<String> getHashTags(){
        return hashtagRepository.findAllHashtagNames(); // TODO:
    }

    private Set<Hashtag> renewHashtagsFromContent(String content) {
        Set<String> hashtagNamesInContent = hashtagService.parseHashtagNames(content);

        // 내용에 있는 해시태그 파싱
        Set<Hashtag> hashtags = hashtagService.findHashtagsByNames(hashtagNamesInContent);
        // 파싱된 해시태그들중 repository 에 있는거 가져오기

        Set<String> existingHashtagNames = hashtags.stream().map(Hashtag::getHashtagName)
                .collect(Collectors.toUnmodifiableSet());
        // 스트링으로 변환

        hashtagNamesInContent.forEach(newHashtagName ->{
            if(!existingHashtagNames.contains(newHashtagName)){
                hashtags.add(Hashtag.of(newHashtagName));
            }
        });
        // 내용에 있는 해시태그 파싱된것중 없는거 추가

        return hashtags;
    }
}

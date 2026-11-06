package com.divination.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.divination.entity.Article;
import com.divination.mapper.ArticleMapper;
import com.divination.service.ArticleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public IPage<Article> listArticles(int page, int size, String category, String keyword) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1); // only published
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Article::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getSummary, keyword));
        }
        wrapper.orderByDesc(Article::getSortOrder, Article::getCreateTime);
        return articleMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Article getArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null && article.getStatus() == 1) {
            incrementViewCount(id);
        }
        return article;
    }

    @Override
    public void incrementViewCount(Long id) {
        articleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    @Override
    public void likeArticle(Long id) {
        articleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("like_count = like_count + 1"));
    }

    @Override
    public List<String> getAllCategories() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().select(Article::getCategory)
                        .eq(Article::getStatus, 1)
                        .groupBy(Article::getCategory));
        return articles.stream().map(Article::getCategory).distinct().collect(Collectors.toList());
    }
}

package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.entity.Article;
import java.util.List;
import java.util.Map;

public interface ArticleService {
    IPage<Article> listArticles(int page, int size, String category, String keyword);
    Article getArticle(Long id);
    void incrementViewCount(Long id);
    void likeArticle(Long id);
    List<String> getAllCategories();
}

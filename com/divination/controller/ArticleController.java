package com.divination.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.common.result.Result;
import com.divination.entity.Article;
import com.divination.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/list")
    public Result<IPage<Article>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.success(articleService.listArticles(page, size, category, keyword));
    }

    @GetMapping("/{id}")
    public Result<Article> getArticle(@PathVariable Long id) {
        Article article = articleService.getArticle(id);
        if (article == null) return Result.error(404, "文章不存在");
        return Result.success(article);
    }

    @PostMapping("/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(articleService.getAllCategories());
    }
}

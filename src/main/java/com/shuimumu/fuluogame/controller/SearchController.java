package com.shuimumu.fuluogame.controller;

import com.shuimumu.fuluogame.service.PostService;
import com.shuimumu.fuluogame.vo.PageVO;
import com.shuimumu.fuluogame.vo.PostContentVO;
import com.shuimumu.fuluogame.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 全局搜索入口。
 *
 * <p>前端（{@code fronded/src/api/index.js}）统一调用 {@code GET /search}，
 * 而单体历史接口是 {@code GET /post/search}。这里补一个根路径别名，两者并存：
 * 老调用方继续用 {@code /post/search}，前端与云版（网关 {@code /search}）保持一致。
 *
 * <p>当前检索域只有帖子（Elasticsearch）；用户/话题检索在 V1.2 规划内补充。
 */
@Tag(name = "搜索接口")
@RestController
public class SearchController {

    private final PostService postService;

    public SearchController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "搜索帖子")
    @GetMapping("/search")
    public Result<PageVO<PostContentVO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        if (q == null || q.isBlank()) {
            return Result.success(PageVO.of(List.of(), 0, safePage, safeSize));
        }
        return Result.success(postService.searchPosts(q.trim(), safePage, safeSize));
    }
}

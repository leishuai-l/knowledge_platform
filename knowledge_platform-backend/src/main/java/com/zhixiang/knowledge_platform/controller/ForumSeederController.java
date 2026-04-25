package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.service.ForumSeederService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "论坛数据管理", description = "用于重置和填充测试数据")
public class ForumSeederController {

    private final ForumSeederService forumSeederService;

    @PostMapping("/reset-and-seed")
    @Operation(summary = "重置并填充论坛数据")
    public ResponseEntity<ApiResponse<String>> resetAndSeed() {
        forumSeederService.resetAndSeedForumData();
        return ResponseEntity.ok(ApiResponse.success("论坛数据已重置并填充完成"));
    }
}

package com.zhixiang.knowledge_platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "10")
    private Integer size;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "是否为第一页", example = "true")
    private Boolean isFirst;

    @Schema(description = "是否为最后一页", example = "false")
    private Boolean isLast;

    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrevious;

    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    /**
     * 从Spring Data的Page对象转换
     */
    public static <T> PageResponse<T> fromPage(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setList(page.getContent());
        response.setTotal(page.getTotalElements());
        response.setPage(page.getNumber() + 1); // Spring Data页码从0开始，前端通常从1开始
        response.setSize(page.getSize());
        response.setPages(page.getTotalPages());
        response.setIsFirst(page.isFirst());
        response.setIsLast(page.isLast());
        response.setHasPrevious(page.hasPrevious());
        response.setHasNext(page.hasNext());
        return response;
    }

    /**
     * 从Spring Data的Page对象转换并映射数据
     */
    public static <T, R> PageResponse<R> fromPage(Page<T> page, List<R> mappedList) {
        PageResponse<R> response = new PageResponse<>();
        response.setList(mappedList);
        response.setTotal(page.getTotalElements());
        response.setPage(page.getNumber() + 1);
        response.setSize(page.getSize());
        response.setPages(page.getTotalPages());
        response.setIsFirst(page.isFirst());
        response.setIsLast(page.isLast());
        response.setHasPrevious(page.hasPrevious());
        response.setHasNext(page.hasNext());
        return response;
    }

    /**
     * 创建空的分页响应
     */
    public static <T> PageResponse<T> empty() {
        PageResponse<T> response = new PageResponse<>();
        response.setList(List.of());
        response.setTotal(0L);
        response.setPage(1);
        response.setSize(10);
        response.setPages(0);
        response.setIsFirst(true);
        response.setIsLast(true);
        response.setHasPrevious(false);
        response.setHasNext(false);
        return response;
    }
}
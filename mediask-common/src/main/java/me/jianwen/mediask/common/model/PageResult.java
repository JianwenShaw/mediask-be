package me.jianwen.mediask.common.model;

import lombok.Data;

import java.util.List;

/**
 * 分页结果（通用）
 *
 * @author jianwen
 */
@Data
public class PageResult<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> list;

    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    public Long getTotalPages() {
        if (total == null || total == 0) {
            return 0L;
        }
        return (total + pageSize - 1) / pageSize;
    }

    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        return new PageResult<>(total, pageNum, pageSize, list);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 1, 10, List.of());
    }
}

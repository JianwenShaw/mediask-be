package me.jianwen.mediask.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装
 * <p>
 * 统一分页查询的返回格式
 * </p>
 *
 * @param <T> 数据类型
 * @author jianwen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从 1 开始）
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return current > 1;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return current < pages;
    }

    /**
     * 构建分页结果
     *
     * @param records 数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        long pages = total % size == 0 ? total / size : total / size + 1;
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .current(current)
                .size(size)
                .pages(pages)
                .build();
    }

    /**
     * 构建空分页结果
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        return PageResult.<T>builder()
                .records(Collections.emptyList())
                .total(0L)
                .current(current)
                .size(size)
                .pages(0L)
                .build();
    }
}

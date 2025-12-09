package me.jianwen.mediask.common.dto;

import lombok.Data;
import me.jianwen.mediask.common.constant.CommonConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页请求基类
 * <p>
 * 所有分页查询的 DTO 应继承此类
 * </p>
 *
 * @author jianwen
 */
@Data
public class PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从 1 开始）
     */
    private Long current = (long) CommonConstant.DEFAULT_PAGE_NUM;

    /**
     * 每页大小（默认 10，最大 100）
     */
    private Long size = (long) CommonConstant.DEFAULT_PAGE_SIZE;

    /**
     * 获取偏移量
     *
     * @return 偏移量
     */
    public Long getOffset() {
        return (current - 1) * size;
    }
}

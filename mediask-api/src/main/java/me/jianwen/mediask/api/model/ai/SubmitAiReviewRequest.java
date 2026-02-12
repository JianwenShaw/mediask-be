package me.jianwen.mediask.api.model.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAiReviewRequest {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @NotNull(message = "复核评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer reviewScore;

    @NotNull(message = "是否采纳不能为空")
    private Boolean adopted;

    private String reviewComment;
}

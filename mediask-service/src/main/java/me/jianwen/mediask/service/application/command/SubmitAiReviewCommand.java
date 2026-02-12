package me.jianwen.mediask.service.application.command;

import lombok.Data;

@Data
public class SubmitAiReviewCommand {

    private Long conversationId;
    private Integer reviewScore;
    private Boolean adopted;
    private String reviewComment;
}

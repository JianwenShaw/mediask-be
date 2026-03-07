package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.schedule.domain.entity.AiFeedbackReview;
import me.jianwen.mediask.schedule.domain.entity.DoctorProfileLite;
import me.jianwen.mediask.schedule.domain.repository.AiMetricsRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorProfileRepository;
import me.jianwen.mediask.service.application.command.SubmitAiReviewCommand;
import me.jianwen.mediask.service.application.dto.ai.AiReviewResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AI 复核应用服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiReviewApplicationService {

    private final AiMetricsRepository aiMetricsRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    @Transactional(rollbackFor = Exception.class)
    public AiReviewResultDTO submitReview(Long reviewerUserId, SubmitAiReviewCommand command) {
        if (command.getReviewScore() == null || command.getReviewScore() < 1 || command.getReviewScore() > 5) {
            throw new BizException(ErrorCode.PARAM_ERROR, "复核评分范围为1-5");
        }

        DoctorProfileLite doctor = doctorProfileRepository.findByUserId(reviewerUserId)
                .orElseThrow(() -> new BizException(ErrorCode.OPERATION_FORBIDDEN, "当前用户不是医生"));

        AiFeedbackReview review = new AiFeedbackReview();
        review.setConversationId(command.getConversationId());
        review.setDoctorId(doctor.getDoctorId());
        review.setDepartmentId(doctor.getDepartmentId());
        review.setReviewScore(command.getReviewScore());
        review.setIsAdopted(Boolean.TRUE.equals(command.getAdopted()) ? 1 : 0);
        review.setReviewComment(command.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        aiMetricsRepository.saveReview(review);

        log.info("医生提交AI复核: userId={}, doctorId={}, conversationId={}, score={}",
                reviewerUserId, doctor.getDoctorId(), command.getConversationId(), command.getReviewScore());
        return AiReviewResultDTO.builder().reviewId(review.getId()).build();
    }
}

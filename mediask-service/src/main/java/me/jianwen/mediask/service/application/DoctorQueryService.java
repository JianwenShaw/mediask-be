package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.service.application.command.DoctorPageQueryCommand;
import me.jianwen.mediask.service.application.dto.doctor.DoctorDTO;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;
import me.jianwen.mediask.user.domain.query.DoctorPageQuery;
import me.jianwen.mediask.user.domain.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生查询服务
 */
@Service
@RequiredArgsConstructor
public class DoctorQueryService {

    private final DoctorRepository doctorRepository;

    public DoctorDTO getDoctor(Long doctorId) {
        DoctorProfile profile = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BizException(ErrorCode.DOCTOR_NOT_FOUND));
        return toDTO(profile);
    }

    public PageResult<DoctorDTO> pageDoctors(DoctorPageQueryCommand command) {
        DoctorPageQuery query = DoctorPageQuery.builder()
                .userId(command.getUserId())
                .doctorCode(command.getDoctorCode())
                .hospitalId(command.getHospitalId())
                .departmentId(command.getDepartmentId())
                .status(command.getStatus())
                .keyword(command.getKeyword())
                .pageNum(command.getPageNum())
                .pageSize(command.getPageSize())
                .build();
        PageResult<DoctorProfile> pageResult = doctorRepository.page(query);
        List<DoctorDTO> list = pageResult.getList().stream().map(this::toDTO).toList();
        return PageResult.of(pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize(), list);
    }

    private DoctorDTO toDTO(DoctorProfile profile) {
        return DoctorDTO.builder()
                .doctorId(profile.getDoctorId())
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .realName(profile.getRealName())
                .phone(profile.getPhone())
                .hospitalId(profile.getHospitalId())
                .hospitalName(profile.getHospitalName())
                .departmentId(profile.getDepartmentId())
                .departmentName(profile.getDepartmentName())
                .doctorCode(profile.getDoctorCode())
                .title(profile.getTitle())
                .specialty(profile.getSpecialty())
                .introduction(profile.getIntroduction())
                .consultationFee(profile.getConsultationFee())
                .licenseNumber(profile.getLicenseNumber())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}

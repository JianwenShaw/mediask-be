package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.service.application.command.CreateDoctorCommand;
import me.jianwen.mediask.service.application.command.UpdateDoctorCommand;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;
import me.jianwen.mediask.user.domain.enums.UserType;
import me.jianwen.mediask.user.domain.repository.DoctorRepository;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorApplicationService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public Long createDoctor(CreateDoctorCommand command) {
        validateRequired(command.getUserId(), command.getHospitalId(), command.getDepartmentId(), command.getDoctorCode());
        validateStatus(command.getStatus());
        validateUser(command.getUserId());
        validateHospitalAndDepartment(command.getHospitalId(), command.getDepartmentId());

        if (doctorRepository.existsByUserId(command.getUserId())) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, "该用户已存在医生档案");
        }
        if (doctorRepository.existsByDoctorCode(command.getDoctorCode())) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, "医生编码已存在");
        }

        DoctorProfile profile = DoctorProfile.builder()
                .userId(command.getUserId())
                .hospitalId(command.getHospitalId())
                .departmentId(command.getDepartmentId())
                .doctorCode(command.getDoctorCode())
                .title(command.getTitle())
                .specialty(command.getSpecialty())
                .introduction(command.getIntroduction())
                .consultationFee(command.getConsultationFee())
                .licenseNumber(command.getLicenseNumber())
                .status(command.getStatus() == null ? 1 : command.getStatus())
                .build();
        Long doctorId = doctorRepository.save(profile);
        log.info("创建医生档案成功: doctorId={}, userId={}", doctorId, command.getUserId());
        return doctorId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDoctor(Long doctorId, UpdateDoctorCommand command) {
        validateStatus(command.getStatus());
        DoctorProfile existing = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BizException(ErrorCode.DOCTOR_NOT_FOUND));

        Long hospitalId = command.getHospitalId() != null ? command.getHospitalId() : existing.getHospitalId();
        Long departmentId = command.getDepartmentId() != null ? command.getDepartmentId() : existing.getDepartmentId();
        validateHospitalAndDepartment(hospitalId, departmentId);

        String doctorCode = command.getDoctorCode() != null ? command.getDoctorCode() : existing.getDoctorCode();
        if (doctorRepository.existsByDoctorCodeExcludeId(doctorCode, doctorId)) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, "医生编码已存在");
        }

        DoctorProfile updated = DoctorProfile.builder()
                .doctorId(doctorId)
                .userId(existing.getUserId())
                .hospitalId(hospitalId)
                .departmentId(departmentId)
                .doctorCode(doctorCode)
                .title(command.getTitle() != null ? command.getTitle() : existing.getTitle())
                .specialty(command.getSpecialty() != null ? command.getSpecialty() : existing.getSpecialty())
                .introduction(command.getIntroduction() != null ? command.getIntroduction() : existing.getIntroduction())
                .consultationFee(command.getConsultationFee() != null ? command.getConsultationFee() : existing.getConsultationFee())
                .licenseNumber(command.getLicenseNumber() != null ? command.getLicenseNumber() : existing.getLicenseNumber())
                .status(command.getStatus() != null ? command.getStatus() : existing.getStatus())
                .build();
        doctorRepository.update(updated);
        log.info("更新医生档案成功: doctorId={}", doctorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDoctorStatus(Long doctorId, Integer status) {
        validateStatus(status);
        if (doctorRepository.findById(doctorId).isEmpty()) {
            throw new BizException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        doctorRepository.updateStatus(doctorId, status);
        log.info("更新医生状态成功: doctorId={}, status={}", doctorId, status);
    }

    private void validateUser(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (user.getUserType() == null || user.getUserType().code() != UserType.DOCTOR.code()) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN, "该用户不是医生类型");
        }
    }

    private void validateHospitalAndDepartment(Long hospitalId, Long departmentId) {
        if (!doctorRepository.hospitalExists(hospitalId)) {
            throw new BizException(ErrorCode.HOSPITAL_NOT_FOUND);
        }
        if (!doctorRepository.departmentExists(departmentId)) {
            throw new BizException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        if (!doctorRepository.departmentBelongsToHospital(departmentId, hospitalId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "科室不属于指定医院");
        }
    }

    private void validateRequired(Long userId, Long hospitalId, Long departmentId, String doctorCode) {
        if (userId == null || hospitalId == null || departmentId == null || doctorCode == null || doctorCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "医生创建参数不完整");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态仅支持0或1");
        }
    }
}

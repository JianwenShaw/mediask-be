package me.jianwen.mediask.domain.repository;

import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;

import java.util.Optional;

public interface DoctorRepository {

    Long save(DoctorProfile doctorProfile);

    void update(DoctorProfile doctorProfile);

    void updateStatus(Long doctorId, Integer status);

    Optional<DoctorProfile> findById(Long doctorId);

    PageResult<DoctorProfile> page(DoctorPageQuery query);

    boolean existsByUserId(Long userId);

    boolean existsByDoctorCode(String doctorCode);

    boolean existsByDoctorCodeExcludeId(String doctorCode, Long doctorId);

    boolean hospitalExists(Long hospitalId);

    boolean departmentExists(Long departmentId);

    boolean departmentBelongsToHospital(Long departmentId, Long hospitalId);
}

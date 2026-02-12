package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.DoctorDO;
import me.jianwen.mediask.dal.mapper.DoctorMapper;
import me.jianwen.mediask.schedule.domain.entity.DoctorProfileLite;
import me.jianwen.mediask.schedule.domain.repository.DoctorProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DoctorProfileRepositoryImpl implements DoctorProfileRepository {

    private final DoctorMapper doctorMapper;

    @Override
    public Optional<DoctorProfileLite> findByUserId(Long userId) {
        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorDO::getUserId, userId);
        DoctorDO doctor = doctorMapper.selectOne(wrapper);
        if (doctor == null) {
            return Optional.empty();
        }
        return Optional.of(DoctorProfileLite.builder()
                .doctorId(doctor.getId())
                .userId(doctor.getUserId())
                .departmentId(doctor.getDeptId())
                .build());
    }
}

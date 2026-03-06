package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.dal.entity.DepartmentDO;
import me.jianwen.mediask.dal.entity.DoctorDO;
import me.jianwen.mediask.dal.entity.HospitalDO;
import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.enums.StatusEnum;
import me.jianwen.mediask.dal.mapper.DepartmentMapper;
import me.jianwen.mediask.dal.mapper.DoctorMapper;
import me.jianwen.mediask.dal.mapper.HospitalMapper;
import me.jianwen.mediask.dal.mapper.UserMapper;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;
import me.jianwen.mediask.user.domain.query.DoctorPageQuery;
import me.jianwen.mediask.user.domain.repository.DoctorRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DoctorRepositoryImpl implements DoctorRepository {

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final HospitalMapper hospitalMapper;

    @Override
    public Long save(DoctorProfile doctorProfile) {
        DoctorDO doctorDO = toDoctorDO(doctorProfile);
        doctorMapper.insert(doctorDO);
        return doctorDO.getId();
    }

    @Override
    public void update(DoctorProfile doctorProfile) {
        DoctorDO doctorDO = toDoctorDO(doctorProfile);
        doctorMapper.updateById(doctorDO);
    }

    @Override
    public void updateStatus(Long doctorId, Integer status) {
        DoctorDO doctorDO = new DoctorDO();
        doctorDO.setId(doctorId);
        doctorDO.setStatus(StatusEnum.fromCode(status));
        doctorMapper.updateById(doctorDO);
    }

    @Override
    public Optional<DoctorProfile> findById(Long doctorId) {
        DoctorDO doctorDO = doctorMapper.selectById(doctorId);
        if (doctorDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDoctorProfile(doctorDO, loadUserMap(Set.of(doctorDO.getUserId())),
                loadDepartmentMap(Set.of(doctorDO.getDeptId())), loadHospitalMap(Set.of(doctorDO.getHospitalId()))));
    }

    @Override
    public List<DoctorProfile> listActiveByDepartment(Long departmentId, List<Long> doctorIds) {
        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorDO::getDeptId, departmentId)
                .eq(DoctorDO::getStatus, StatusEnum.ENABLED)
                .in(doctorIds != null && !doctorIds.isEmpty(), DoctorDO::getId, doctorIds)
                .orderByAsc(DoctorDO::getId);

        List<DoctorDO> doctors = doctorMapper.selectList(wrapper);
        if (doctors == null || doctors.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = doctors.stream().map(DoctorDO::getUserId).collect(Collectors.toSet());
        Set<Long> deptIds = doctors.stream().map(DoctorDO::getDeptId).collect(Collectors.toSet());
        Set<Long> hospitalIds = doctors.stream().map(DoctorDO::getHospitalId).collect(Collectors.toSet());
        Map<Long, UserDO> userMap = loadUserMap(userIds);
        Map<Long, DepartmentDO> departmentMap = loadDepartmentMap(deptIds);
        Map<Long, HospitalDO> hospitalMap = loadHospitalMap(hospitalIds);
        return doctors.stream()
                .map(record -> toDoctorProfile(record, userMap, departmentMap, hospitalMap))
                .toList();
    }

    @Override
    public PageResult<DoctorProfile> page(DoctorPageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        StatusEnum statusEnum = query.getStatus() != null ? StatusEnum.fromCode(query.getStatus()) : null;

        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, DoctorDO::getUserId, query.getUserId())
                .eq(StringUtils.hasText(query.getDoctorCode()), DoctorDO::getDoctorCode, query.getDoctorCode())
                .eq(query.getHospitalId() != null, DoctorDO::getHospitalId, query.getHospitalId())
                .eq(query.getDepartmentId() != null, DoctorDO::getDeptId, query.getDepartmentId())
                .eq(statusEnum != null, DoctorDO::getStatus, statusEnum)
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(DoctorDO::getDoctorCode, query.getKeyword())
                        .or().like(DoctorDO::getTitle, query.getKeyword())
                        .or().like(DoctorDO::getLicenseNumber, query.getKeyword()))
                .orderByDesc(DoctorDO::getUpdatedAt);

        Page<DoctorDO> page = doctorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DoctorDO> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(page.getTotal(), pageNum, pageSize, List.of());
        }

        Set<Long> userIds = records.stream().map(DoctorDO::getUserId).collect(Collectors.toSet());
        Set<Long> deptIds = records.stream().map(DoctorDO::getDeptId).collect(Collectors.toSet());
        Set<Long> hospitalIds = records.stream().map(DoctorDO::getHospitalId).collect(Collectors.toSet());

        Map<Long, UserDO> userMap = loadUserMap(userIds);
        Map<Long, DepartmentDO> departmentMap = loadDepartmentMap(deptIds);
        Map<Long, HospitalDO> hospitalMap = loadHospitalMap(hospitalIds);

        List<DoctorProfile> list = records.stream()
                .map(record -> toDoctorProfile(record, userMap, departmentMap, hospitalMap))
                .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorDO::getUserId, userId);
        return doctorMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByDoctorCode(String doctorCode) {
        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorDO::getDoctorCode, doctorCode);
        return doctorMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByDoctorCodeExcludeId(String doctorCode, Long doctorId) {
        LambdaQueryWrapper<DoctorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorDO::getDoctorCode, doctorCode)
                .ne(DoctorDO::getId, doctorId);
        return doctorMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean hospitalExists(Long hospitalId) {
        return hospitalMapper.selectById(hospitalId) != null;
    }

    @Override
    public boolean departmentExists(Long departmentId) {
        return departmentMapper.selectById(departmentId) != null;
    }

    @Override
    public boolean departmentBelongsToHospital(Long departmentId, Long hospitalId) {
        DepartmentDO department = departmentMapper.selectById(departmentId);
        return department != null && hospitalId.equals(department.getHospitalId());
    }

    private DoctorDO toDoctorDO(DoctorProfile doctorProfile) {
        DoctorDO doctorDO = new DoctorDO();
        doctorDO.setId(doctorProfile.getDoctorId());
        doctorDO.setUserId(doctorProfile.getUserId());
        doctorDO.setHospitalId(doctorProfile.getHospitalId());
        doctorDO.setDeptId(doctorProfile.getDepartmentId());
        doctorDO.setDoctorCode(doctorProfile.getDoctorCode());
        doctorDO.setTitle(doctorProfile.getTitle());
        doctorDO.setSpecialty(doctorProfile.getSpecialty());
        doctorDO.setIntroduction(doctorProfile.getIntroduction());
        doctorDO.setConsultationFee(doctorProfile.getConsultationFee());
        doctorDO.setLicenseNumber(doctorProfile.getLicenseNumber());
        doctorDO.setStatus(StatusEnum.fromCode(doctorProfile.getStatus()));
        return doctorDO;
    }

    private DoctorProfile toDoctorProfile(DoctorDO doctorDO, Map<Long, UserDO> userMap,
                                          Map<Long, DepartmentDO> departmentMap, Map<Long, HospitalDO> hospitalMap) {
        UserDO userDO = userMap.get(doctorDO.getUserId());
        DepartmentDO departmentDO = departmentMap.get(doctorDO.getDeptId());
        HospitalDO hospitalDO = hospitalMap.get(doctorDO.getHospitalId());
        return DoctorProfile.builder()
                .doctorId(doctorDO.getId())
                .userId(doctorDO.getUserId())
                .username(userDO != null ? userDO.getUsername() : null)
                .realName(userDO != null ? userDO.getRealName() : null)
                .phone(userDO != null ? userDO.getPhone() : null)
                .hospitalId(doctorDO.getHospitalId())
                .hospitalName(hospitalDO != null ? hospitalDO.getHospitalName() : null)
                .departmentId(doctorDO.getDeptId())
                .departmentName(departmentDO != null ? departmentDO.getDeptName() : null)
                .doctorCode(doctorDO.getDoctorCode())
                .title(doctorDO.getTitle())
                .specialty(doctorDO.getSpecialty())
                .introduction(doctorDO.getIntroduction())
                .consultationFee(doctorDO.getConsultationFee())
                .licenseNumber(doctorDO.getLicenseNumber())
                .status(doctorDO.getStatus() != null ? doctorDO.getStatus().getCode() : null)
                .createdAt(doctorDO.getCreatedAt())
                .updatedAt(doctorDO.getUpdatedAt())
                .build();
    }

    private Map<Long, UserDO> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserDO> users = userMapper.selectBatchIds(userIds);
        Map<Long, UserDO> userMap = new HashMap<>();
        for (UserDO user : users) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private Map<Long, DepartmentDO> loadDepartmentMap(Set<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Map.of();
        }
        List<DepartmentDO> departments = departmentMapper.selectBatchIds(departmentIds);
        Map<Long, DepartmentDO> departmentMap = new HashMap<>();
        for (DepartmentDO department : departments) {
            departmentMap.put(department.getId(), department);
        }
        return departmentMap;
    }

    private Map<Long, HospitalDO> loadHospitalMap(Set<Long> hospitalIds) {
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            return Map.of();
        }
        List<HospitalDO> hospitals = hospitalMapper.selectBatchIds(hospitalIds);
        Map<Long, HospitalDO> hospitalMap = new HashMap<>();
        for (HospitalDO hospital : hospitals) {
            hospitalMap.put(hospital.getId(), hospital);
        }
        return hospitalMap;
    }
}

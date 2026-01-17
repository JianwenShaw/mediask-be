package me.jianwen.mediask.schedule.domain.algorithm.problem;

import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 医生集合
 *
 * <p>提供高效的医生查询和分组功能：
 * <ul>
 *   <li>按科室分组</li>
 *   <li>按专家/普通分类</li>
 *   <li>快速查找</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class DoctorSet {

    /**
     * 医生列表
     */
    private List<Doctor> doctors;

    /**
     * 创建空的医生集合
     */
    public static DoctorSet empty() {
        return new DoctorSet(Collections.emptyList());
    }

    /**
     * 从医生列表创建集合
     */
    public static DoctorSet of(List<Doctor> doctors) {
        return new DoctorSet(new ArrayList<>(doctors));
    }

    /**
     * 获取所有专家医生
     */
    public List<Doctor> getExperts() {
        return doctors.stream()
                .filter(Doctor::isExpert)
                .toList();
    }

    /**
     * 获取所有普通医生
     */
    public List<Doctor> getRegularDoctors() {
        return doctors.stream()
                .filter(d -> !d.isExpert())
                .toList();
    }

    /**
     * 按科室获取医生
     */
    public Map<Long, List<Doctor>> getDoctorsByDept() {
        return doctors.stream()
                .collect(Collectors.groupingBy(Doctor::getDeptId));
    }

    /**
     * 获取指定科室的医生
     */
    public List<Doctor> getDoctorsByDeptId(Long deptId) {
        return doctors.stream()
                .filter(d -> Objects.equals(d.getDeptId(), deptId))
                .toList();
    }

    /**
     * 获取指定ID的医生
     */
    public Optional<Doctor> getById(Long doctorId) {
        return doctors.stream()
                .filter(d -> d.getId().equals(doctorId))
                .findFirst();
    }

    /**
     * 获取医生数量
     */
    public int size() {
        return doctors.size();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return doctors.isEmpty();
    }
}

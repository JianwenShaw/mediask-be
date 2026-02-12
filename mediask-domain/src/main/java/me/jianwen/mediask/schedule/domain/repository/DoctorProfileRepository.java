package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.entity.DoctorProfileLite;

import java.util.Optional;

public interface DoctorProfileRepository {

    Optional<DoctorProfileLite> findByUserId(Long userId);
}

package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.CalendarDayDO;
import me.jianwen.mediask.dal.mapper.CalendarDayMapper;
import me.jianwen.mediask.schedule.domain.optimization.model.CalendarDayRule;
import me.jianwen.mediask.schedule.domain.repository.CalendarDayRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CalendarDayRepositoryImpl implements CalendarDayRepository {

    private static final String DEFAULT_REGION_CODE = "CN-NATIONAL";

    private final CalendarDayMapper mapper;

    @Override
    public List<CalendarDayRule> listByDateRange(LocalDate startDate, LocalDate endDate, String regionCode) {
        String effectiveRegion = (regionCode == null || regionCode.isBlank()) ? DEFAULT_REGION_CODE : regionCode;
        LambdaQueryWrapper<CalendarDayDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CalendarDayDO::getCalendarDate, startDate)
                .le(CalendarDayDO::getCalendarDate, endDate)
                .eq(CalendarDayDO::getStatus, 1)
                .and(q -> q.eq(CalendarDayDO::getRegionCode, effectiveRegion)
                        .or()
                        .eq(CalendarDayDO::getRegionCode, DEFAULT_REGION_CODE))
                .orderByAsc(CalendarDayDO::getCalendarDate, CalendarDayDO::getRegionCode);

        return mapper.selectList(wrapper).stream()
                .map(data -> new CalendarDayRule(
                        data.getCalendarDate(),
                        data.getIsHoliday() != null && data.getIsHoliday() == 1,
                        data.getIsMakeupWorkday() != null && data.getIsMakeupWorkday() == 1,
                        data.getHolidayName()
                ))
                .toList();
    }
}

package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Value;

/**
 * 号源容量值对象
 * 封装总号源数和剩余号源数的业务逻辑
 *
 * @author jianwen
 */
@Value
public class SlotCapacity {

    /**
     * 总号源数
     */
    int totalSlots;

    /**
     * 剩余号源数
     */
    int availableSlots;

    public SlotCapacity(int totalSlots, int availableSlots) {
        if (totalSlots < 0) {
            throw new IllegalArgumentException("Total slots cannot be negative");
        }
        if (availableSlots < 0) {
            throw new IllegalArgumentException("Available slots cannot be negative");
        }
        if (availableSlots > totalSlots) {
            throw new IllegalArgumentException("Available slots cannot exceed total slots");
        }
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
    }

    /**
     * 创建初始容量
     */
    public static SlotCapacity initial(int totalSlots) {
        return new SlotCapacity(totalSlots, totalSlots);
    }

    /**
     * 扣减号源
     */
    public SlotCapacity decrease() {
        if (availableSlots <= 0) {
            throw new IllegalStateException("No available slots to decrease");
        }
        return new SlotCapacity(totalSlots, availableSlots - 1);
    }

    /**
     * 增加号源（取消预约时）
     */
    public SlotCapacity increase() {
        if (availableSlots >= totalSlots) {
            throw new IllegalStateException("Available slots cannot exceed total slots");
        }
        return new SlotCapacity(totalSlots, availableSlots + 1);
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return availableSlots == 0;
    }

    /**
     * 是否有剩余
     */
    public boolean hasAvailable() {
        return availableSlots > 0;
    }

    /**
     * 获取已用号源数
     */
    public int getUsedSlots() {
        return totalSlots - availableSlots;
    }

    /**
     * 获取使用率（百分比）
     */
    public double getUsageRate() {
        if (totalSlots == 0) {
            return 0.0;
        }
        return (double) getUsedSlots() / totalSlots * 100;
    }
}

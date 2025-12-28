package me.jianwen.mediask.schedule.domain.valueobject;

/**
 * 号源容量
 */
public class SlotCapacity {

    private final int totalSlots;
    private final int availableSlots;

    public SlotCapacity(int totalSlots, int availableSlots) {
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }
}


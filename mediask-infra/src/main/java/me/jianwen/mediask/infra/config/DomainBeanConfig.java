package me.jianwen.mediask.infra.config;

import me.jianwen.mediask.schedule.domain.heuristic.solver.SolverFactory;
import me.jianwen.mediask.schedule.domain.heuristic.solver.impl.GeneticAlgorithmSolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.impl.GreedyLocalSearchSolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.impl.GreedySolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.impl.HybridSolver;
import me.jianwen.mediask.schedule.domain.repository.AppointmentSlotRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleDomainService;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleStrategy;
import me.jianwen.mediask.schedule.domain.service.AppointmentStateMachineDomainService;
import me.jianwen.mediask.schedule.domain.optimization.service.DepartmentScheduleOptimizationDomainService;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.service.impl.CustomDateScheduleStrategy;
import me.jianwen.mediask.schedule.domain.service.impl.PeriodicScheduleStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 领域对象装配配置
 */
@Configuration
public class DomainBeanConfig {

    @Bean
    public PeriodicScheduleStrategy periodicScheduleStrategy() {
        return new PeriodicScheduleStrategy();
    }

    @Bean
    public CustomDateScheduleStrategy customDateScheduleStrategy() {
        return new CustomDateScheduleStrategy();
    }

    @Bean
    public AutoScheduleDomainService autoScheduleDomainService(
            DoctorScheduleRepository doctorScheduleRepository,
            PeriodicScheduleStrategy periodicScheduleStrategy,
            CustomDateScheduleStrategy customDateScheduleStrategy) {
        List<AutoScheduleStrategy> strategies = List.of(periodicScheduleStrategy, customDateScheduleStrategy);
        return new AutoScheduleDomainService(strategies, doctorScheduleRepository);
    }

    @Bean
    public SlotManagementDomainService slotManagementDomainService(AppointmentSlotRepository appointmentSlotRepository) {
        return new SlotManagementDomainService(appointmentSlotRepository);
    }

    @Bean
    public AppointmentStateMachineDomainService appointmentStateMachineDomainService() {
        return new AppointmentStateMachineDomainService();
    }

    @Bean
    public DepartmentScheduleOptimizationDomainService departmentScheduleOptimizationDomainService() {
        return new DepartmentScheduleOptimizationDomainService();
    }

    @Bean
    public GreedySolver greedySolver() {
        return new GreedySolver();
    }

    @Bean
    public GreedyLocalSearchSolver greedyLocalSearchSolver() {
        return new GreedyLocalSearchSolver();
    }

    @Bean
    public GeneticAlgorithmSolver geneticAlgorithmSolver() {
        return new GeneticAlgorithmSolver();
    }

    @Bean
    public HybridSolver hybridSolver(
            GreedySolver greedySolver,
            GreedyLocalSearchSolver greedyLocalSearchSolver,
            GeneticAlgorithmSolver geneticAlgorithmSolver) {
        return new HybridSolver(greedySolver, greedyLocalSearchSolver, geneticAlgorithmSolver);
    }

    @Bean
    public SolverFactory solverFactory(
            GreedySolver greedySolver,
            GreedyLocalSearchSolver greedyLocalSearchSolver,
            GeneticAlgorithmSolver geneticAlgorithmSolver,
            HybridSolver hybridSolver) {
        return new SolverFactory(greedySolver, greedyLocalSearchSolver, geneticAlgorithmSolver, hybridSolver);
    }
}

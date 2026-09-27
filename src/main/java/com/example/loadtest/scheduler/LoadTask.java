package com.example.loadtest.scheduler;

import com.example.loadtest.service.CpuLoadService;
import com.example.loadtest.service.FailureSimulatorService;
import com.example.loadtest.service.MemoryLoadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Point d'entrée planifié : toutes les 60 secondes, déclenche un calcul CPU
 * et une allocation mémoire, puis journalise l'état de la JVM. C'est ce
 * fichier de log (format Logback standard) que Sentrix Agent doit collecter.
 */
@Component
public class LoadTask {

    private static final Logger log = LoggerFactory.getLogger(LoadTask.class);

    private final CpuLoadService cpuLoadService;
    private final MemoryLoadService memoryLoadService;
    private final FailureSimulatorService failureSimulatorService;
    private final AtomicLong cycleCounter = new AtomicLong(0);

    public LoadTask(CpuLoadService cpuLoadService, MemoryLoadService memoryLoadService,
                     FailureSimulatorService failureSimulatorService) {
        this.cpuLoadService = cpuLoadService;
        this.memoryLoadService = memoryLoadService;
        this.failureSimulatorService = failureSimulatorService;
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 5_000)
    public void runCycle() {
        long cycle = cycleCounter.incrementAndGet();
        log.info("=== Debut du cycle #{} ===", cycle);

        long durationMs = cpuLoadService.runHeavyComputation();
        memoryLoadService.runCycle(cycle);
        failureSimulatorService.maybeSimulateFailure(cycle);

        logJvmMemory();

        log.info("=== Fin du cycle #{} (calcul CPU: {} ms) ===", cycle, durationMs);
    }

    private void logJvmMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMb = runtime.totalMemory() / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);

        log.info("Etat JVM -> heap utilisee: {} Mo / allouee: {} Mo / max: {} Mo | buffer applicatif: ~{} Mo",
                usedMb, totalMb, maxMb, memoryLoadService.currentBufferSizeMb());
    }
}

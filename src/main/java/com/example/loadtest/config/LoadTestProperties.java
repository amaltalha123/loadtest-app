package com.example.loadtest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres exposés dans application.yml sous la clé "loadtest".
 * Permet d'ajuster l'intensité de la charge sans recompiler.
 */
@ConfigurationProperties(prefix = "loadtest")
public class LoadTestProperties {

    /**
     * Taille (en Mo) d'un "chunk" mémoire alloué à chaque cycle.
     */
    private int memoryChunkMb = 15;

    /**
     * Nombre maximum de chunks conservés en mémoire avant qu'une purge
     * ne soit déclenchée (permet de visualiser une montée en RAM).
     */
    private int memoryMaxChunks = 12;

    /**
     * Tous les N cycles, une partie de la mémoire accumulée est libérée
     * (permet de visualiser une baisse en "dents de scie" sur le graphe RAM).
     */
    private int releaseEveryCycles = 6;

    /**
     * Taille de la matrice utilisée pour la charge CPU (N x N).
     * Plus la valeur est grande, plus le pic CPU par cycle est marqué.
     */
    private int cpuMatrixSize = 400;

    /**
     * Simule un événement d'erreur (exception + stacktrace) de façon
     * périodique, pour tester le niveau ERROR et le parsing multi-ligne
     * des stack traces côté agent. Ce n'est PAS un vrai dysfonctionnement.
     */
    private boolean simulateErrors = true;

    /**
     * Fréquence (en nombre de cycles, donc en minutes) de l'erreur simulée.
     */
    private int errorEveryCycles = 15;

    public int getMemoryChunkMb() {
        return memoryChunkMb;
    }

    public void setMemoryChunkMb(int memoryChunkMb) {
        this.memoryChunkMb = memoryChunkMb;
    }

    public int getMemoryMaxChunks() {
        return memoryMaxChunks;
    }

    public void setMemoryMaxChunks(int memoryMaxChunks) {
        this.memoryMaxChunks = memoryMaxChunks;
    }

    public int getReleaseEveryCycles() {
        return releaseEveryCycles;
    }

    public void setReleaseEveryCycles(int releaseEveryCycles) {
        this.releaseEveryCycles = releaseEveryCycles;
    }

    public int getCpuMatrixSize() {
        return cpuMatrixSize;
    }

    public void setCpuMatrixSize(int cpuMatrixSize) {
        this.cpuMatrixSize = cpuMatrixSize;
    }

    public boolean isSimulateErrors() {
        return simulateErrors;
    }

    public void setSimulateErrors(boolean simulateErrors) {
        this.simulateErrors = simulateErrors;
    }

    public int getErrorEveryCycles() {
        return errorEveryCycles;
    }

    public void setErrorEveryCycles(int errorEveryCycles) {
        this.errorEveryCycles = errorEveryCycles;
    }
}

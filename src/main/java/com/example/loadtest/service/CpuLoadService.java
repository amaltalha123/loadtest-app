package com.example.loadtest.service;

import com.example.loadtest.config.LoadTestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simule une charge CPU réelle via une multiplication de matrices N x N.
 * Ce n'est pas une simple boucle vide : le calcul est fait pour de vrai afin
 * que le pic CPU soit bien visible dans les métriques système.
 */
@Service
public class CpuLoadService {

    private static final Logger log = LoggerFactory.getLogger(CpuLoadService.class);

    private final LoadTestProperties properties;

    public CpuLoadService(LoadTestProperties properties) {
        this.properties = properties;
    }

    /**
     * Exécute le calcul et retourne la durée en millisecondes.
     */
    public long runHeavyComputation() {
        int n = properties.getCpuMatrixSize();
        double[][] a = randomMatrix(n);
        double[][] b = randomMatrix(n);

        long start = System.nanoTime();
        double[][] result = multiply(a, b);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        // Empêche le JIT de considérer le résultat comme "mort code" et de le supprimer.
        double checksum = result[0][0] + result[n - 1][n - 1];
        log.info("Calcul termine : multiplication de matrices {}x{} en {} ms (checksum={})",
                n, n, durationMs, String.format("%.4f", checksum));

        return durationMs;
    }

    private double[][] randomMatrix(int n) {
        double[][] m = new double[n][n];
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = rnd.nextDouble();
            }
        }
        return m;
    }

    private double[][] multiply(double[][] a, double[][] b) {
        int n = a.length;
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                double aik = a[i][k];
                for (int j = 0; j < n; j++) {
                    result[i][j] += aik * b[k][j];
                }
            }
        }
        return result;
    }
}

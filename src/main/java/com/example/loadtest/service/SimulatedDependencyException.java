package com.example.loadtest.service;

/**
 * Exception délibérément déclenchée par {@link FailureSimulatorService} pour
 * tester la collecte des logs ERROR (et le parsing multi-ligne des stack
 * traces) côté Sentrix Agent. Ne représente jamais une vraie panne.
 */
public class SimulatedDependencyException extends RuntimeException {
    public SimulatedDependencyException(String message) {
        super(message);
    }
}

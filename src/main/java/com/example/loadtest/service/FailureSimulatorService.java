package com.example.loadtest.service;

import com.example.loadtest.config.LoadTestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Déclenche, à intervalle régulier, une exception simulée (log ERROR +
 * stacktrace complète), uniquement pour vérifier que le dashboard et le
 * parsing multi-ligne de Sentrix Agent gèrent correctement ce niveau de log.
 *
 * Aucun impact fonctionnel : l'exception est immédiatement catchée ici.
 */
@Service
public class FailureSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(FailureSimulatorService.class);

    private final LoadTestProperties properties;

    public FailureSimulatorService(LoadTestProperties properties) {
        this.properties = properties;
    }

    public void maybeSimulateFailure(long cycle) {
        if (!properties.isSimulateErrors()) {
            return;
        }
        if (properties.getErrorEveryCycles() <= 0) {
            return;
        }
        if (cycle % properties.getErrorEveryCycles() != 0) {
            return;
        }

        try {
            throw new SimulatedDependencyException(
                    "Timeout simule en appelant le service externe 'inventory-api' "
                            + "(delai depasse : 5000 ms) -- ceci est une erreur FICTIVE, "
                            + "generee volontairement pour tester la collecte ERROR");
        } catch (SimulatedDependencyException ex) {
            log.error("Echec simule lors d'un appel a un service externe (cycle #{})", cycle, ex);
        }
    }
}

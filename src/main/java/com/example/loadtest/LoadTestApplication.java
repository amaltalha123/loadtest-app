package com.example.loadtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Petite application "cobaye" utilisée uniquement pour générer, chaque minute,
 * de l'activité CPU et une consommation mémoire visible, afin de tester et
 * démontrer la collecte de métriques système de Sentrix Agent, en l'absence
 * d'une vraie application à superviser sur le serveur.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class LoadTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadTestApplication.class, args);
    }
}

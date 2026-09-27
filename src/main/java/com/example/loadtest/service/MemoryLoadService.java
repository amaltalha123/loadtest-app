package com.example.loadtest.service;

import com.example.loadtest.config.LoadTestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Simule une consommation mémoire progressive : à chaque appel, un nouveau
 * "chunk" de données est alloué et rempli (pour éviter que la JVM ne
 * l'optimise ou ne le partage), afin que la mémoire réellement utilisée
 * (Resident Set Size / heap) augmente visiblement.
 *
 * Toutes les N cycles, une partie du buffer est libérée pour obtenir une
 * courbe "en dents de scie", plus parlante qu'une simple droite montante.
 */
@Service
public class MemoryLoadService {

    private static final Logger log = LoggerFactory.getLogger(MemoryLoadService.class);

    private final LoadTestProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final List<byte[]> buffer = new ArrayList<>();

    public MemoryLoadService(LoadTestProperties properties) {
        this.properties = properties;
    }

    /**
     * Alloue un nouveau chunk mémoire et, si nécessaire, en libère une partie.
     *
     * @param cycle numéro du cycle courant (utilisé pour déclencher la purge périodique)
     */
    public void runCycle(long cycle) {
        allocateChunk();

        boolean overCapacity = buffer.size() > properties.getMemoryMaxChunks();
        boolean scheduledRelease = properties.getReleaseEveryCycles() > 0
                && cycle % properties.getReleaseEveryCycles() == 0;

        if (overCapacity) {
            log.warn("Seuil memoire depasse : {} bloc(s) en buffer (max configure: {}) -> purge",
                    buffer.size(), properties.getMemoryMaxChunks());
        }

        if (overCapacity || scheduledRelease) {
            releaseHalf();
        }
    }

    private void allocateChunk() {
        int bytes = properties.getMemoryChunkMb() * 1024 * 1024;
        byte[] chunk = new byte[bytes];
        // On remplit le chunk avec des données aléatoires : ça force la JVM à
        // réellement "toucher" (committer) chaque page mémoire, contrairement
        // à un simple `new byte[n]` qui pourrait rester en pages zéro paresseuses.
        random.nextBytes(chunk);
        buffer.add(chunk);

        log.info("Allocation d'un nouveau bloc memoire de {} Mo (total buffer: {} bloc(s), ~{} Mo)",
                properties.getMemoryChunkMb(), buffer.size(),
                buffer.size() * properties.getMemoryChunkMb());
    }

    private void releaseHalf() {
        int toRemove = Math.max(1, buffer.size() / 2);
        for (int i = 0; i < toRemove && !buffer.isEmpty(); i++) {
            buffer.remove(buffer.size() - 1);
        }
        log.info("Purge memoire : {} bloc(s) libere(s), {} bloc(s) restant(s) en buffer",
                toRemove, buffer.size());
    }

    public int currentBufferSizeMb() {
        return buffer.size() * properties.getMemoryChunkMb();
    }
}

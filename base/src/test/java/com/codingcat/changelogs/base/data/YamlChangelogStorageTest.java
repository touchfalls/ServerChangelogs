package com.codingcat.changelogs.base.data;

import com.codingcat.changelogs.base.data.storage.YamlChangelogStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlChangelogStorageTest {
    @Test
    void firstSeenSurvivesRestartWithoutOtherWrites(@TempDir Path dir) {
        Path file = dir.resolve("_data.yml");
        UUID player = UUID.randomUUID();
        Instant firstSeen = Instant.parse("2026-01-01T00:00:00Z");

        YamlChangelogStorage before = new YamlChangelogStorage(file);
        before.init();
        // Nothing else touches the storage afterwards, so this write alone has to reach the disk
        before.recordFirstSeen(player, firstSeen);

        YamlChangelogStorage after = new YamlChangelogStorage(file);
        after.init();
        assertEquals(firstSeen, after.getFirstSeenAt(player));
        // A later join keeps the original timestamp
        assertEquals(firstSeen, after.recordFirstSeen(player, firstSeen.plusSeconds(60)));
    }

    @Test
    void concurrentWritesAreNotLost(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("_data.yml");
        YamlChangelogStorage storage = new YamlChangelogStorage(file);
        storage.init();
        storage.storeEntry(new ChangelogEntry(0, List.of(text("x")), Instant.EPOCH, null, new HashSet<>()));
        List<UUID> players = IntStream.range(0, 200).mapToObj(_ -> UUID.randomUUID()).toList();

        // Configuration-phase joins run off the server thread, so first-seen and read marks can race
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            List<Future<?>> futures = players.stream().<Future<?>>map(p -> pool.submit(() -> {
                storage.recordFirstSeen(p, Instant.EPOCH);
                storage.markAsRead(0, p);
                storage.listEntries().forEach(e -> e.playersRead().contains(p));
            })).toList();
            for (Future<?> f : futures) f.get();
        }

        YamlChangelogStorage reloaded = new YamlChangelogStorage(file);
        reloaded.init();
        for (UUID p : players) {
            assertEquals(Instant.EPOCH, reloaded.getFirstSeenAt(p));
            assertTrue(reloaded.getByUID(0).playersRead().contains(p));
        }
    }
}

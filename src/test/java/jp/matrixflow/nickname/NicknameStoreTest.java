package jp.matrixflow.nickname;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NicknameStoreTest {

    @TempDir
    Path dir;

    @Test
    void survivesReload() throws Exception {
        File file = dir.resolve("sub/nicknames.yml").toFile();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        NicknameStore store = new NicknameStore(file);
        store.set(a, "ゆう");
        store.set(b, "たろう");
        store.set(b, null);

        NicknameStore again = new NicknameStore(file);
        assertEquals(Optional.of("ゆう"), again.get(a));
        assertTrue(again.get(b).isEmpty());
        assertEquals(List.of(), again.othersThan(a));
        assertEquals(List.of("ゆう"), List.copyOf(again.othersThan(b)));
    }
}

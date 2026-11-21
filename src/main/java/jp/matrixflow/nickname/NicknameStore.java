package jp.matrixflow.nickname;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/** Nicknames by player UUID, kept in a YAML file so they survive restarts. Main thread only. */
final class NicknameStore {

    private final File file;
    private final Map<UUID, String> names = new HashMap<>();

    NicknameStore(File file) {
        this.file = file;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            String name = yaml.getString(key);
            try {
                if (name != null && !name.isBlank()) {
                    names.put(UUID.fromString(key), name);
                }
            } catch (IllegalArgumentException ignored) {
                // Not a UUID: skip the line rather than fail to start.
            }
        }
    }

    Optional<String> get(UUID player) {
        return Optional.ofNullable(names.get(player));
    }

    /** Sets or, with null, removes a nickname, and writes the file. */
    void set(UUID player, String name) throws IOException {
        if (name == null) {
            names.remove(player);
        } else {
            names.put(player, name);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        names.forEach((id, n) -> yaml.set(id.toString(), n));
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        yaml.save(file);
    }

    /** Every nickname except this player's own. */
    Collection<String> othersThan(UUID player) {
        return names.entrySet().stream()
                .filter(e -> !e.getKey().equals(player))
                .map(Map.Entry::getValue)
                .toList();
    }

    List<UUID> players() {
        return List.copyOf(names.keySet());
    }
}

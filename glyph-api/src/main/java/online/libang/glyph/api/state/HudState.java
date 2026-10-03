package online.libang.glyph.api.state;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe player state with atomic, immutable snapshots and per-value revisions. */
public final class HudState {
    public record Snapshot(long revision, Map<String, HudValue> values, Map<String, Long> versions) {
        public Snapshot {
            values = Map.copyOf(values);
            versions = Map.copyOf(versions);
        }
        public long version(String key) { return versions.getOrDefault(key, 0L); }
        public HudValue get(String key) { return values.get(key); }
    }
    private final AtomicReference<Snapshot> current = new AtomicReference<>(new Snapshot(0, Map.of(), Map.of()));
    public Snapshot snapshot() { return current.get(); }
    public boolean set(String key, HudValue value) {
        return setAll(Map.of(Objects.requireNonNull(key), Objects.requireNonNull(value)));
    }
    /** Publishes related values together, so one frame cannot observe half an update. */
    public boolean setAll(Map<String, ? extends HudValue> changes) {
        var copied = Map.copyOf(changes);
        while (true) {
            var before = current.get();
            var values = new HashMap<>(before.values());
            var versions = new HashMap<>(before.versions());
            boolean changed = false;
            long revision = before.revision() + 1;
            for (var entry : copied.entrySet()) {
                if (!Objects.equals(values.get(entry.getKey()), entry.getValue())) {
                    values.put(entry.getKey(), entry.getValue());
                    versions.put(entry.getKey(), revision);
                    changed = true;
                }
            }
            if (!changed) return false;
            if (current.compareAndSet(before, new Snapshot(revision, values, versions))) return true;
        }
    }
    public boolean remove(String key) {
        Objects.requireNonNull(key);
        while (true) {
            var before = current.get();
            if (!before.values().containsKey(key)) return false;
            var values = new HashMap<>(before.values());
            var versions = new HashMap<>(before.versions());
            values.remove(key);
            // Keep a tombstone version to invalidate consumers after removal.
            versions.put(key, before.revision() + 1);
            if (current.compareAndSet(before, new Snapshot(before.revision() + 1, values, versions))) return true;
        }
    }
}

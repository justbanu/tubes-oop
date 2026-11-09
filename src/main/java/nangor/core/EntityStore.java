package nangor.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Iteration snapshots allow removal during a chain reaction without shared mutation. */
final class EntityStore<T extends Entity> {
    private final Map<Long, T> entries = new LinkedHashMap<>();
    void add(T entity) {
        if (entries.putIfAbsent(entity.id, entity) != null) throw new IllegalArgumentException("ID entitas duplikat");
    }
    List<T> all() { return List.copyOf(entries.values()); }
    void removeIf(Predicate<T> predicate) { entries.values().removeIf(predicate); }
    int size() { return entries.size(); }
    void clear() { entries.clear(); }
}

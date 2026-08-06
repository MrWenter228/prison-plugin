package com.prison.core.managers;

import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Тримає в пам'яті вибір pos1/pos2 (wand-ом) для кожного адміна під час
 * створення чи редагування шахти. Дані не персистяться - це лише сесія редагування.
 */
public class SelectionManager {

    public static class Selection {
        public Location pos1;
        public Location pos2;
    }

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public Selection getOrCreate(UUID uuid) {
        return selections.computeIfAbsent(uuid, u -> new Selection());
    }

    public Selection get(UUID uuid) {
        return selections.get(uuid);
    }

    public void setPos1(UUID uuid, Location loc) {
        getOrCreate(uuid).pos1 = loc;
    }

    public void setPos2(UUID uuid, Location loc) {
        getOrCreate(uuid).pos2 = loc;
    }

    public boolean isComplete(UUID uuid) {
        Selection s = selections.get(uuid);
        return s != null && s.pos1 != null && s.pos2 != null;
    }

    public void clear(UUID uuid) {
        selections.remove(uuid);
    }
}

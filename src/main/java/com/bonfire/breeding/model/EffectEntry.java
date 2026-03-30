package com.bonfire.breeding.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class EffectEntry {
    private String type; // "particle" or "sound"
    private String name;
    private int count;
    private double[] spread;
    private float volume;
    private float pitch;
    private int delayTicks;

    public static EffectEntry fromJson(JsonObject json) {
        EffectEntry e = new EffectEntry();
        e.type = json.has("type") ? json.get("type").getAsString() : "particle";
        e.name = json.has("name") ? json.get("name").getAsString() : "";
        e.count = json.has("count") ? json.get("count").getAsInt() : 5;
        e.volume = json.has("volume") ? json.get("volume").getAsFloat() : 1f;
        e.pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 1f;
        e.delayTicks = json.has("delay_ticks") ? json.get("delay_ticks").getAsInt() : 0;
        e.spread = new double[]{0.2, 0.2, 0.2};
        if (json.has("spread")) {
            JsonArray arr = json.getAsJsonArray("spread");
            if (arr.size() >= 3) {
                e.spread[0] = arr.get(0).getAsDouble();
                e.spread[1] = arr.get(1).getAsDouble();
                e.spread[2] = arr.get(2).getAsDouble();
            }
        }
        return e;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("name", name);
        if ("particle".equals(type)) {
            json.addProperty("count", count);
            JsonArray sp = new JsonArray();
            sp.add(spread[0]); sp.add(spread[1]); sp.add(spread[2]);
            json.add("spread", sp);
        } else {
            json.addProperty("volume", volume);
            json.addProperty("pitch", pitch);
        }
        if (delayTicks > 0) json.addProperty("delay_ticks", delayTicks);
        return json;
    }

    public void play(Entity entity, Plugin plugin) {
        if (entity == null || entity.isDead()) return;
        Runnable action = () -> execute(entity);
        if (delayTicks > 0 && plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
        } else {
            action.run();
        }
    }

    private void execute(Entity entity) {
        if (entity.isDead()) return;
        Location loc = entity.getLocation().add(0, 0.5, 0);
        World world = loc.getWorld();
        if (world == null) return;

        if ("particle".equalsIgnoreCase(type)) {
            try {
                Particle p = Particle.valueOf(name);
                world.spawnParticle(p, loc, count, spread[0], spread[1], spread[2]);
            } catch (IllegalArgumentException ignored) {}
        } else if ("sound".equalsIgnoreCase(type)) {
            try {
                Sound s = Sound.valueOf(name);
                world.playSound(loc, s, volume, pitch);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public int getDelayTicks() { return delayTicks; }
    public String getType() { return type; }
    public String getName() { return name; }
}

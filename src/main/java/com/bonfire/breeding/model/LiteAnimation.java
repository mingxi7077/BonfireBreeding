package com.bonfire.breeding.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class LiteAnimation {
    private String sound;
    private String particle;
    private List<EffectEntry> effects;

    public static LiteAnimation fromJson(JsonObject json) {
        if (json == null) return null;
        LiteAnimation anim = new LiteAnimation();

        // Backward-compatible: parse lite_animation block
        if (json.has("lite_animation")) {
            JsonObject lite = json.getAsJsonObject("lite_animation");
            anim.sound = lite.has("sound") ? lite.get("sound").getAsString() : null;
            anim.particle = lite.has("particle") ? lite.get("particle").getAsString() : null;
        }

        // New: parse effects array
        anim.effects = new ArrayList<>();
        if (json.has("effects")) {
            JsonArray arr = json.getAsJsonArray("effects");
            for (JsonElement el : arr) {
                anim.effects.add(EffectEntry.fromJson(el.getAsJsonObject()));
            }
        }

        return anim;
    }

    public JsonObject toJson() {
        JsonObject wrapper = new JsonObject();
        if (sound != null || particle != null) {
            JsonObject lite = new JsonObject();
            if (sound != null) lite.addProperty("sound", sound);
            if (particle != null) lite.addProperty("particle", particle);
            wrapper.add("lite_animation", lite);
        }
        if (effects != null && !effects.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (EffectEntry e : effects) arr.add(e.toJson());
            wrapper.add("effects", arr);
        }
        return wrapper;
    }

    /**
     * Play all effects. lite_animation plays immediately, effects respect delay_ticks.
     */
    public void play(Entity entity) {
        play(entity, null);
    }

    public void play(Entity entity, Plugin plugin) {
        if (entity == null || entity.isDead()) return;
        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Play legacy lite_animation immediately
        playLegacySound(world, loc);
        playLegacyParticle(world, loc);

        // Play new effects (with delay support)
        if (effects != null && plugin != null) {
            for (EffectEntry effect : effects) {
                effect.play(entity, plugin);
            }
        } else if (effects != null) {
            for (EffectEntry effect : effects) {
                if (effect.getDelayTicks() <= 0) {
                    effect.play(entity, null);
                }
            }
        }
    }

    private void playLegacySound(World world, Location loc) {
        if (sound == null || sound.isEmpty()) return;
        String[] parts = sound.split(":");
        try {
            Sound s = Sound.valueOf(parts[0]);
            float volume = parts.length >= 2 ? Float.parseFloat(parts[1]) : 1f;
            float pitch = parts.length >= 3 ? Float.parseFloat(parts[2]) : 1f;
            world.playSound(loc, s, volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    private void playLegacyParticle(World world, Location loc) {
        if (particle == null || particle.isEmpty()) return;
        String[] parts = particle.split(":");
        try {
            Particle p = Particle.valueOf(parts[0]);
            int count = parts.length >= 2 ? Integer.parseInt(parts[1]) : 5;
            double dx = parts.length >= 3 ? Double.parseDouble(parts[2]) : 0.2;
            double dy = parts.length >= 4 ? Double.parseDouble(parts[3]) : 0.2;
            double dz = parts.length >= 5 ? Double.parseDouble(parts[4]) : 0.2;
            world.spawnParticle(p, loc.clone().add(0, 0.5, 0), count, dx, dy, dz);
        } catch (IllegalArgumentException ignored) {}
    }

    public List<EffectEntry> getEffects() { return effects; }
}

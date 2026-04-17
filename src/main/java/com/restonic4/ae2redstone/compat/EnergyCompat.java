package com.restonic4.ae2redstone.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for energy compatibility layers.
 *
 * Each integration class is only instantiated (and therefore only class-loaded)
 * when its owning mod is detected at runtime.  This prevents NoClassDefFoundError
 * crashes for players who don't have every optional mod installed.
 */
public class EnergyCompat {

    private static final List<IEnergyIntegration> INTEGRATIONS = new ArrayList<>();

    public static void init() {
        INTEGRATIONS.clear();

        if (FabricLoader.getInstance().isModLoaded("ad_astra")) {
            tryRegister("com.restonic4.ae2redstone.compat.AdAstraIntegration", "Ad Astra");
        }

        if (FabricLoader.getInstance().isModLoaded("powergrid")) {
            tryRegister("com.restonic4.ae2redstone.compat.PowerGridIntegration", "Create: Power Grid");
        }
    }

    /** Returns all loaded integrations (useful for a future multi-output block). */
    public static List<IEnergyIntegration> getAll() {
        return INTEGRATIONS;
    }

    public static boolean hasAny() {
        return !INTEGRATIONS.isEmpty();
    }

    // -------------------------------------------------------------------------

    private static void tryRegister(String className, String modLabel) {
        try {
            Class<?> cls = Class.forName(className);
            IEnergyIntegration instance = (IEnergyIntegration) cls.getDeclaredConstructor().newInstance();
            INTEGRATIONS.add(instance);
        } catch (Exception e) {
            // Mod present but class failed — log a warning, don't crash.
            System.err.println("[AE2Redstone] Failed to load energy integration for " + modLabel + ": " + e.getMessage());
        }
    }
}
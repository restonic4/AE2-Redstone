package com.restonic4.ae2redstone.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Common interface for all energy compatibility layers.
 * Each implementation wraps a specific mod's energy API and is only
 * class-loaded when that mod is actually present (see EnergyCompat).
 */
public interface IEnergyIntegration {

    /**
     * Attempt to push up to {@code maxAmount} AE-units (already converted)
     * into the target mod's energy network.
     *
     * @param pos       the block position of the converter (used to locate the
     *                  adjacent energy storage on the correct side)
     * @param direction the output face of the converter block
     * @param maxAmount maximum AE energy to transfer this tick
     * @param level     server-side world reference
     * @return the amount of energy that was actually accepted (0 if full / absent)
     */
    long transferEnergy(
            BlockPos pos,
            Direction direction,
            long maxAmount,
            ServerLevel level
    );

    /**
     * @return a human-readable name shown in the GUI / Jade tooltip.
     */
    String getModName();

    /**
     * Conversion ratio: how many target-mod energy units equal 1 AE unit.
     * Example: if Ad Astra uses "Joules" and 1 AE = 2 J, return 2.0.
     */
    double getConversionRatio();
}
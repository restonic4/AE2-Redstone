package com.restonic4.ae2redstone.compat;

import earth.terrarium.botarium.common.energy.EnergyApi;
import earth.terrarium.botarium.common.energy.base.EnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;

public class PowerGridIntegration implements IEnergyIntegration {
    public static final double CONVERSION_RATIO = 0.5;

    @Override
    public long transferEnergy(BlockPos pos, Direction direction, long maxAeAmount, ServerLevel level) {
        BlockPos targetPos = pos.relative(direction);
        BlockEntity targetBe = level.getBlockEntity(targetPos);

        if (!(targetBe instanceof BatteryBlockEntity battery)) {
            return 0;
        }

        double currentEnergy = battery.getEnergy();
        double capacity = battery.getCapacity();
        double spaceLeft = capacity - currentEnergy;

        if (spaceLeft <= 0) {
            return 0;
        }

        double pgAmountAvailable = maxAeAmount * CONVERSION_RATIO;

        double accepted = Math.min(pgAmountAvailable, spaceLeft);

        if (accepted <= 0) {
            return 0;
        }

        battery.setEnergy(currentEnergy + accepted);

        return (long) Math.ceil(accepted / CONVERSION_RATIO);
    }

    @Override
    public String getModName() {
        return "Create: Power Grid";
    }

    @Override
    public double getConversionRatio() {
        return CONVERSION_RATIO;
    }
}
package com.restonic4.ae2redstone.compat;

import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import earth.terrarium.botarium.common.energy.EnergyApi;
import earth.terrarium.botarium.common.energy.base.EnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Converts AE2 energy into Botarium energy (used by Ad Astra).
 *
 * This class must ONLY be loaded when "ad_astra" / "botarium" is present.
 * EnergyCompat handles that guard — never reference this class directly
 * from non-compat code.
 *
 * Conversion: 1 AE  =  2 FE/J  (adjust CONVERSION_RATIO as needed)
 */
public class AdAstraIntegration implements IEnergyIntegration {

    /** How many Botarium energy units equal 1 AE unit. */
    public static final double CONVERSION_RATIO = 2.0;

    @Override
    public long transferEnergy(BlockPos pos, Direction direction, long maxAeAmount, ServerLevel level) {
        // The output face is 'direction'; the adjacent block is one step in that direction.
        BlockPos targetPos = pos.relative(direction);
        BlockEntity targetBe = level.getBlockEntity(targetPos);
        if (targetBe == null) return 0;

        // Ask Botarium for an EnergyContainer on the face that points back at us.
        EnergyContainer container = EnergyApi.getBlockEnergyContainer(targetBe, direction.getOpposite());
        if (container == null) return 0;

        // Convert AE → Botarium units
        long botariumAmount = (long) (maxAeAmount * CONVERSION_RATIO);

        // Simulate first to see how much is actually accepted
        long accepted = container.insertEnergy(botariumAmount, true);
        if (accepted <= 0) return 0;

        // Real insert
        container.insertEnergy(accepted, false);

        // Return how many AE we consumed (round up so we don't give free energy)
        return (long) Math.ceil(accepted / CONVERSION_RATIO);
    }

    @Override
    public String getModName() {
        return "Ad Astra (Botarium)";
    }

    @Override
    public double getConversionRatio() {
        return CONVERSION_RATIO;
    }
}
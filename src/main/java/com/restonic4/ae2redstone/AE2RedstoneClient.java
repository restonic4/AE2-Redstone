package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.EnergyPredictorBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AE2RedstoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // We don't have any client-side specific registries yet,
        // but this is where block render layers or custom models would go.
    }

    public static void openPredictorScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof EnergyPredictorBlockEntity predictor) {
                // Pass the whole BlockEntity instead of just the values
                mc.setScreen(new EnergyPredictorScreen(predictor));
            }
        }
    }
}
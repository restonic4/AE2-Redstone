package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.EnergyPredictorBlockEntity;
import com.restonic4.ae2redstone.screens.EnergyPredictorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AE2RedstoneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {}

    public static void openPredictorScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof EnergyPredictorBlockEntity predictor) {
                mc.setScreen(new EnergyPredictorScreen(predictor));
            }
        }
    }
}
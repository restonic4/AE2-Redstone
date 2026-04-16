package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlockEntity;
import com.restonic4.ae2redstone.block.energy_predictor.EnergyPredictorBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModMessages {
    public static final ResourceLocation UPDATE_PREDICTOR_SETTINGS = new ResourceLocation(AE2Redstone.MOD_ID, "update_predictor_settings");
    public static final ResourceLocation UPDATE_CONVERTER_SETTINGS = new ResourceLocation(AE2Redstone.MOD_ID, "update_converter_settings");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_PREDICTOR_SETTINGS, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            long targetTimeTicks = buf.readLong();
            boolean triggerWhenLessThan = buf.readBoolean();

            server.execute(() -> {
                if (player.level().isLoaded(pos) && player.blockPosition().closerThan(pos, 8)) {
                    BlockEntity be = player.level().getBlockEntity(pos);
                    if (be instanceof EnergyPredictorBlockEntity predictor) {
                        predictor.updateSettings(targetTimeTicks, triggerWhenLessThan);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CONVERTER_SETTINGS,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    long maxTransfer = buf.readLong();

                    server.execute(() -> {
                        if (player.level().isLoaded(pos) && player.blockPosition().closerThan(pos, 8)) {
                            BlockEntity be = player.level().getBlockEntity(pos);
                            if (be instanceof EnergyConverterBlockEntity converter) {
                                converter.updateSettings(maxTransfer);
                            }
                        }
                    });
                });
    }
}
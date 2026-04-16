package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.ModBlocks;
import com.restonic4.ae2redstone.compat.EnergyCompat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static com.restonic4.ae2redstone.block.ModBlocks.CONVERTER_BLOCK;
import static com.restonic4.ae2redstone.block.ModBlocks.PREDICTOR_BLOCK;


public class AE2Redstone implements ModInitializer {
    public static final String MOD_ID = "ae2_redstone";
    public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

    public static final CreativeModeTab AE2_REDSTONE_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(MOD_ID, "main_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.ae2_redstone.main_tab"))
                    .icon(() -> new ItemStack(PREDICTOR_BLOCK))
                    .displayItems((parameters, entries) -> {
                        entries.accept(PREDICTOR_BLOCK);
                        entries.accept(CONVERTER_BLOCK);
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModMessages.registerC2SPackets();
        EnergyCompat.init();
    }
}

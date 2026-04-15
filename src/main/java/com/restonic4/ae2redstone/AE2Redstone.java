package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.EnergyPredictorBlock;
import com.restonic4.ae2redstone.block.EnergyPredictorBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AE2Redstone implements ModInitializer {
    public static final String MOD_ID = "ae2_redstone";
    public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

    public static BlockEntityType<EnergyPredictorBlockEntity> PREDICTOR_BLOCK_ENTITY;
    public static final Block PREDICTOR_BLOCK = new EnergyPredictorBlock(
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0f, 6.0f)
    );

    public static final CreativeModeTab AE2_REDSTONE_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(MOD_ID, "main_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.ae2_redstone.main_tab"))
                    .icon(() -> new ItemStack(PREDICTOR_BLOCK))
                    .displayItems((parameters, entries) -> {
                        entries.accept(PREDICTOR_BLOCK);
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MOD_ID, "energy_predictor"), PREDICTOR_BLOCK);

        // Register Item
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "energy_predictor"),
                new BlockItem(PREDICTOR_BLOCK, new FabricItemSettings()));

        // Register Block Entity
        PREDICTOR_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(MOD_ID, "energy_predictor_be"),
                FabricBlockEntityTypeBuilder.create(EnergyPredictorBlockEntity::new, PREDICTOR_BLOCK).build()
        );

        ModMessages.registerC2SPackets();
    }
}

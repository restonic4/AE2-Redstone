package com.restonic4.ae2redstone.block;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.BlockDefinition;
import com.google.common.base.Preconditions;
import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlock;
import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlockEntity;
import com.restonic4.ae2redstone.block.energy_predictor.EnergyPredictorBlock;
import com.restonic4.ae2redstone.block.energy_predictor.EnergyPredictorBlockEntity;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static com.restonic4.ae2redstone.AE2Redstone.MOD_ID;

public class ModBlocks {
    public static final EnergyPredictorBlock PREDICTOR_BLOCK = new EnergyPredictorBlock();
    public static BlockEntityType<EnergyPredictorBlockEntity> PREDICTOR_BLOCK_ENTITY;

    public static final EnergyConverterBlock CONVERTER_BLOCK = new EnergyConverterBlock();
    public static BlockEntityType<EnergyConverterBlockEntity> CONVERTER_BLOCK_ENTITY;

    public static void register() {
        PREDICTOR_BLOCK_ENTITY = registerAE(
                "energy_predictor",
                PREDICTOR_BLOCK,
                EnergyPredictorBlockEntity.class,
                EnergyPredictorBlockEntity::new
        );

        CONVERTER_BLOCK_ENTITY = registerAE(
                "energy_converter",
                CONVERTER_BLOCK,
                EnergyConverterBlockEntity.class,
                EnergyConverterBlockEntity::new
        );
    }

    private static void registerBlockWithItem(String id, Block block) {
        ResourceLocation identifier = new ResourceLocation(MOD_ID, id);
        Registry.register(BuiltInRegistries.BLOCK, identifier, block);
        Registry.register(BuiltInRegistries.ITEM, identifier, new BlockItem(block, new FabricItemSettings()));
    }

    private static <T extends AEBaseBlockEntity> BlockEntityType<T> registerAE(
            String id,
            AEBaseEntityBlock<T> block,
            Class<T> beClass,
            FabricBlockEntityTypeBuilder.Factory<T> factory
    ) {
        registerBlockWithItem(id, block);

        BlockEntityType<T> type = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(MOD_ID, id + "_be"),
                FabricBlockEntityTypeBuilder.create(factory, block).build()
        );

        BlockEntityTicker<T> serverTicker = null;
        if (ServerTickingBlockEntity.class.isAssignableFrom(beClass)) {
            serverTicker = (level, pos, state, entity) -> {
                ((ServerTickingBlockEntity) entity).serverTick();
            };
        }
        BlockEntityTicker<T> clientTicker = null;
        if (ClientTickingBlockEntity.class.isAssignableFrom(beClass)) {
            clientTicker = (level, pos, state, entity) -> {
                ((ClientTickingBlockEntity) entity).clientTick();
            };
        }

        block.setBlockEntity(beClass, type, clientTicker, serverTicker);

        return type;
    }
}
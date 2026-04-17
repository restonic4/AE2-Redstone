package com.restonic4.ae2redstone.block;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
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
                EnergyPredictorBlockEntity::new,
                true,
                false
        );

        CONVERTER_BLOCK_ENTITY = registerAE(
                "energy_converter",
                CONVERTER_BLOCK,
                EnergyConverterBlockEntity.class,
                EnergyConverterBlockEntity::new,
                true,
                false
        );
    }

    private static void registerBlockWithItem(String id, Block block) {
        ResourceLocation identifier = new ResourceLocation(MOD_ID, id);
        Registry.register(BuiltInRegistries.BLOCK, identifier, block);
        Registry.register(BuiltInRegistries.ITEM, identifier, new BlockItem(block, new FabricItemSettings()));
    }

    private static <T extends AEBaseBlockEntity & ITickableBlockEntity> BlockEntityType<T> registerAE(
            String id,
            AEBaseEntityBlock<T> block,
            Class<T> beClass,
            FabricBlockEntityTypeBuilder.Factory<T> factory,
            boolean tickServer,
            boolean tickClient
    ) {
        registerBlockWithItem(id, block);

        BlockEntityType<T> type = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(MOD_ID, id + "_be"),
                FabricBlockEntityTypeBuilder.create(factory, block).build()
        );

        BlockEntityTicker<T> serverTicker = tickServer ? (lvl, pos, st, be) -> be.tick() : null;
        BlockEntityTicker<T> clientTicker = tickClient ? (lvl, pos, st, be) -> be.tick() : null;

        block.setBlockEntity(beClass, type, clientTicker, serverTicker);

        return type;
    }
}
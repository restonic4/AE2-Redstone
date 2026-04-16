package com.restonic4.ae2redstone.block.energy_converter;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import com.restonic4.ae2redstone.AE2RedstoneClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Energy Converter block.
 *
 * Orientation: uses the full 6-direction facing strategy (like a piston).
 *   - BACK  face  →  AE2 cable connection
 *   - FRONT face  →  target mod energy output
 *
 * The ACTIVE property lights up the front face when the block is pumping energy.
 */
public class EnergyConverterBlock extends AEBaseEntityBlock<EnergyConverterBlockEntity> {

    /** True while the block is actively transferring energy this tick. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public EnergyConverterBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .requiresCorrectToolForDrops()
                .strength(3.5f, 6.0f)
                .lightLevel(state -> state.getValue(ACTIVE) ? 7 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    // -------------------------------------------------------------------------
    // Block state

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /**
     * Full 6-direction facing (all faces, like a piston / dispenser).
     * The player places it pointing away from themselves so the FRONT faces
     * the target inventory/cable, matching natural intuition.
     */
    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    // -------------------------------------------------------------------------
    // AE2 cable connectivity — only the BACK face connects to the AE network.

    // AEBaseEntityBlock uses getOrientationStrategy() to resolve sides, so
    // EnergyConverterBlockEntity.getGridConnectableSides() restricts it to BACK only.

    // -------------------------------------------------------------------------
    // Interaction — open settings screen on right-click

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            AE2RedstoneClient.openConverterScreen(pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
}
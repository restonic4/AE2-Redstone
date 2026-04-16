package com.restonic4.ae2redstone.block.energy_predictor;

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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class EnergyPredictorBlock extends AEBaseEntityBlock<EnergyPredictorBlockEntity> {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public EnergyPredictorBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(3.0f, 6.0f));
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (direction == facing) {
            if (level.getBlockEntity(pos) instanceof EnergyPredictorBlockEntity predictor) {
                return predictor.isEmitting() ? 15 : 0;
            }
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            AE2RedstoneClient.openPredictorScreen(pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
}

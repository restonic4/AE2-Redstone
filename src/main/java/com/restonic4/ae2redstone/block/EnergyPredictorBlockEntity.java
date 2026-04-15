package com.restonic4.ae2redstone.block;

import appeng.api.networking.*;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.util.AECableType;
import com.restonic4.ae2redstone.AE2Redstone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyPredictorBlockEntity extends BlockEntity implements IInWorldGridNodeHost {

    private boolean isEmitting = false;
    private long targetTimeTicks = 2000;
    private boolean triggerWhenLessThan = true;

    // Debug Variables
    private double debugStored = 0;
    private double debugGen = 0;
    private double debugUsage = 0;
    private double debugNet = 0;
    private double debugTicksToEmpty = -1;

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, new NodeListener());

    public EnergyPredictorBlockEntity(BlockPos pos, BlockState state) {
        super(AE2Redstone.PREDICTOR_BLOCK_ENTITY, pos, state);
        this.mainNode.setIdlePowerUsage(1.0);
        this.mainNode.setInWorldNode(true);
    }

    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        if (dir != null && this.getBlockState().hasProperty(EnergyPredictorBlock.FACING)) {
            Direction facing = this.getBlockState().getValue(EnergyPredictorBlock.FACING);
            if (dir == facing.getOpposite()) {
                return mainNode.getNode();
            }
            return null;
        }
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    public boolean isEmitting() {
        return isEmitting;
    }

    public void setEmitting(boolean shouldEmit) {
        if (this.isEmitting != shouldEmit) {
            this.isEmitting = shouldEmit;
            this.setChanged();

            if (this.level != null && !this.level.isClientSide()) {
                BlockState currentState = this.getBlockState();

                if (currentState.hasProperty(EnergyPredictorBlock.POWERED)) {
                    this.level.setBlock(this.worldPosition, currentState.setValue(EnergyPredictorBlock.POWERED, shouldEmit), 3);
                }

                this.level.updateNeighborsAt(this.getBlockPos(), currentState.getBlock());
            }
        }
    }

    public void tick() {
        if (this.level == null || this.level.isClientSide()) return;

        if (!this.mainNode.isReady()) {
            this.mainNode.create(this.level, this.getBlockPos());
        }

        if (this.level.getGameTime() % 5 != 0) return;

        if (!this.mainNode.isReady() || this.mainNode.getNode() == null || this.mainNode.getNode().getGrid() == null) {
            if (AE2Redstone.IS_DEV) {
                this.debugStored = 0;
                this.debugGen = 0;
                this.debugUsage = 0;
                this.debugNet = 0;
                this.debugTicksToEmpty = 0;
            }

            setEmitting(false);
            return;
        }

        IEnergyService energyService = this.mainNode.getNode().getGrid().getService(IEnergyService.class);
        if (energyService == null) {
            setEmitting(false);
            return;
        }

        // Check if the network has storage
        double maxStored = energyService.getMaxStoredPower();
        if (maxStored <= 0) {
            if (AE2Redstone.IS_DEV) {
                this.debugStored = 0;
                this.debugGen = 0;
                this.debugUsage = 0;
                this.debugNet = 0;
                this.debugTicksToEmpty = 0;
            }

            setEmitting(false);
            return;
        }

        double stored = energyService.getStoredPower();
        double generation = energyService.getAvgPowerInjection();
        double usage = energyService.getAvgPowerUsage();
        double netRate = generation - usage;

        if (AE2Redstone.IS_DEV) {
            this.debugStored = stored;
            this.debugGen = generation;
            this.debugUsage = usage;
            this.debugNet = netRate;
        }

        boolean shouldEmit = false;

        if (stored <= 0 && netRate <= 0) {
            if (AE2Redstone.IS_DEV) this.debugTicksToEmpty = 0;
            shouldEmit = triggerWhenLessThan;
        } else if (netRate >= 0) {
            if (AE2Redstone.IS_DEV) this.debugTicksToEmpty = -1;
            shouldEmit = !triggerWhenLessThan;
        } else {
            double drainRate = Math.abs(netRate);
            double ticksToEmpty = stored / drainRate;
            if (AE2Redstone.IS_DEV) this.debugTicksToEmpty = ticksToEmpty;

            if (triggerWhenLessThan) {
                shouldEmit = ticksToEmpty <= targetTimeTicks;
            } else {
                shouldEmit = ticksToEmpty > targetTimeTicks;
            }
        }

        setEmitting(shouldEmit);

        if (AE2Redstone.IS_DEV && this.level.getGameTime() % 20 == 0) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public double getDebugStored() { return debugStored; }
    public double getDebugGen() { return debugGen; }
    public double getDebugUsage() { return debugUsage; }
    public double getDebugNet() { return debugNet; }
    public double getDebugTicksToEmpty() { return debugTicksToEmpty; }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("TargetTimeTicks", this.targetTimeTicks);
        tag.putBoolean("TriggerWhenLessThan", this.triggerWhenLessThan);

        if (AE2Redstone.IS_DEV) {
            tag.putDouble("DebugStored", this.debugStored);
            tag.putDouble("DebugGen", this.debugGen);
            tag.putDouble("DebugUsage", this.debugUsage);
            tag.putDouble("DebugNet", this.debugNet);
            tag.putDouble("DebugTicksToEmpty", this.debugTicksToEmpty);
            tag.putBoolean("IsEmitting", this.isEmitting);
        }

        if (this.mainNode.isReady()) {
            this.mainNode.saveToNBT(tag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("TargetTimeTicks")) this.targetTimeTicks = tag.getLong("TargetTimeTicks");
        if (tag.contains("TriggerWhenLessThan")) this.triggerWhenLessThan = tag.getBoolean("TriggerWhenLessThan");

        if (AE2Redstone.IS_DEV) {
            this.debugStored = tag.getDouble("DebugStored");
            this.debugGen = tag.getDouble("DebugGen");
            this.debugUsage = tag.getDouble("DebugUsage");
            this.debugNet = tag.getDouble("DebugNet");
            this.debugTicksToEmpty = tag.getDouble("DebugTicksToEmpty");
            this.isEmitting = tag.getBoolean("IsEmitting");
        }

        this.mainNode.loadFromNBT(tag);
    }

    private static class NodeListener implements IGridNodeListener<EnergyPredictorBlockEntity> {
        @Override
        public void onSaveChanges(EnergyPredictorBlockEntity nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    public long getTargetTimeTicks() {
        return targetTimeTicks;
    }

    public boolean isTriggerWhenLessThan() {
        return triggerWhenLessThan;
    }

    public void updateSettings(long timeTicks, boolean triggerLess) {
        this.targetTimeTicks = timeTicks;
        this.triggerWhenLessThan = triggerLess;
        this.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.mainNode.destroy();
    }
}
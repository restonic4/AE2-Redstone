package com.restonic4.ae2redstone.block;

import appeng.api.networking.*;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.restonic4.ae2redstone.AE2Redstone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

import static com.restonic4.ae2redstone.block.ModBlocks.PREDICTOR_BLOCK;
import static com.restonic4.ae2redstone.block.ModBlocks.PREDICTOR_BLOCK_ENTITY;

public class EnergyPredictorBlockEntity extends AENetworkBlockEntity implements ITickableBlockEntity {

    private boolean isEmitting = false;
    private long targetTimeTicks = 2000;
    private boolean triggerWhenLessThan = true;

    // These are always stored (not just dev) so Jade and other mods can read them
    private double storedPower = 0;
    private double generation = 0;
    private double usage = 0;
    private double netRate = 0;
    private double ticksToEmpty = -1; // -1 means infinite (charging or full)

    public EnergyPredictorBlockEntity(BlockPos pos, BlockState state) {
        super(PREDICTOR_BLOCK_ENTITY, pos, state);
        this.getMainNode().setFlags();
        this.getMainNode().setIdlePowerUsage(1.0);
        this.getMainNode().setVisualRepresentation(PREDICTOR_BLOCK);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return orientation.getSides(EnumSet.of(RelativeSide.FRONT));
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
                    this.level.setBlock(this.worldPosition, currentState.setValue(EnergyPredictorBlock.POWERED, shouldEmit), 2);
                }

                this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
            }
        }
    }

    @Override
    public void tick() {
        if (this.level == null || this.level.isClientSide()) return;
        if (!getMainNode().isReady()) return;

        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) return;

        IEnergyService energyService = grid.getService(IEnergyService.class);
        if (energyService == null) return;

        if (this.level.getGameTime() % 5 != 0) return;

        if (!this.getMainNode().isReady() || this.getMainNode().getNode() == null || this.getMainNode().getNode().getGrid() == null) {
            this.storedPower = 0;
            this.generation = 0;
            this.usage = 0;
            this.netRate = 0;
            this.ticksToEmpty = 0;

            setEmitting(false);
            return;
        }

        double maxStored = energyService.getMaxStoredPower();
        if (maxStored <= 0) {
            this.storedPower = 0;
            this.generation = 0;
            this.usage = 0;
            this.netRate = 0;
            this.ticksToEmpty = 0;

            setEmitting(false);
            return;
        }

        this.storedPower = energyService.getStoredPower();
        this.generation = energyService.getAvgPowerInjection();
        this.usage = energyService.getAvgPowerUsage();
        this.netRate = this.generation - this.usage;

        boolean shouldEmit = false;

        if (this.storedPower <= 0 && this.netRate <= 0) {
            this.ticksToEmpty = 0;
            shouldEmit = triggerWhenLessThan;
        } else if (this.netRate >= 0) {
            this.ticksToEmpty = -1;
            shouldEmit = !triggerWhenLessThan;
        } else {
            double drainRate = Math.abs(this.netRate);
            this.ticksToEmpty = this.storedPower / drainRate;

            if (triggerWhenLessThan) {
                shouldEmit = this.ticksToEmpty <= targetTimeTicks;
            } else {
                shouldEmit = this.ticksToEmpty > targetTimeTicks;
            }
        }

        setEmitting(shouldEmit);

        if (this.level.getGameTime() % 20 == 0) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    public double getStoredPower() { return storedPower; }
    public double getGeneration() { return generation; }
    public double getUsage() { return usage; }
    public double getNetRate() { return netRate; }
    public double getTicksToEmpty() { return ticksToEmpty; }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putLong("TargetTimeTicks", this.targetTimeTicks);
        tag.putBoolean("TriggerWhenLessThan", this.triggerWhenLessThan);
        tag.putBoolean("IsEmitting", this.isEmitting);

        // Always save runtime data so Jade can read it on load
        tag.putDouble("StoredPower", this.storedPower);
        tag.putDouble("Generation", this.generation);
        tag.putDouble("Usage", this.usage);
        tag.putDouble("NetRate", this.netRate);
        tag.putDouble("TicksToEmpty", this.ticksToEmpty);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);

        this.targetTimeTicks = data.getLong("TargetTimeTicks");
        this.triggerWhenLessThan = data.getBoolean("TriggerWhenLessThan");
        this.storedPower = data.getDouble("StoredPower");
        this.generation = data.getDouble("Generation");
        this.usage = data.getDouble("Usage");
        this.netRate = data.getDouble("NetRate");
        this.ticksToEmpty = data.getDouble("TicksToEmpty");
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
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        this.isEmitting = data.readBoolean();
        this.targetTimeTicks = data.readLong();
        this.triggerWhenLessThan = data.readBoolean();
        this.storedPower = data.readDouble();
        this.generation = data.readDouble();
        this.usage = data.readDouble();
        this.netRate = data.readDouble();
        this.ticksToEmpty = data.readDouble();
        return true; // Return true to trigger a re-render
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isEmitting);
        data.writeLong(this.targetTimeTicks);
        data.writeBoolean(this.triggerWhenLessThan);
        data.writeDouble(this.storedPower);
        data.writeDouble(this.generation);
        data.writeDouble(this.usage);
        data.writeDouble(this.netRate);
        data.writeDouble(this.ticksToEmpty);
    }
}
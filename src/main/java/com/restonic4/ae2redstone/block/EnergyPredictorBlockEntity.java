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

    // These are always stored (not just dev) so Jade and other mods can read them
    private double storedPower = 0;
    private double generation = 0;
    private double usage = 0;
    private double netRate = 0;
    private double ticksToEmpty = -1; // -1 means infinite (charging or full)

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, new NodeListener());

    public EnergyPredictorBlockEntity(BlockPos pos, BlockState state) {
        super(AE2Redstone.PREDICTOR_BLOCK_ENTITY, pos, state);
        this.mainNode.setIdlePowerUsage(1.0);
        this.mainNode.setInWorldNode(true);
        this.mainNode.setVisualRepresentation(AE2Redstone.PREDICTOR_BLOCK);
    }

    public IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    /**
     * Returns the AE2 grid node only for the back face (opposite of the redstone output face).
     * All other directions return null so AE2 does not try to connect from them.
     */
    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        if (dir == null) return null;

        if (this.getBlockState().hasProperty(EnergyPredictorBlock.FACING)) {
            Direction facing = this.getBlockState().getValue(EnergyPredictorBlock.FACING);
            // Only expose the node on the back face (opposite of the redstone output)
            if (dir == facing.getOpposite()) {
                return mainNode.getNode();
            }
            return null;
        }

        return mainNode.getNode();
    }

    /**
     * Reports cable connection type per direction.
     * SMART only on the back face; NONE everywhere else.
     * This is what controls the visual cable rendering in AE2.
     */
    @Override
    public AECableType getCableConnectionType(Direction dir) {
        if (dir == null) return AECableType.NONE;

        if (this.getBlockState().hasProperty(EnergyPredictorBlock.FACING)) {
            Direction facing = this.getBlockState().getValue(EnergyPredictorBlock.FACING);
            if (dir == facing.getOpposite()) {
                return AECableType.SMART;
            }
            return AECableType.NONE;
        }

        return AECableType.NONE;
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
                    // Flag 2 = send update to client only, no block update to neighbors yet.
                    // Flag 1 = notify neighbors. We call updateNeighborsAt separately to avoid
                    // triggering AE2 node invalidation from a combined flag-3 block update.
                    this.level.setBlock(this.worldPosition, currentState.setValue(EnergyPredictorBlock.POWERED, shouldEmit), 2);
                }

                this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
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
            this.storedPower = 0;
            this.generation = 0;
            this.usage = 0;
            this.netRate = 0;
            this.ticksToEmpty = 0;

            setEmitting(false);
            return;
        }

        IEnergyService energyService = this.mainNode.getNode().getGrid().getService(IEnergyService.class);
        if (energyService == null) {
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
            this.ticksToEmpty = -1; // Infinite — network is gaining power
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

        // Always sync to client so Jade and the GUI have fresh data
        if (this.level.getGameTime() % 20 == 0) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    // --- Getters for Jade / GUI ---

    public double getStoredPower() { return storedPower; }
    public double getGeneration() { return generation; }
    public double getUsage() { return usage; }
    public double getNetRate() { return netRate; }
    public double getTicksToEmpty() { return ticksToEmpty; }

    // --- NBT ---

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

        if (this.mainNode.isReady()) {
            this.mainNode.saveToNBT(tag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("TargetTimeTicks")) this.targetTimeTicks = tag.getLong("TargetTimeTicks");
        if (tag.contains("TriggerWhenLessThan")) this.triggerWhenLessThan = tag.getBoolean("TriggerWhenLessThan");
        if (tag.contains("IsEmitting")) this.isEmitting = tag.getBoolean("IsEmitting");

        if (tag.contains("StoredPower")) this.storedPower = tag.getDouble("StoredPower");
        if (tag.contains("Generation")) this.generation = tag.getDouble("Generation");
        if (tag.contains("Usage")) this.usage = tag.getDouble("Usage");
        if (tag.contains("NetRate")) this.netRate = tag.getDouble("NetRate");
        if (tag.contains("TicksToEmpty")) this.ticksToEmpty = tag.getDouble("TicksToEmpty");

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
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }
}
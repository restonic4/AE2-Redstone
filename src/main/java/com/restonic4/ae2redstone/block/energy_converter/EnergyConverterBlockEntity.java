package com.restonic4.ae2redstone.block.energy_converter;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.restonic4.ae2redstone.block.ITickableBlockEntity;
import com.restonic4.ae2redstone.compat.EnergyCompat;
import com.restonic4.ae2redstone.compat.IEnergyIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

import static com.restonic4.ae2redstone.block.ModBlocks.CONVERTER_BLOCK;
import static com.restonic4.ae2redstone.block.ModBlocks.CONVERTER_BLOCK_ENTITY;

/**
 * Block entity for the Energy Converter.
 *
 * Every 5 ticks on the server it:
 *  1. Reads the current AE grid energy state.
 *  2. Asks the active {@link IEnergyIntegration} how much can be accepted.
 *  3. Extracts that amount from the AE network and pushes it to the target mod.
 *  4. Updates the ACTIVE blockstate so the texture can react.
 *
 * Idle power cost: 2 AE/t (set via setIdlePowerUsage).
 * Transfer cap:    configurable via the GUI (default 1000 AE/t).
 */
public class EnergyConverterBlockEntity extends AENetworkBlockEntity implements ITickableBlockEntity {

    // --- Configurable settings (synced to client) ---
    private long maxTransferPerTick = 1000L;   // AE units per tick

    // --- Runtime state (server-authoritative, synced for display) ---
    private boolean isActive = false;
    private long lastTransferred = 0L;         // AE extracted last transfer cycle
    private double storedPower = 0;
    private double networkCapacity = 0;

    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(CONVERTER_BLOCK_ENTITY, pos, state);
        this.getMainNode().setFlags();
        this.getMainNode().setIdlePowerUsage(10.0);
        this.getMainNode().setVisualRepresentation(CONVERTER_BLOCK);
    }

    // -------------------------------------------------------------------------
    // AE2 grid connectivity — BACK face only (RelativeSide.BACK)

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return orientation.getSides(EnumSet.of(RelativeSide.BACK));
    }

    // -------------------------------------------------------------------------
    // Tick

    @Override
    public void tick() {
        if (this.level == null || this.level.isClientSide()) return;
        if (!getMainNode().isReady()) return;

        IGrid grid = getMainNode().getGrid();
        if (grid == null) return;

        // Only run every 5 ticks to match the energy predictor style
        if (this.level.getGameTime() % 5 != 0) return;

        IEnergyService energyService = grid.getService(IEnergyService.class);
        if (energyService == null) return;

        this.storedPower   = energyService.getStoredPower();
        this.networkCapacity = energyService.getMaxStoredPower();

        // Get the integration (null = no compatible mod loaded)
        IEnergyIntegration integration = EnergyCompat.getFirst();

        if (integration == null || this.storedPower <= 0) {
            setActive(false);
            this.lastTransferred = 0;
            syncToClient();
            return;
        }

        // How much we're allowed to push this cycle (5 ticks worth)
        long budgetAe = maxTransferPerTick * 5L;
        // Don't try to extract more than what the network actually has
        long availableAe = (long) this.storedPower;
        long toTransfer = Math.min(budgetAe, availableAe);

        if (toTransfer <= 0) {
            setActive(false);
            this.lastTransferred = 0;
            syncToClient();
            return;
        }

        // Get the output direction (FRONT face = facing direction)
        Direction facing = getFacing();
        if (facing == null) {
            setActive(false);
            syncToClient();
            return;
        }

        // Let the integration push the energy; it returns how much AE was consumed
        long actuallyConsumedAe = integration.transferEnergy(
                this.worldPosition, facing, toTransfer, (ServerLevel) this.level
        );

        if (actuallyConsumedAe > 0) {
            // Extract from AE network
            energyService.extractAEPower(actuallyConsumedAe, Actionable.MODULATE, PowerMultiplier.CONFIG);
            this.lastTransferred = actuallyConsumedAe;
            setActive(true);
        } else {
            // Target was full or absent
            this.lastTransferred = 0;
            setActive(false);
        }

        syncToClient();
    }

    // -------------------------------------------------------------------------
    // Helpers

    /** Returns the direction the block's FRONT face is pointing (the output side). */
    @Nullable
    private Direction getFacing() {
        if (this.level == null) return null;
        BlockState state = this.getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        // Fallback for horizontal-only strategies
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private void setActive(boolean active) {
        if (this.isActive == active) return;
        this.isActive = active;
        this.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            BlockState current = this.getBlockState();
            if (current.hasProperty(EnergyConverterBlock.ACTIVE)) {
                this.level.setBlock(this.worldPosition, current.setValue(EnergyConverterBlock.ACTIVE, active), 2);
            }
        }
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide()
                && this.level.getGameTime() % 20 == 0) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    // -------------------------------------------------------------------------
    // Public getters (used by GUI & Jade)

    public boolean isActive()              { return isActive; }
    public long getLastTransferred()       { return lastTransferred; }
    public long getMaxTransferPerTick()    { return maxTransferPerTick; }
    public double getStoredPower()         { return storedPower; }
    public double getNetworkCapacity()     { return networkCapacity; }

    // -------------------------------------------------------------------------
    // Settings update (called from server-side packet handler)

    public void updateSettings(long newMaxTransfer) {
        this.maxTransferPerTick = Math.max(1, newMaxTransfer);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("MaxTransferPerTick", this.maxTransferPerTick);
        tag.putBoolean("IsActive", this.isActive);
        tag.putLong("LastTransferred", this.lastTransferred);
        tag.putDouble("StoredPower", this.storedPower);
        tag.putDouble("NetworkCapacity", this.networkCapacity);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.maxTransferPerTick = data.getLong("MaxTransferPerTick");
        if (this.maxTransferPerTick <= 0) this.maxTransferPerTick = 1000;
        this.isActive       = data.getBoolean("IsActive");
        this.lastTransferred = data.getLong("LastTransferred");
        this.storedPower    = data.getDouble("StoredPower");
        this.networkCapacity = data.getDouble("NetworkCapacity");
    }

    // -------------------------------------------------------------------------
    // Client sync packets

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

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        this.isActive          = data.readBoolean();
        this.maxTransferPerTick = data.readLong();
        this.lastTransferred   = data.readLong();
        this.storedPower       = data.readDouble();
        this.networkCapacity   = data.readDouble();
        return true;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isActive);
        data.writeLong(this.maxTransferPerTick);
        data.writeLong(this.lastTransferred);
        data.writeDouble(this.storedPower);
        data.writeDouble(this.networkCapacity);
    }
}
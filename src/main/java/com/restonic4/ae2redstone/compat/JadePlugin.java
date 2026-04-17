package com.restonic4.ae2redstone.compat;

import com.restonic4.ae2redstone.AE2Redstone;
import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlock;
import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlockEntity;
import com.restonic4.ae2redstone.block.energy_predictor.EnergyPredictorBlock;
import com.restonic4.ae2redstone.block.energy_predictor.EnergyPredictorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    public static final ResourceLocation ENERGY_PREDICTOR_DATA =
            new ResourceLocation(AE2Redstone.MOD_ID, "energy_predictor");

    public static final ResourceLocation ENERGY_CONVERTER_DATA =
            new ResourceLocation(AE2Redstone.MOD_ID, "energy_converter");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyPredictorProvider.INSTANCE, EnergyPredictorBlockEntity.class);
        registration.registerBlockDataProvider(EnergyConverterProvider.INSTANCE, EnergyConverterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyPredictorProvider.INSTANCE, EnergyPredictorBlock.class);
        registration.registerBlockComponent(EnergyConverterProvider.INSTANCE, EnergyConverterBlock.class);
    }

    // =========================================================================
    // Energy Predictor provider (unchanged from original)
    // =========================================================================

    public static class EnergyPredictorProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {

        public static final EnergyPredictorProvider INSTANCE = new EnergyPredictorProvider();

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (!(be instanceof EnergyPredictorBlockEntity predictor)) return;
            tag.putDouble("StoredPower", predictor.getStoredPower());
            tag.putDouble("Generation", predictor.getGeneration());
            tag.putDouble("Usage", predictor.getUsage());
            tag.putDouble("NetRate", predictor.getNetRate());
            tag.putDouble("TicksToEmpty", predictor.getTicksToEmpty());
            tag.putLong("TargetTimeTicks", predictor.getTargetTimeTicks());
            tag.putBoolean("IsEmitting", predictor.isEmitting());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            if (tag == null || !tag.contains("TicksToEmpty")) return;

            double ticksToEmpty    = tag.getDouble("TicksToEmpty");
            double storedPower     = tag.getDouble("StoredPower");
            double netRate         = tag.getDouble("NetRate");
            long   targetTimeTicks = tag.getLong("TargetTimeTicks");
            boolean emitting       = tag.getBoolean("IsEmitting");

            if (ticksToEmpty < 0) {
                tooltip.add(Component.literal("§aTime to empty: §fInfinite §7(charging)"));
            } else if (ticksToEmpty == 0) {
                tooltip.add(Component.literal("§cTime to empty: §fDepleted"));
            } else {
                tooltip.add(Component.literal("§eTime to empty: §f" + formatTime(ticksToEmpty / 20.0)));
            }

            tooltip.add(Component.literal("§7Target: §f" + formatTime(targetTimeTicks / 20.0)));
            String rateColor = netRate >= 0 ? "§a" : "§c";
            tooltip.add(Component.literal("§7Net rate: " + rateColor + String.format("%.1f AE/t", netRate)));
            tooltip.add(Component.literal(String.format("§7Stored: §f%.0f AE", storedPower)));
            tooltip.add(Component.literal(emitting ? "§cRedstone: §fON" : "§7Redstone: §fOFF"));
        }

        @Override
        public ResourceLocation getUid() { return ENERGY_PREDICTOR_DATA; }
    }

    // =========================================================================
    // Energy Converter provider
    // =========================================================================

    public static class EnergyConverterProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {

        public static final EnergyConverterProvider INSTANCE = new EnergyConverterProvider();

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (!(be instanceof EnergyConverterBlockEntity converter)) return;
            tag.putBoolean("IsActive",         converter.isActive());
            tag.putLong("LastTransferred",      converter.getLastTransferred());
            tag.putLong("MaxTransferPerTick",   converter.getMaxTransferPerTick());
            tag.putDouble("StoredPower",        converter.getStoredPower());
            tag.putDouble("NetworkCapacity",    converter.getNetworkCapacity());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            if (tag == null || !tag.contains("IsActive")) return;

            boolean active          = tag.getBoolean("IsActive");
            long    lastTransferred = tag.getLong("LastTransferred");
            long    maxTransfer     = tag.getLong("MaxTransferPerTick");
            double  stored          = tag.getDouble("StoredPower");
            double  capacity        = tag.getDouble("NetworkCapacity");

            // --- SMART JADE DISPLAY ---
            String activeModName = tag.getString("ActiveModName");
            String displayMod = (activeModName != null && !activeModName.isEmpty()) ? activeModName : "None";

            tooltip.add(Component.literal("§7Output: §f" + displayMod));
            // --------------------------

            // Active state
            tooltip.add(Component.literal(active ? "§aTransferring" : "§7Idle"));

            // Last cycle transfer (per 5 ticks)
            tooltip.add(Component.literal(String.format("§7Transferred: §f%d AE §7(last cycle)", lastTransferred)));
            tooltip.add(Component.literal(String.format("§7Cap: §f%d AE/t", maxTransfer)));

            // AE network fill level
            if (capacity > 0) {
                double pct = (stored / capacity) * 100.0;
                tooltip.add(Component.literal(String.format("§7AE: §f%.0f / %.0f §7(%.1f%%)", stored, capacity, pct)));
            }
        }

        @Override
        public ResourceLocation getUid() { return ENERGY_CONVERTER_DATA; }
    }

    // -------------------------------------------------------------------------

    private static String formatTime(double totalSeconds) {
        if (totalSeconds < 60) {
            return String.format("%.1fs", totalSeconds);
        } else if (totalSeconds < 3600) {
            long m = (long)(totalSeconds / 60);
            long s = (long)(totalSeconds % 60);
            return String.format("%dm %02ds", m, s);
        } else {
            long h = (long)(totalSeconds / 3600);
            long m = (long)((totalSeconds % 3600) / 60);
            long s = (long)(totalSeconds % 60);
            return String.format("%dh %02dm %02ds", h, m, s);
        }
    }
}
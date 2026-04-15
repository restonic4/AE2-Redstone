package com.restonic4.ae2redstone.compat;

import com.restonic4.ae2redstone.AE2Redstone;
import com.restonic4.ae2redstone.block.EnergyPredictorBlock;
import com.restonic4.ae2redstone.block.EnergyPredictorBlockEntity;
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

    // Unique ID for this data provider — must match between server and client sides
    public static final ResourceLocation ENERGY_PREDICTOR_DATA =
            new ResourceLocation(AE2Redstone.MOD_ID, "energy_predictor");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyPredictorProvider.INSTANCE, EnergyPredictorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyPredictorProvider.INSTANCE, EnergyPredictorBlock.class);
    }

    // Combined server + client provider
    public static class EnergyPredictorProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {

        public static final EnergyPredictorProvider INSTANCE = new EnergyPredictorProvider();

        /**
         * Server side: write data into the tag that gets sent to the client.
         */
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

        /**
         * Client side: read the tag and add lines to the Jade tooltip.
         */
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            if (tag == null || !tag.contains("TicksToEmpty")) return;

            double ticksToEmpty = tag.getDouble("TicksToEmpty");
            double storedPower = tag.getDouble("StoredPower");
            double netRate = tag.getDouble("NetRate");
            long targetTimeTicks = tag.getLong("TargetTimeTicks");
            boolean emitting = tag.getBoolean("IsEmitting");

            // --- Time to empty line ---
            if (ticksToEmpty < 0) {
                tooltip.add(Component.literal("§aTime to empty: §fInfinite §7(charging)"));
            } else if (ticksToEmpty == 0) {
                tooltip.add(Component.literal("§cTime to empty: §fDepleted"));
            } else {
                double secondsToEmpty = ticksToEmpty / 20.0;
                String timeStr = formatTime(secondsToEmpty);
                tooltip.add(Component.literal("§eTime to empty: §f" + timeStr));
            }

            // --- Target time line ---
            double targetSeconds = targetTimeTicks / 20.0;
            tooltip.add(Component.literal("§7Target: §f" + formatTime(targetSeconds)));

            // --- Net rate line ---
            String rateColor = netRate >= 0 ? "§a" : "§c";
            tooltip.add(Component.literal("§7Net rate: " + rateColor + String.format("%.1f AE/t", netRate)));

            // --- Stored power ---
            tooltip.add(Component.literal(String.format("§7Stored: §f%.0f AE", storedPower)));

            // --- Redstone state ---
            if (emitting) {
                tooltip.add(Component.literal("§cRedstone: §fON"));
            } else {
                tooltip.add(Component.literal("§7Redstone: §fOFF"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY_PREDICTOR_DATA;
        }

        // Helper: converts raw seconds into a human-readable string like "1h 23m 45s"
        private static String formatTime(double totalSeconds) {
            if (totalSeconds < 60) {
                return String.format("%.1fs", totalSeconds);
            } else if (totalSeconds < 3600) {
                long m = (long) (totalSeconds / 60);
                long s = (long) (totalSeconds % 60);
                return String.format("%dm %02ds", m, s);
            } else {
                long h = (long) (totalSeconds / 3600);
                long m = (long) ((totalSeconds % 3600) / 60);
                long s = (long) (totalSeconds % 60);
                return String.format("%dh %02dm %02ds", h, m, s);
            }
        }
    }
}
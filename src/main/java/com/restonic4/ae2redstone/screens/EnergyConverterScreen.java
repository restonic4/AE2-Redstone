package com.restonic4.ae2redstone.screens;

import com.restonic4.ae2redstone.AE2Redstone;
import com.restonic4.ae2redstone.ModMessages;
import com.restonic4.ae2redstone.block.energy_converter.EnergyConverterBlockEntity;
import com.restonic4.ae2redstone.compat.EnergyCompat;
import com.restonic4.ae2redstone.compat.IEnergyIntegration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class EnergyConverterScreen extends Screen {

    private final EnergyConverterBlockEntity blockEntity;
    private EditBox transferInput;

    public EnergyConverterScreen(EnergyConverterBlockEntity blockEntity) {
        super(Component.literal("Energy Converter Settings"));
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.transferInput = new EditBox(
                this.font, cx - 50, cy - 30, 100, 20,
                Component.literal("AE/t transfer cap")
        );
        this.transferInput.setValue(String.valueOf(blockEntity.getMaxTransferPerTick()));
        this.addRenderableWidget(this.transferInput);

        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), btn -> saveAndClose())
                .bounds(cx - 50, cy + 5, 100, 20).build());
    }

    private void saveAndClose() {
        long value;
        try {
            value = Long.parseLong(this.transferInput.getValue());
            if (value < 1) value = 1;
        } catch (NumberFormatException e) {
            value = blockEntity.getMaxTransferPerTick();
        }

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockEntity.getBlockPos());
        buf.writeLong(value);
        ClientPlayNetworking.send(ModMessages.UPDATE_CONVERTER_SETTINGS, buf);
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        super.render(g, mx, my, partial);

        int cx = this.width / 2;
        int cy = this.height / 2;

        g.drawCenteredString(this.font, this.title, cx, cy - 70, 0xFFFFFF);
        g.drawString(this.font, "Max Transfer (AE/t):", cx - 50, cy - 45, 0xA0A0A0);

        // --- SMART UI DISPLAY ---
        String activeMod = blockEntity.getActiveModName();
        // Null check included just to be safe during synchronization
        boolean isConnected = activeMod != null && !activeMod.isEmpty();

        String targetMod = isConnected ? activeMod : "Not connected / Idle";
        int modColor = isConnected ? 0x55FF55 : 0xFF5555;

        g.drawCenteredString(this.font, "Output: " + targetMod, cx, cy + 35, modColor);

        // If we are actively connected, find the ratio for that specific mod and display it
        if (isConnected) {
            for (IEnergyIntegration integration : EnergyCompat.getAll()) {
                if (integration.getModName().equals(activeMod)) {
                    String ratio = String.format("Ratio: 1 AE = %.1f %s energy",
                            integration.getConversionRatio(), integration.getModName());
                    g.drawCenteredString(this.font, ratio, cx, cy + 48, 0x888888);
                    break;
                }
            }
        }
        // ------------------------

        // Dev debug info
        if (AE2Redstone.IS_DEV) {
            int dy = cy + 65;
            g.drawString(this.font,
                    String.format("Stored: %.0f / %.0f AE",
                            blockEntity.getStoredPower(), blockEntity.getNetworkCapacity()),
                    cx - 100, dy, 0x55FF55);
            g.drawString(this.font,
                    String.format("Last transfer: %d AE", blockEntity.getLastTransferred()),
                    cx - 100, dy + 12, 0x55FF55);
            g.drawString(this.font,
                    "Active: " + (blockEntity.isActive() ? "YES" : "NO"),
                    cx - 100, dy + 24, blockEntity.isActive() ? 0xFF5555 : 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
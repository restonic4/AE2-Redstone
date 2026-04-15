package com.restonic4.ae2redstone;

import com.restonic4.ae2redstone.block.EnergyPredictorBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class EnergyPredictorScreen extends Screen {

    private final EnergyPredictorBlockEntity blockEntity;
    private boolean triggerWhenLessThan;

    private EditBox timeInput;
    private Button toggleModeButton;

    public EnergyPredictorScreen(EnergyPredictorBlockEntity blockEntity) {
        super(Component.literal("Energy Predictor Settings"));
        this.blockEntity = blockEntity;
        this.triggerWhenLessThan = blockEntity.isTriggerWhenLessThan();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        long currentSeconds = blockEntity.getTargetTimeTicks() / 20;

        this.timeInput = new EditBox(this.font, centerX - 50, centerY - 50, 100, 20, Component.literal("Time in seconds"));
        this.timeInput.setValue(String.valueOf(currentSeconds));
        this.addRenderableWidget(this.timeInput);

        this.toggleModeButton = Button.builder(getModeText(), button -> {
            this.triggerWhenLessThan = !this.triggerWhenLessThan;
            button.setMessage(getModeText());
        }).bounds(centerX - 75, centerY - 20, 150, 20).build();
        this.addRenderableWidget(this.toggleModeButton);

        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
            saveAndClose();
        }).bounds(centerX - 50, centerY + 10, 100, 20).build());
    }

    private Component getModeText() {
        return this.triggerWhenLessThan
                ? Component.literal("Emit when Time < Target")
                : Component.literal("Emit when Time >= Target");
    }

    private void saveAndClose() {
        long newSeconds;
        try {
            newSeconds = Long.parseLong(this.timeInput.getValue());
            if (newSeconds < 0) newSeconds = 0;
        } catch (NumberFormatException e) {
            newSeconds = blockEntity.getTargetTimeTicks() / 20;
        }

        long newTicks = newSeconds * 20;

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockEntity.getBlockPos());
        buf.writeLong(newTicks);
        buf.writeBoolean(triggerWhenLessThan);

        ClientPlayNetworking.send(ModMessages.UPDATE_PREDICTOR_SETTINGS, buf);
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 80, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Target Time (Seconds):", centerX - 50, centerY - 65, 0xA0A0A0);

        // --- Render Debug Information ONLY in Dev Environment ---
        if (AE2Redstone.IS_DEV) {
            int debugY = centerY + 40;
            int debugColor = 0x55FF55; // Light green

            String storedStr = String.format("Stored Power: %.2f AE", blockEntity.getDebugStored());
            String genStr = String.format("Generation: %.2f AE/t", blockEntity.getDebugGen());
            String useStr = String.format("Usage: %.2f AE/t", blockEntity.getDebugUsage());
            String netStr = String.format("Net Rate: %.2f AE/t", blockEntity.getDebugNet());

            String timeStr;
            if (blockEntity.getDebugTicksToEmpty() < 0) {
                timeStr = "Time to Empty: INFINITE (Gaining Power)";
            } else {
                timeStr = String.format("Time to Empty: %.1f sec", blockEntity.getDebugTicksToEmpty() / 20.0);
            }

            String emitStr = "Redstone Emitting: " + (blockEntity.isEmitting() ? "YES" : "NO");

            guiGraphics.drawString(this.font, storedStr, centerX - 100, debugY, debugColor);
            guiGraphics.drawString(this.font, genStr, centerX - 100, debugY + 10, debugColor);
            guiGraphics.drawString(this.font, useStr, centerX - 100, debugY + 20, debugColor);
            guiGraphics.drawString(this.font, netStr, centerX - 100, debugY + 30, debugColor);
            guiGraphics.drawString(this.font, timeStr, centerX - 100, debugY + 40, debugColor);

            int emitColor = blockEntity.isEmitting() ? 0xFF5555 : 0xAAAAAA; // Red if yes, gray if no
            guiGraphics.drawString(this.font, emitStr, centerX - 100, debugY + 55, emitColor);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
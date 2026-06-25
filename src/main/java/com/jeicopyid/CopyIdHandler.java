package com.jeicopyid;

import mezz.jei.api.IIngredientListOverlay;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

@Mod.EventBusSubscriber(modid = Jeicopyid.MODID, value = Side.CLIENT)
public class CopyIdHandler {

    @SubscribeEvent
    public static void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }

        if (Keyboard.getEventKey() != Keyboard.KEY_C) {
            return;
        }

        if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen == null) {
            return;
        }

        IJeiRuntime runtime = JeiCopyIdPlugin.getJeiRuntime();
        if (runtime == null) {
            return;
        }

        IIngredientListOverlay overlay = runtime.getIngredientListOverlay();
        if (overlay != null && overlay.hasKeyboardFocus()) {
            return;
        }

        Object ingredient = getIngredientUnderMouse(runtime);
        if (ingredient == null) {
            return;
        }

        String id = formatIngredientId(ingredient);
        if (id == null || id.isEmpty()) {
            return;
        }

        copyToClipboard(id);
        mc.player.sendStatusMessage(new TextComponentString("Copied: " + id), true);
        event.setCanceled(true);
    }

    private static Object getIngredientUnderMouse(IJeiRuntime runtime) {
        IIngredientListOverlay overlay = runtime.getIngredientListOverlay();
        if (overlay != null) {
            Object ingredient = overlay.getIngredientUnderMouse();
            if (ingredient != null) {
                return ingredient;
            }
        }

        RecipesGui recipesGui = (RecipesGui) runtime.getRecipesGui();
        if (recipesGui.isOpen()) {
            int[] mouse = getMousePosition();
            IClickedIngredient<?> clicked = recipesGui.getIngredientUnderMouse(mouse[0], mouse[1]);
            if (clicked != null) {
                return clicked.getValue();
            }
        }

        return null;
    }

    private static int[] getMousePosition() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaled = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
        int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;
        return new int[]{mouseX, mouseY};
    }

    private static String formatIngredientId(Object ingredient) {
        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            if (stack.isEmpty()) {
                return null;
            }

            ResourceLocation registryName = stack.getItem().getRegistryName();
            if (registryName == null) {
                return null;
            }

            int meta = stack.getMetadata();
            if (meta != 0) {
                return registryName + ":" + meta;
            }
            return registryName.toString();
        }

        if (ingredient instanceof net.minecraftforge.fluids.FluidStack) {
            net.minecraftforge.fluids.FluidStack fluidStack = (net.minecraftforge.fluids.FluidStack) ingredient;
            return fluidStack.getFluid().getName();
        }

        return ingredient.toString();
    }

    private static void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (HeadlessException e) {
            Jeicopyid.LOGGER.warn("Clipboard is not available", e);
        }
    }
}
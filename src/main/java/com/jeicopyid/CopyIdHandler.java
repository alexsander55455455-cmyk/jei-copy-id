package com.jeicopyid;

import mezz.jei.api.IIngredientListOverlay;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.inventory.Slot;
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
import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = Jeicopyid.MODID, value = Side.CLIENT)
public class CopyIdHandler {

    private static final Field CREATIVE_SEARCH_FIELD;
    private static final Field GUI_LEFT_FIELD;
    private static final Field GUI_TOP_FIELD;

    static {
        try {
            CREATIVE_SEARCH_FIELD = GuiContainerCreative.class.getDeclaredField("searchField");
            CREATIVE_SEARCH_FIELD.setAccessible(true);
            GUI_LEFT_FIELD = GuiContainer.class.getDeclaredField("guiLeft");
            GUI_LEFT_FIELD.setAccessible(true);
            GUI_TOP_FIELD = GuiContainer.class.getDeclaredField("guiTop");
            GUI_TOP_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not find required GUI fields", e);
        }
    }

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

        Object ingredient = getTargetUnderMouse(mc);
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

    private static Object getTargetUnderMouse(Minecraft mc) {
        int[] mouse = getMousePosition();

        if (mc.currentScreen instanceof GuiContainerCreative) {
            GuiContainerCreative creative = (GuiContainerCreative) mc.currentScreen;
            if (!isCreativeSearchFocused(creative)) {
                ItemStack stack = getHoveredItemStack(creative, mouse[0], mouse[1]);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        }

        IJeiRuntime runtime = JeiCopyIdPlugin.getJeiRuntime();
        if (runtime == null) {
            return null;
        }

        IIngredientListOverlay overlay = runtime.getIngredientListOverlay();
        if (overlay != null && overlay.hasKeyboardFocus()) {
            return null;
        }

        if (overlay != null) {
            Object ingredient = overlay.getIngredientUnderMouse();
            if (ingredient != null) {
                return ingredient;
            }
        }

        RecipesGui recipesGui = (RecipesGui) runtime.getRecipesGui();
        if (recipesGui.isOpen()) {
            IClickedIngredient<?> clicked = recipesGui.getIngredientUnderMouse(mouse[0], mouse[1]);
            if (clicked != null) {
                return clicked.getValue();
            }
        }

        return null;
    }

    private static boolean isCreativeSearchFocused(GuiContainerCreative gui) {
        try {
            GuiTextField searchField = (GuiTextField) CREATIVE_SEARCH_FIELD.get(gui);
            return searchField != null && searchField.isFocused();
        } catch (IllegalAccessException e) {
            Jeicopyid.LOGGER.warn("Could not read creative search field focus", e);
            return false;
        }
    }

    private static ItemStack getHoveredItemStack(GuiContainer gui, int mouseX, int mouseY) {
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (isPointInSlot(gui, slot, mouseX, mouseY) && slot.getHasStack()) {
                return slot.getStack();
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isPointInSlot(GuiContainer gui, Slot slot, int mouseX, int mouseY) {
        try {
            int guiLeft = GUI_LEFT_FIELD.getInt(gui);
            int guiTop = GUI_TOP_FIELD.getInt(gui);
            int localX = mouseX - guiLeft;
            int localY = mouseY - guiTop;
            return localX >= slot.xPos - 1
                    && localX < slot.xPos + 17
                    && localY >= slot.yPos - 1
                    && localY < slot.yPos + 17;
        } catch (IllegalAccessException e) {
            Jeicopyid.LOGGER.warn("Could not read GUI position", e);
            return false;
        }
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
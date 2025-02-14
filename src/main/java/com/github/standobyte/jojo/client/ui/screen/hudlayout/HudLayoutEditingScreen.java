package com.github.standobyte.jojo.client.ui.screen.hudlayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.client.InputHandler;
import com.github.standobyte.jojo.client.InputHandler.MouseButton;
import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.client.ui.screen.widgets.CustomButton;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromclient.ClActionsLayoutPacket;
import com.github.standobyte.jojo.power.IPower;
import com.github.standobyte.jojo.power.IPower.PowerClassification;
import com.github.standobyte.jojo.power.layout.ActionHotbarLayout;
import com.github.standobyte.jojo.power.layout.ActionHotbarLayout.ActionSwitch;
import com.github.standobyte.jojo.power.layout.ActionsLayout;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

/* 
 * FIXME !!!! (layout editing) close the window when the player's power being changed/replaced
 */
// TODO saving layout variants
@SuppressWarnings("deprecation")
public class HudLayoutEditingScreen extends Screen {
    private static final ResourceLocation WINDOW = new ResourceLocation(JojoMod.MOD_ID, "textures/gui/layout_editing.png");
    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 124;
    
    private static PowerClassification selectedTab = null;
    private IPower<?, ?> selectedPower;
    private List<IPower<?, ?>> powersPresent = new ArrayList<>();
    
    private Optional<ActionData<?>> draggedAction = Optional.empty();
    private Optional<ActionData<?>> hoveredAction = Optional.empty();
    private boolean isQuickActionSlotHovered;
    private VisibilityButton quickAccessHudVisibilityButton;
    
    private Collection<IPower<?, ?>> editedLayouts = new ArrayList<>();

    public HudLayoutEditingScreen() {
        super(new TranslationTextComponent("jojo.screen.edit_hud_layout"));
    }
    
    @Override
    protected void init() {
        addButton(new CustomButton(getWindowX() + WINDOW_WIDTH - 30, getWindowY() + WINDOW_HEIGHT - 30, 24, 24, 
                button -> {
                    selectedPower.getActionsHudLayout().resetLayout();
                    markLayoutEdited(selectedPower);
                }, 
                (button, matrixStack, x, y) -> {
                    renderTooltip(matrixStack, new TranslationTextComponent("jojo.screen.edit_hud_layout.reset"), x, y);
                }) {

            @Override
            protected void renderCustomButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.getTextureManager().bind(WINDOW);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                blit(matrixStack, x, y, 0, 184 + getYImage(isHovered()) * 24, width, height);
            }
        });
        
        addButton(quickAccessHudVisibilityButton = new VisibilityButton(getWindowX() + 30, getWindowY() + WINDOW_HEIGHT - 28,
                button -> {
                    ActionsLayout<?> layout = selectedPower.getActionsHudLayout();
                    boolean newValue = !layout.isMmbActionHudVisible();
                    layout.setMmbActionHudVisibility(newValue);
                    quickAccessHudVisibilityButton.setVisibilityState(newValue);
                    markLayoutEdited(selectedPower);
                }));
        
        if (selectedTab != null) {
            IPower.getPowerOptional(minecraft.player, selectedTab).ifPresent(power -> {
                if (!power.hasPower()) {
                    selectedTab = null;
                }
            });
        }
        
        powersPresent.clear();
        for (PowerClassification powerClassification : PowerClassification.values()) {
            IPower.getPowerOptional(minecraft.player, powerClassification).ifPresent(power -> {
                if (power.hasPower()) {
                    powersPresent.add(power);
                    if (selectedTab == null || powerClassification == ActionsOverlayGui.getInstance().getCurrentMode()) {
                        selectTab(power);
                    }
                }
            });
        }
        
        if (selectedTab != null && selectedPower == null) {
            selectTab(IPower.getPlayerPower(minecraft.player, selectedTab));
        }
    }
    
    public boolean works() {
        return selectedPower != null && selectedPower.hasPower();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        if (!works()) return;
        renderBackground(matrixStack, 0);
        hoveredAction = getActionAt(mouseX, mouseY);
        isQuickActionSlotHovered = isQuickAccessActionSlotAt(mouseX, mouseY);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        renderTabButtons(matrixStack, false);
        renderWindow(matrixStack);
        renderTabButtons(matrixStack, true);
        renderHint(matrixStack);
        renderSlots(matrixStack, mouseX, mouseY);
        renderDragged(matrixStack, mouseX, mouseY);
        renderToolTips(matrixStack, mouseX, mouseY);
        buttons.forEach(button -> button.render(matrixStack, mouseX, mouseY, partialTick));
        drawText(matrixStack);
    }
    

    private void renderWindow(MatrixStack matrixStack) {
        RenderSystem.enableBlend();
        minecraft.getTextureManager().bind(WINDOW);
        blit(matrixStack, getWindowX(), getWindowY(), 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        blit(matrixStack, getWindowX() + 9, getWindowY() + 3, 232, 3, 9, 16);
        blit(matrixStack, getWindowX() + 9, getWindowY() + 39, 232, 39, 9, 16);
        blit(matrixStack, getWindowX() + 9, getWindowY() + 75, 232, 75, 9, 16);
        RenderSystem.disableBlend();
    }
    

    private void renderTabButtons(MatrixStack matrixStack, boolean renderSelectedTabButton) {
        for (int i = 0; i < powersPresent.size(); i++) {
            boolean isTabSelected = isTabSelected(powersPresent.get(i));
            if (isTabSelected ^ renderSelectedTabButton) continue;
            int textureX = i == 0 ? 200 : 228;
            int textureY = isTabSelected ? 224 : 192;
            minecraft.getTextureManager().bind(WINDOW);
            int[] xy = getTabButtonCoords(i);
            blit(matrixStack, xy[0], xy[1], textureX, textureY, 28, 32);

            RenderSystem.enableBlend();
            minecraft.getTextureManager().bind(powersPresent.get(i).clGetPowerTypeIcon());
            blit(matrixStack, xy[0] + 6, xy[1] + 10, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
            if (renderSelectedTabButton) break;
        }
    }
    
    private int[] getTabButtonCoords(int tabIndex) {
        int x = getWindowX() + tabIndex * 29;
        int y = getWindowY() - 28;
        return new int[] {x, y};
    }
    
    private boolean isTabSelected(IPower<?, ?> power) {
        return power == selectedPower;
    }

    private static final int HOTBARS_X = 8;
    private static final int ATTACKS_HOTBAR_Y = 20;
    private static final int ABILITIES_HOTBAR_Y = 56;
    private static final int QUICK_ACCESS_Y = 92;
    private <P extends IPower<P, ?>> void renderSlots(MatrixStack matrixStack, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        P iSuckAtThis = (P) selectedPower;
        int x = HOTBARS_X + getWindowX();
        renderHotbar(iSuckAtThis, ActionsLayout.Hotbar.LEFT_CLICK, matrixStack, x, ATTACKS_HOTBAR_Y + getWindowY(), mouseX, mouseY);
        renderHotbar(iSuckAtThis, ActionsLayout.Hotbar.RIGHT_CLICK, matrixStack, x, ABILITIES_HOTBAR_Y + getWindowY(), mouseX, mouseY);
        renderActionSlot(matrixStack, x, QUICK_ACCESS_Y + getWindowY(), mouseX, mouseY, 
                iSuckAtThis, iSuckAtThis.getActionsHudLayout().getVisibleQuickAccessAction(shift, iSuckAtThis, ActionTarget.EMPTY), true, 
                draggedAction.isPresent(), 
                isQuickActionSlotHovered && draggedAction.isPresent(), 
                true);
        RenderSystem.disableBlend();
    }
    
    private <P extends IPower<P, ?>> void renderHotbar(P power, ActionsLayout.Hotbar hotbar,
            MatrixStack matrixStack, int hotbarX, int hotbarY,
            int mouseX, int mouseY) {
        int i = 0;
        for (ActionSwitch<P> actionSwitch : power.getActionsHudLayout().getHotbar(hotbar).getLayoutView()) {
            renderActionSlot(matrixStack, hotbarX + i * 18, hotbarY, mouseX, mouseY, 
                    power, actionSwitch, 
                    draggedAction.map(dragged -> dragged.hotbar == hotbar).orElse(false), 
                    hoveredAction.map(slot -> slot.actionSwitch == actionSwitch).orElse(false), 
                    draggedAction.map(dragged -> dragged.actionSwitch != actionSwitch).orElse(true));
            i++;
        }
    }
    
    private <P extends IPower<P, ?>> void renderDragged(MatrixStack matrixStack, int mouseX, int mouseY) {
        draggedAction.ifPresent(dragged -> {
            RenderSystem.translatef(0.0F, 0.0F, 32.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            this.setBlitOffset(200);
            ActionSwitch<P> actionSwitch = (ActionSwitch<P>) dragged.actionSwitch;
            renderActionIcon(matrixStack, mouseX - 8, mouseY - 8, 
                    actionSwitch.getAction(), actionSwitch.isEnabled(), (P) selectedPower);
            this.setBlitOffset(0);
            RenderSystem.disableBlend();
            RenderSystem.translatef(0.0F, 0.0F, -32.0F);
        });
    }
    
    private <P extends IPower<P, ?>> void renderActionSlot(MatrixStack matrixStack, 
            int x, int y, int mouseX, int mouseY, 
            P power, ActionSwitch<P> actionSwitch, 
            boolean fitsForDragged, boolean isHoveredOver, boolean renderActionIcon) {
        renderActionSlot(matrixStack, 
                x, y, mouseX, mouseY, 
                power, actionSwitch.getAction(), actionSwitch.isEnabled(), 
                fitsForDragged, isHoveredOver, renderActionIcon);
    }
    
    private <P extends IPower<P, ?>> void renderActionSlot(MatrixStack matrixStack, 
            int x, int y, int mouseX, int mouseY, 
            P power, Action<P> action, boolean isEnabled, 
            boolean fitsForDragged, boolean isHoveredOver, boolean renderActionIcon) {
        minecraft.getTextureManager().bind(WINDOW);
        int texX = isHoveredOver ? 82 : 64;
        if (fitsForDragged) {
            texX += 36;
        }
        blit(matrixStack, x, y, texX, 238, 18, 18);

        if (renderActionIcon) {
            renderActionIcon(matrixStack, x + 1, y + 1, action, isEnabled, power);
        }
    }
    
    private <P extends IPower<P, ?>> void renderActionIcon(MatrixStack matrixStack, int x, int y, Action<P> action, boolean isEnabled, P power) {
        if (action != null) {
            Action<P> actionResolved = power.getActionsHudLayout().resolveVisibleActionInSlot(action, shift, power, ActionTarget.EMPTY);
            if (actionResolved != null) action = actionResolved;
            if (shift) {
                action = action.getShiftVariationIfPresent();
            }
            
            ResourceLocation icon = action.getIconTexture(power);
            minecraft.getTextureManager().bind(icon);
            
            boolean isUnlocked = action.isUnlocked(power);
            float alpha = isEnabled ? isUnlocked ? 1.0F : 0.6F : 0.2F;
            float color = isEnabled && isUnlocked ? 1.0F : 0.0F;
            
            RenderSystem.color4f(color, color, color, alpha);
            blit(matrixStack, x, y, 0, 0, 16, 16, 16, 16);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
    
    private Optional<ActionData<?>> getActionAt(int mouseX, int mouseY) {
        return getHotbarAt(mouseY - getWindowY())
                .flatMap(hotbar -> getSlotInHotbar(hotbar, mouseX - getWindowX())
                .flatMap(action -> Optional.of(new ActionData<>(action, hotbar))));
    }
    
    private Optional<ActionsLayout.Hotbar> getHotbarAt(int mouseY) {
        if (mouseY >= ATTACKS_HOTBAR_Y && mouseY < ATTACKS_HOTBAR_Y + 18) return Optional.of(ActionsLayout.Hotbar.LEFT_CLICK);
        if (mouseY >= ABILITIES_HOTBAR_Y && mouseY < ABILITIES_HOTBAR_Y + 18) return Optional.of(ActionsLayout.Hotbar.RIGHT_CLICK);
        return Optional.empty();
    }
    
    private Optional<ActionSwitch<?>> getSlotInHotbar(ActionsLayout.Hotbar hotbar, int mouseX) {
        mouseX -= HOTBARS_X;
        if (mouseX < 0) return Optional.empty();
        List<? extends ActionSwitch<?>> layout = selectedPower.getActionsHudLayout().getHotbar(hotbar).getLayoutView();
        int slot = mouseX / 18;
        return slot < layout.size() ? Optional.of(layout.get(slot)) : Optional.empty();
    }
    
    private boolean isQuickAccessActionSlotAt(int mouseX, int mouseY) {
        mouseX -= getWindowX();
        mouseY -= getWindowY();
        return mouseX >= HOTBARS_X && mouseX < HOTBARS_X + 18 && mouseY >= QUICK_ACCESS_Y && mouseY < QUICK_ACCESS_Y + 18;
    }
    
    private void renderHint(MatrixStack matrixStack) {
        minecraft.getTextureManager().bind(WINDOW);
        int x = getWindowX() + WINDOW_WIDTH - 48;
        int y = getWindowY() + WINDOW_HEIGHT - 17;
        blit(matrixStack, x, y, 32, 245, 11, 11);
    }
    
    private final List<ITextComponent> hintTooltip = ImmutableList.of(
            new TranslationTextComponent("jojo.screen.edit_hud_layout.hint.lmb"), 
            new TranslationTextComponent("jojo.screen.edit_hud_layout.hint.rmb"), 
            new TranslationTextComponent("jojo.screen.edit_hud_layout.hint.mmb"));
    private void renderToolTips(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (draggedAction.isPresent()) return;
        int tab = getTabButtonAt(mouseX, mouseY);
        if (tab >= 0 && tab < powersPresent.size()) {
            renderTooltip(matrixStack, powersPresent.get(tab).getName(), mouseX, mouseY);
        }
        else {
            renderActionNameTooltip(matrixStack, mouseX, mouseY);
        }
        
        int hintX = getWindowX() + WINDOW_WIDTH - 48;
        int hintY = getWindowY() + WINDOW_HEIGHT - 17;
        if (mouseX >= hintX && mouseX < hintX + 11 && mouseY >= hintY && mouseY < hintY + 11) {
            renderComponentTooltip(matrixStack, hintTooltip, mouseX, mouseY);
        }
    }
    
    private <P extends IPower<P, ?>> void renderActionNameTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
        P power = (P) selectedPower;
        hoveredAction.ifPresent(slot -> {
            renderActionName(matrixStack, power, (Action<P>) slot.actionSwitch.getAction(), mouseX, mouseY, slot.actionSwitch.isEnabled());
        });
        if (isQuickActionSlotHovered) {
            renderActionName(matrixStack, power, power.getActionsHudLayout().getVisibleQuickAccessAction(shift, power, ActionTarget.EMPTY), mouseX, mouseY, true);
        }
    }
    
    private <P extends IPower<P, ?>> void renderActionName(MatrixStack matrixStack, P power, Action<P> action, int mouseX, int mouseY, boolean isEnabled) {
        if (action == null) return;
        Action<P> actionReplacing = power.getActionsHudLayout().resolveVisibleActionInSlot(action, shift, power, ActionTarget.EMPTY);
        if (actionReplacing != null) {
            action = actionReplacing;
        }
        IFormattableTextComponent name;
        
        if (action.isUnlocked(power)) {
            name = action.getTranslatedName(power, action.getTranslationKey(power, ActionTarget.EMPTY));
        }
        
        else {
            name = action.getNameLocked(power).withStyle(TextFormatting.GRAY, TextFormatting.ITALIC);
        }
        
        if (!isEnabled) {
            name.withStyle(TextFormatting.DARK_GRAY);
        }
        renderTooltip(matrixStack, name, mouseX, mouseY);
    }
    
    private void drawText(MatrixStack matrixStack) {
        int x = getWindowX();
        int y = getWindowY();
        minecraft.font.draw(matrixStack, new TranslationTextComponent("jojo.screen.edit_hud_layout.attacks"), 
                x + 22, y + 8, 0x404040);
        minecraft.font.draw(matrixStack, new TranslationTextComponent("jojo.screen.edit_hud_layout.abilities"), 
                x + 22, y + 44, 0x404040);
        minecraft.font.draw(matrixStack, new TranslationTextComponent("jojo.screen.edit_hud_layout.mmb"), 
                x + 22, y + 80, 0x404040);
    }
    
    private int getTabButtonAt(int mouseX, int mouseY) {
        mouseX -= getWindowX();
        mouseY -= getWindowY();
        if (mouseY > -28 && mouseY < 0 && mouseX >= 0) {
            int tab = mouseX / 29;
            return tab;
        }
        return -1;
    }
    
    private int getWindowX() { return (width - WINDOW_WIDTH) / 2; }
    private int getWindowY() { return (height - WINDOW_HEIGHT) / 2; }

    @Override
    public boolean mouseClicked(double mouseXd, double mouseYd, int mouseButton) {
        MouseButton button = MouseButton.getButtonFromId(mouseButton);
        if (button == null) return false;
        int mouseX = (int) mouseXd;
        int mouseY = (int) mouseYd;
        Optional<ActionData<?>> clickedSlot = getActionAt(mouseX, mouseY);
        
        if (draggedAction.isPresent()) {
            clickedSlot.ifPresent(clicked -> {
                if (draggedAction.get().hotbar == clicked.hotbar) {
                    selectedPower.getActionsHudLayout().getHotbar(clicked.hotbar).swapActionsOrder(
                            draggedAction.get().actionSwitch.getAction(), 
                            clicked.actionSwitch.getAction());
                    markLayoutEdited(selectedPower);
                }
            });
            if (isQuickActionSlotHovered) {
                setQuickAccess(selectedPower, draggedAction.get().actionSwitch.getAction());
            }
            draggedAction = Optional.empty();
            return true;
        }
        
        else {
            int tab = getTabButtonAt(mouseX, mouseY);
            if (tab >= 0 && tab < powersPresent.size()) {
                selectTab(powersPresent.get(tab));
                return true;
            }
            if (clickedSlot.isPresent()) {
                ActionsLayout.Hotbar hotbar = clickedSlot.get().hotbar;
                switch (button) {
                case LEFT:
                    draggedAction = clickedSlot;
                    return true;
                case RIGHT:
                    ActionSwitch<?> slot = clickedSlot.get().actionSwitch;
                    selectedPower.getActionsHudLayout().getHotbar(hotbar).setIsEnabled(slot.getAction(), !slot.isEnabled());
                    markLayoutEdited(selectedPower);
                    
                    if (slot.isEnabled() && selectedPower == ActionsOverlayGui.getInstance().getCurrentPower()
                            && isActionVisible(slot.getAction(), selectedPower)) {
                        int slotIndex = selectedPower.getActionsHudLayout().getHotbar(hotbar).getEnabled().indexOf(slot.getAction());
                        if (slotIndex >= 0) {
                            ActionsOverlayGui.getInstance().selectAction(hotbar, slotIndex);
                        }
                    }
                    return true;
                case MIDDLE:
                    setQuickAccess(selectedPower, clickedSlot.get().actionSwitch.getAction());
                    return true;
                default:
                    return false;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    private void selectTab(IPower<?, ?> power) {
        if (power != null && power.hasPower()) {
            selectedPower = power;
            selectedTab = power.getPowerClassification();
            quickAccessHudVisibilityButton.setVisibilityState(power.getActionsHudLayout().isMmbActionHudVisible());
        }
    }
    
    private <P extends IPower<P, ?>> boolean isActionVisible(Action<P> action, IPower<?, ?> power) {
        return action.getVisibleAction((P) power, ActionTarget.EMPTY) != null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (hoveredAction.isPresent()) {
            ActionsLayout.Hotbar hotbar = hoveredAction.get().hotbar;
            if (hotbar != null) {
                ActionsOverlayGui.getInstance().scrollAction(hotbar, scroll > 0);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }
    
    private boolean shift = false;
    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (InputHandler.renderShiftVarInScreenUI(minecraft, key, scanCode)) {
            shift = true;
        }
        if (InputHandler.getInstance().editHotbars.matches(key, scanCode)) {
            onClose();
            return true;
        } 
        else {
            int numKey = getNumKey(key, scanCode);
            if (numKey > -1) {
                Optional<ActionData<?>> toMove = draggedAction;
                if (!toMove.isPresent()) toMove = hoveredAction;
                if (toMove.map(action -> {
                    ActionHotbarLayout<?> actionsHotbar = selectedPower.getActionsHudLayout().getHotbar(action.hotbar);
                    if (numKey < actionsHotbar.getLayoutView().size()) {
                        actionsHotbar.swapActionsOrder(
                                action.actionSwitch.getAction(), 
                                actionsHotbar.getLayoutView().get(numKey).getAction());
                        markLayoutEdited(selectedPower);
                        return true;
                    }
                    return false;
                }).orElse(false)) {
                    if (draggedAction.isPresent()) {
                        draggedAction = Optional.empty();
                    }
                    return true;
                }
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    
    private int getNumKey(int key, int scanCode) {
        for (int i = 0; i < 9; ++i) {
            if (minecraft.options.keyHotbarSlots[i].isActiveAndMatches(InputMappings.getKey(key, scanCode))) {
                return i;
            }
        }
        return -1;
    }
    
    private void markLayoutEdited(IPower<?, ?> power) {
        editedLayouts.add(power);
    }

    private <P extends IPower<P, ?>> void setQuickAccess(IPower<?, ?> power, Action<P> action) {
        ((P) power).getActionsHudLayout().setQuickAccessAction(action);
        markLayoutEdited(power);
    }
    
    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (InputHandler.renderShiftVarInScreenUI(minecraft, key, scanCode)) {
            shift = false;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        editedLayouts.forEach(power -> {
            PacketManager.sendToServer(new ClActionsLayoutPacket(
                    power.getPowerClassification(), power.getType(), power.getActionsHudLayout()));
        });
        ActionsOverlayGui.getInstance().revealActionNames();
    }
    
    private static class ActionData<P extends IPower<P, ?>> {
        private final ActionSwitch<P> actionSwitch;
        private final ActionsLayout.Hotbar hotbar;
        
        private ActionData(ActionSwitch<P> actionSwitch, ActionsLayout.Hotbar hotbar) {
            this.actionSwitch = actionSwitch;
            this.hotbar = hotbar;
        }
    }
}

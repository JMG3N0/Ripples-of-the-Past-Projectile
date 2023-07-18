package com.github.standobyte.jojo.client.ui.screen.hamon;

import java.util.List;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.resources.CustomResources;
import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class HamonIntroTabGui extends HamonTabGui {
    private final IFormattableTextComponent aboutName;
    private final List<IReorderingProcessor> aboutText;
    private final IFormattableTextComponent breathName;
    private final List<IReorderingProcessor> breathTextBar;
    private final List<IReorderingProcessor> breathTextEnergy;
    private final List<IReorderingProcessor> breathTextAbility;
    private final List<IReorderingProcessor> breathTextStability;
    private final List<IReorderingProcessor> breathTextStability2;
    private final List<IReorderingProcessor> statsTransitionText;
    private int y1;
    private int y2;
    private int y3;
    private int tickCount = 0;
    private int bar2RenderTime = -1;
    private int bar3RenderTime = -1;
    
    HamonIntroTabGui(Minecraft minecraft, HamonScreen screen, int index, String title) {
        super(minecraft, screen, index, title, -1, 1);
        int textWidth = HamonScreen.WINDOW_WIDTH - 30;
        aboutName = new TranslationTextComponent("hamon.intro.about.name");
        aboutText = minecraft.font.split(new TranslationTextComponent("hamon.intro.about.text"), textWidth);
        breathName = new TranslationTextComponent("hamon.intro.breath.name");
        breathTextBar = minecraft.font.split(new TranslationTextComponent("hamon.intro.breath.text1", 
                new TranslationTextComponent("hamon.intro.breath.text1.underlined").withStyle(TextFormatting.UNDERLINE)), textWidth);
        breathTextEnergy = minecraft.font.split(new TranslationTextComponent("hamon.intro.breath.text2", 
                new TranslationTextComponent("hamon.intro.breath.text2.underlined").withStyle(TextFormatting.UNDERLINE)), textWidth);
        breathTextAbility = minecraft.font.split(new TranslationTextComponent("hamon.intro.breath.text3", 
                new TranslationTextComponent("hamon.intro.breath.text3.underlined").withStyle(TextFormatting.UNDERLINE)), textWidth);
        breathTextStability = minecraft.font.split(new TranslationTextComponent("hamon.intro.breath.text4", 
                new TranslationTextComponent("hamon.intro.breath.text4.underlined").withStyle(TextFormatting.UNDERLINE)), textWidth);
        breathTextStability2 = minecraft.font.split(new TranslationTextComponent("hamon.intro.breath.text5"), textWidth);
        statsTransitionText = minecraft.font.split(new TranslationTextComponent("hamon.intro.stats_transition"), textWidth);
    }
    
    @Override
    protected void addButtons() {}
    
    @Override
    protected void drawText(MatrixStack matrixStack) {
        int textX = intScrollX + 5;
        int textY = intScrollY + 6;
        drawString(matrixStack, minecraft.font, aboutName, textX - 3, textY, 0xFFFFFF);
        
        textY += 2;
        for (int i = 0; i < aboutText.size(); i++) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, aboutText.get(i), (float) textX, (float) textY, 0xFFFFFF);
        }
        
        textY += 15;
        drawString(matrixStack, minecraft.font, breathName, textX - 3, textY, 0xFFFFFF);
        
        textY += 2;
        for (IReorderingProcessor line : breathTextBar) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }
        y1 = textY + 11;
        
        textY += 19;
        for (IReorderingProcessor line : breathTextEnergy) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }
        
        for (IReorderingProcessor line : breathTextAbility) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }
        y2 = textY + 36;
        
        textY += 44;
        for (IReorderingProcessor line : breathTextStability) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }
        y3 = textY + 12;
        
        textY += 14;
        for (IReorderingProcessor line : breathTextStability2) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }

        textY += 16;
        for (IReorderingProcessor line : statsTransitionText) {
            textY += minecraft.font.lineHeight;
            minecraft.font.draw(matrixStack, line, (float) textX, (float) textY, 0xFFFFFF);
        }
        
        maxY = textY + 15 - intScrollY;
    }
    
    @Override
    protected void drawActualContents(HamonScreen screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        minecraft.textureManager.bind(ActionsOverlayGui.OVERLAY_LOCATION);
        float ticks = tickCount + partialTick;
        int x = intScrollX + 5;
        
        // empty energy bar
        renderEnergyBar(matrixStack, x, y1, 1, 0);
        
        // charging energy bar
        boolean bar2InView = y2 > -7 && y2 < 199;
        if (bar2RenderTime < 0 && bar2InView) {
            bar2RenderTime = tickCount;
        }
        if (bar2RenderTime >= 0) {
            float barTicks = (ticks - bar2RenderTime) % 100;
            renderEnergyBar(matrixStack, x, y2, 1, MathHelper.clamp((barTicks - 20F) / 60F, 0F, 1F));
        }
        
        // energy bar with lower breath stability charging not as fast
        boolean bar3InView = y3 > -7 && y3 < 199;
        if (bar3RenderTime < 0 && bar3InView) {
            bar3RenderTime = tickCount;
        }
        if (bar3RenderTime >= 0) {
            float barTicks = (ticks - bar3RenderTime) % 750;
            float fillStab = 0.4F + 0.6F * barTicks / 720;
            float fillEnergy = MathHelper.clamp((barTicks - 20) / 60, 0, fillStab);
            renderEnergyBar(matrixStack, x, y3, fillStab, fillEnergy);
        }
        
        renderHamonBreathIcon(matrixStack, intScrollX + 98, y2 - 21);
        
        RenderSystem.disableBlend();
    }
    
    @SuppressWarnings("deprecation")
    private void renderEnergyBar(MatrixStack matrixStack, int x, int y, float fillStab, float fillEnergy) {
        float[] hamonRGB = ClientUtil.rgb(ModPowers.HAMON.get().getColor());
        blit(matrixStack, x, y, 0, 128, 202, 8);
        RenderSystem.color4f(hamonRGB[0], hamonRGB[1], hamonRGB[2], 0.4F);
        blit(matrixStack, x + 1, y + 1, 1, 161, (int) (200 * fillStab), 6);
        
        RenderSystem.color4f(hamonRGB[0], hamonRGB[1], hamonRGB[2], 1.0F);
        blit(matrixStack, x + 1, y + 1, 1, 161, (int) (200 * fillEnergy), 6);
        
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrixStack, x + 1, y + 1, 0, 145, 200, 6);
    }
    
    private void renderHamonBreathIcon(MatrixStack matrixStack, int x, int y) {
        minecraft.getTextureManager().bind(ActionsOverlayGui.OVERLAY_LOCATION);
        blit(matrixStack, x - 15, y - 1, 236, 128, 9, 16);
        minecraft.getTextureManager().bind(ActionsOverlayGui.WIDGETS_LOCATION);
        blit(matrixStack, x - 3, y - 3, 0, 0, 22, 22);
        TextureAtlasSprite textureAtlasSprite = CustomResources.getActionSprites().getSprite(ModHamonActions.HAMON_BREATH.get().getRegistryName());
        minecraft.getTextureManager().bind(textureAtlasSprite.atlas().location());
        blit(matrixStack, x, y, 0, 16, 16, textureAtlasSprite);
        minecraft.getTextureManager().bind(ActionsOverlayGui.WIDGETS_LOCATION);
        blit(matrixStack, x - 4, y - 4, 0, 22, 24, 22);
    }
    
    @Override
    void tick() {
        tickCount++;
    }

    @Override
    void drawIcon(MatrixStack matrixStack, int windowX, int windowY, ItemRenderer itemRenderer) {
        minecraft.getTextureManager().bind(ModPowers.HAMON.get().getIconTexture());
        blit(matrixStack, windowX - 32 + 13, windowY + getTabY() + 6, 0, 0, 16, 16, 16, 16);
    }
    
    
    
    @Override
    boolean mouseClicked(double mouseX, double mouseY, int mouseButton, boolean mouseInsideWindow) {
        return false;
    }
    
    @Override
    boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }
    
    @Override
    void updateTab() {}
}

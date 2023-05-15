package com.github.standobyte.jojo.client.render.entity.renderer.stand;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.client.render.entity.model.stand.HierophantGreenModel;
import com.github.standobyte.jojo.entity.stand.stands.HierophantGreenEntity;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class HierophantGreenRenderer extends StandEntityRenderer<HierophantGreenEntity, HierophantGreenModel> {

    public HierophantGreenRenderer(EntityRendererManager renderManager) {
        super(renderManager, new HierophantGreenModel(), new ResourceLocation(JojoMod.MOD_ID, "textures/entity/stand/hierophant_green.png"), 0);
    }
}
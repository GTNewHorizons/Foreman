package com.eldrinn.foreman.gui;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.drawable.UITexture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ForemanIcons {

    public static final UITexture DELETE = UITexture.fullImage("foreman", "textures/gui/icons/delete.png");
    public static final UITexture PLUS = UITexture.fullImage("foreman", "textures/gui/icons/plus.png");
    public static final UITexture SEARCH = UITexture.fullImage("foreman", "textures/gui/icons/search.png");

    public static final UITexture CHECK_ON = UITexture.builder()
        .location(ModularUI.ID, "gui/widgets/toggle_config")
        .imageSize(14, 28)
        .subAreaUV(0f, 0.5f, 1f, 1f)
        .build();

    private ForemanIcons() {}
}

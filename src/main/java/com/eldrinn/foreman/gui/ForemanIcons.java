package com.eldrinn.foreman.gui;

import com.cleanroommc.modularui.drawable.UITexture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ForemanIcons {

    public static final UITexture DELETE = UITexture.fullImage("foreman", "textures/gui/icons/delete.png");
    public static final UITexture PLUS = UITexture.fullImage("foreman", "textures/gui/icons/plus.png");
    public static final UITexture SEARCH = UITexture.fullImage("foreman", "textures/gui/icons/search.png");

    private ForemanIcons() {}
}

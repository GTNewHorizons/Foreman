package com.eldrinn.foreman.gui.widget;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** TextFieldWidget without right-click-to-clear. Search bar uses stock TextFieldWidget instead. */
@SideOnly(Side.CLIENT)
public class PlainTextField extends TextFieldWidget {

    @Override
    public @org.jetbrains.annotations.NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton == 1) {
            // skip clear — right-click is reserved for the search bar only
            return Interactable.Result.IGNORE;
        }
        return super.onMousePressed(mouseButton);
    }
}

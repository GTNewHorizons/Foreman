package com.eldrinn.foreman.proxy;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.eldrinn.foreman.gui.ForemanGui;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public static final KeyBinding KEY_OPEN_GUI = new KeyBinding(
        "key.foreman.open",
        Keyboard.KEY_Y,
        "key.categories.foreman");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientRegistry.registerKeyBinding(KEY_OPEN_GUI);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (KEY_OPEN_GUI.isPressed()) {
            ForemanGui.open();
        }
        ForemanGui.tick();
    }
}

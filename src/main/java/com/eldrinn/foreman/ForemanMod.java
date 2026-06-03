package com.eldrinn.foreman;

import com.eldrinn.foreman.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = ForemanMod.MODID, version = Tags.VERSION, name = "Foreman", acceptedMinecraftVersions = "[1.7.10]")
public class ForemanMod {

    public static final String MODID = "foreman";

    @SuppressWarnings("unused") // assigned by FML via @SidedProxy reflection
    @SidedProxy(
        clientSide = "com.eldrinn.foreman.proxy.ClientProxy",
        serverSide = "com.eldrinn.foreman.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}

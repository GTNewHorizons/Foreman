package com.eldrinn.foreman.proxy;

import com.eldrinn.foreman.command.ForemanCommand;
import com.eldrinn.foreman.event.PlayerLoginHandler;
import com.eldrinn.foreman.network.ForemanNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ForemanNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
        // PlayerLoggedInEvent fires on FML's bus, not MinecraftForge.EVENT_BUS
        FMLCommonHandler.instance().bus().register(new PlayerLoginHandler());
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ForemanCommand());
    }
}

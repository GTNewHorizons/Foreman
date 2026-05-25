package com.eldrinn.foreman.network;

import java.io.IOException;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.foreman.gui.ForemanGui;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class OpenGuiPacket implements IPacket {

    @Override
    public void encode(PacketBuffer buf) throws IOException {}

    @Override
    public void decode(PacketBuffer buf) throws IOException {}

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ForemanGui.open();
        return null;
    }
}

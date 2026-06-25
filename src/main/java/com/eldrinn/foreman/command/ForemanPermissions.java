package com.eldrinn.foreman.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.Loader;

/**
 * Per-subcommand permission gate for /foreman.
 *
 * When ServerUtilities is installed, each node is registered with its default level and checks delegate to its
 * PermissionAPI (so admins tune access via ranks). Without ServerUtilities, OP-default nodes require OP and ALL-default
 * nodes are open. Console is always allowed.
 */
public final class ForemanPermissions {

    private ForemanPermissions() {}

    private static final String SU = "serverutilities";

    /** Cached once: mod set doesn't change after load. */
    private static final boolean SU_PRESENT = Loader.isModLoaded(SU);

    public static final String LIST = "foreman.list";
    public static final String GUI = "foreman.gui";
    public static final String OPEN = "foreman.open";
    public static final String CREATE = "foreman.create";
    public static final String ASSIGN = "foreman.assign";
    public static final String UNASSIGN = "foreman.unassign";
    public static final String DONE = "foreman.done";
    public static final String RELOAD = "foreman.reload";
    public static final String IMPORT = "foreman.import";
    public static final String EXPORT = "foreman.export";

    /** Human-readable node descriptions shown in the ServerUtilities rank editor. */
    private static String describe(String node) {
        return "Foreman: /foreman " + node.substring(node.indexOf('.') + 1);
    }

    /** Nodes that require OP / explicit ALLOW by default. All others default to ALL. */
    private static boolean isOpByDefault(String node) {
        return RELOAD.equals(node) || IMPORT.equals(node) || EXPORT.equals(node);
    }

    /** Registers all nodes with ServerUtilities. No-op when SU is absent. Call from init(). */
    public static void registerNodes() {
        if (!SU_PRESENT) return;
        for (String node : new String[] { LIST, GUI, OPEN, CREATE, ASSIGN, UNASSIGN, DONE, RELOAD, IMPORT, EXPORT }) {
            ForemanPermissionsSU.registerNode(node, isOpByDefault(node), describe(node));
        }
    }

    /**
     * @return true if the sender may use the subcommand backing {@code node}.
     */
    public static boolean has(ICommandSender sender, String node) {
        if (!(sender instanceof EntityPlayerMP player)) return true; // console
        if (SU_PRESENT) {
            return ForemanPermissionsSU.hasPermission(player, node);
        }
        // Fallback without ServerUtilities.
        if (!isOpByDefault(node)) return true;
        return MinecraftServer.getServer()
            .getConfigurationManager()
            .func_152596_g(player.getGameProfile());
    }
}

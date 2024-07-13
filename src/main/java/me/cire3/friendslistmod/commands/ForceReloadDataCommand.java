package me.cire3.friendslistmod.commands;

import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

public class ForceReloadDataCommand {
    public static void register(FriendsListMod mod) {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("forcereloadplayerdata").executes(context -> {
            mod.setupData(MinecraftClient.getInstance());
            context.getSource().sendFeedback(new LiteralText("Successfully reloaded player data!"));
            return 1;
        }));
    }
}
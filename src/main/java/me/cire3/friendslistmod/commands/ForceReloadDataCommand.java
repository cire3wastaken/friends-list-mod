package me.cire3.friendslistmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class ForceReloadDataCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, FriendsListMod mod) {
        dispatcher.register(ClientCommandManager.literal("forcereloadplayerdata").executes(context -> {
            mod.setupData();
            context.getSource().sendFeedback(Text.literal("Successfully reloaded player data!"));
            return 1;
        }));
    }
}
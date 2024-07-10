package me.cire3.friendslistmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class ToggleModCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("toggleplayeroutlines").executes(context -> {
            context.getSource().sendFeedback(Text.literal("Toggled player outlines in Friends List Mod!"));
            FriendsListMod.enabled = !FriendsListMod.enabled;
            return 1;
        }));
    }
}

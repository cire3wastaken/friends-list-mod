package me.cire3.friendslistmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class ToggleArchOnlyMessagesCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("togglearchonlymessages").executes(context -> {
            FriendsListMod.sendOnArchOnly = !FriendsListMod.sendOnArchOnly;
            context.getSource().sendFeedback(Text.literal("Toggled arch only messages in Friends List Mod! Messages are now sent on " +
                    (FriendsListMod.sendOnArchOnly ? "ONLY ARCH" : "ALL servers") + "."));
            return 1;
        }));
    }
}
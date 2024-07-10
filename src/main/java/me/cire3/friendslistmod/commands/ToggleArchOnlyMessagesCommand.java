package me.cire3.friendslistmod.commands;

import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.text.LiteralText;

public class ToggleArchOnlyMessagesCommand {
    public static void register() {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("togglearchonlymessages").executes(context -> {
            FriendsListMod.sendOnArchOnly = !FriendsListMod.sendOnArchOnly;
            context.getSource().sendFeedback(new LiteralText("Toggled arch only messages in Friends List Mod! Messages are now sent on " +
                    (FriendsListMod.sendOnArchOnly ? "ONLY ARCH" : "ALL servers") + "."));
            return 1;
        }));
    }
}
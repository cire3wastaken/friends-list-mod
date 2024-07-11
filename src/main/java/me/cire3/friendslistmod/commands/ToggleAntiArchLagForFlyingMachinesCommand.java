package me.cire3.friendslistmod.commands;

import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.text.LiteralText;

public class ToggleAntiArchLagForFlyingMachinesCommand {
    public static void register(FriendsListMod mod) {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("toggleantiarchlagforflayingmachines").executes(context -> {
            FriendsListMod.antiArchLagForFlyingMachine = !FriendsListMod.antiArchLagForFlyingMachine;

            context.getSource().sendFeedback(new LiteralText("Toggled antiArchLagForFlyingMachine in Friends List Mod! antiArchLagForFlyingMachine is now " + (FriendsListMod.antiArchLagForFlyingMachine ? "ON" : "OFF") + "."));
            return 1;
        }));
    }
}

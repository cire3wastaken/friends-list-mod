package me.cire3.friendslistmod.commands;

import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.LiteralText;

public class TogglePlayerOutlinesCommand {
    public static void register(FriendsListMod mod) {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("toggleplayeroutlines").executes(context -> {
            FriendsListMod.outlinesEnabled = !FriendsListMod.outlinesEnabled;
            if (FriendsListMod.outlinesEnabled) {
                mod.updateAll(MinecraftClient.getInstance());
            } else {
                for (AbstractClientPlayerEntity player : FriendsListMod.teammateEntities) {
                    player.removeStatusEffect(StatusEffects.GLOWING);
                    player.setGlowing(false);
                }

                for (AbstractClientPlayerEntity player : FriendsListMod.kosEntities) {
                    player.removeStatusEffect(StatusEffects.GLOWING);
                    player.setGlowing(false);
                }
            }

            context.getSource().sendFeedback(new LiteralText("Toggled player outlines in Friends List Mod! Player outlines are now " + (FriendsListMod.outlinesEnabled ? "ON" : "OFF") + "."));
            return 1;
        }));
    }
}
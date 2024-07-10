package me.cire3.friendslistmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.cire3.friendslistmod.FriendsListMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;

public class ToggleModCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess, FriendsListMod mod) {
        dispatcher.register(ClientCommandManager.literal("toggleplayeroutlines").executes(context -> {
            FriendsListMod.enabled = !FriendsListMod.enabled;
            if (FriendsListMod.enabled) {
                mod.update();
            } else {
                for (AbstractClientPlayerEntity player : FriendsListMod.teammateEntities) {
                    player.removeStatusEffect(StatusEffects.GLOWING);
                    player.setGlowing(false);
                }

                for (AbstractClientPlayerEntity player : FriendsListMod.kosEntities) {
                    player.removeStatusEffect(StatusEffects.GLOWING);
                    player.setGlowing(true);
                }
            }

            context.getSource().sendFeedback(Text.literal("Toggled player outlines in Friends List Mod! Player outlines are now " + (FriendsListMod.enabled ? "ON" : "OFF") + "."));
            return 1;
        }));
    }
}

package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityPassengersSet", at = @At(value = "TAIL"))
    public void friendslistmod$attemptToReenterRiderAsPassengerWhenLaggy(EntityPassengersSetS2CPacket packet, CallbackInfo ci, @Local boolean bl) {
        if (!FriendsListMod.antiArchLagForFlyingMachine)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null)
            return;

        if (mc.getCurrentServerEntry() != null && !mc.getCurrentServerEntry().address.contains("mc.arch.lol")) {
            Entity entity = mc.world.getEntityById(packet.getId());

            boolean wasPlayerAPassenger = bl;

            if (entity == null)
                return;

            for (int i : packet.getPassengerIds()) {
                Entity entity2 = mc.world.getEntityById(i);
                if (entity2 != null)
                    if (entity2 == mc.player)
                        return; // we dont care if the player is still in the passengers
            }

            if (!wasPlayerAPassenger)
                return;

            // player is not in new passenger list, player was a passenger before this packet, and this is enabled
            // ran NEXT tick

            for (int i = 0; i < 4; i++) {
                FriendsListMod.scheduleTask(i, () -> {
                    if (mc.player != null && mc.world != null) {
                        if (mc.player.getVehicle() == null)
                            mc.player.interact(entity, Hand.MAIN_HAND);
                    }
                });
            }

            // we are not in a boat, this is the 5th tick since being kicked out, log out so we dont die
            FriendsListMod.scheduleTask(4, () -> {
                if (mc.player != null && mc.world != null) {
                    if (mc.player.getVehicle() == null)
                        mc.disconnect();
                }
            });
        }
    }
}

package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayPingS2CPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityPassengersSet", at = @At(value = "TAIL"))
    public void friendslistmod$attemptToReenterAsPassengerWhenLaggy(EntityPassengersSetS2CPacket packet, CallbackInfo ci, @Local(ordinal = 0) Entity entity, @Local boolean bl) {
        if (!FriendsListMod.antiArchLagForFlyingMachine)
            return;

        boolean wasPlayerAPassenger = bl;
        if (!wasPlayerAPassenger)
            return;

        if (entity == null)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null)
            return;

        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.contains("mc.arch.lol")) {
            // we could technically remove this loop via another mixin, but its not priority + the loop is only up to a few passengers
            for (int i : packet.getPassengerIds()) {
                if (i == mc.player.getId())
                    return; // we dont care if the player is still in the passengers
            }

            // player is not in new passenger list, player was a passenger before this packet, and this feature is enabled
            for (int i = 0; i < 4; i++) {
                FriendsListMod.scheduleTask(i, () -> {
                    if (mc.player != null && mc.world != null) {
                        if (mc.player.getVehicle() == null)
                            mc.player.interact(entity, Hand.MAIN_HAND);
                    }
                });
            }

            // we are not a passenger, this is the 5th tick since being kicked out, log out so we dont die
            FriendsListMod.scheduleTask(4, () -> {
                if (mc.player != null && mc.world != null) {
                    if (mc.player.getVehicle() == null)
                        mc.disconnect();
                }
            });
        }
    }

//     polar is actually retarded and won't kick me for skipping transactions ?!?!
    @Inject(method = "onPing", at = @At("HEAD"), cancellable = true)
    public void friendslistmod$cancelNextTransaction(PlayPingS2CPacket packet, CallbackInfo ci) {
        if (FriendsListMod.cancelTransactionAmount > 0) {
            FriendsListMod.cancelTransactionAmount--;
            ci.cancel();
        }
    }
}

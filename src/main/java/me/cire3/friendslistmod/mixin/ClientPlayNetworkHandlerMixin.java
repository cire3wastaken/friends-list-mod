package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
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

        Entity entity = mc.world.getEntityById(packet.getId());

        if (entity == null)
            return;

        if (!bl) // bl is whether player was riding
            return;

        FriendsListMod.scheduleTask(0, () -> mc.player.interact(entity, Hand.MAIN_HAND));
        FriendsListMod.scheduleTask(1, () -> mc.player.interact(entity, Hand.MAIN_HAND));
        FriendsListMod.scheduleTask(2, () -> mc.player.interact(entity, Hand.MAIN_HAND));
        FriendsListMod.scheduleTask(3, () -> mc.player.interact(entity, Hand.MAIN_HAND));

        FriendsListMod.scheduleTask(4, () -> {
            if (mc.player != null && mc.world != null) {
                if (mc.player.getVehicle() == null)
                    mc.disconnect();
            }
        });
    }
}

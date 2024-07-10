package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyExpressionValue(method = "renderLabelIfPresent", at = @At(value = "CONSTANT", args = "intValue=-1"))
    public int friendslistmod$changenametagcolor(int value, @Local(argsOnly = true) Entity entity) {
        if (FriendsListMod.enabled) {
            if (entity instanceof AbstractClientPlayerEntity player) {
                if (FriendsListMod.teammateEntities.contains(player))
                    return 65280;
                if (FriendsListMod.kosEntities.contains(player))
                    return 16711680;
            }
        }

        return value;
    }

    @ModifyExpressionValue(method = "renderLabelIfPresent", at = @At(value = "CONSTANT", args = "doubleValue=4096.0"))
    public double friendslistmod$alwaysrendernametag(double value, @Local(argsOnly = true) Entity entity) {
        if (FriendsListMod.enabled) {
            if (entity instanceof AbstractClientPlayerEntity player) {
                if (FriendsListMod.kosEntities.contains(player) || FriendsListMod.teammateEntities.contains(player))
                    return Double.MAX_VALUE;
            }
        }

        return value;
    }
}

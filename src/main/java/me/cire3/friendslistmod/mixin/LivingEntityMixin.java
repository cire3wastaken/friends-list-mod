package me.cire3.friendslistmod.mixin;

import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    public void friendslistmod$forceEntityGlowing(CallbackInfoReturnable<Boolean> cir) {
        if ((Entity) (Object) this instanceof AbstractClientPlayerEntity entity) {
            if (FriendsListMod.kosEntities.contains(entity) || FriendsListMod.teammateEntities.contains(entity))
                cir.setReturnValue(true);
        }
    }
}

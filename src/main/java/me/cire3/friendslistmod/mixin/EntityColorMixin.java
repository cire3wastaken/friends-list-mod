package me.cire3.friendslistmod.mixin;

import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// copied from https://github.com/Emafire003/ColoredGlowLib/blob/main/src/main/java/me/emafire003/dev/coloredglowlib/mixin/EntityColorMixin.java cuz lazy
@Mixin(Entity.class)
public abstract class EntityColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    public void friendslistmod$injectChangeColorValue(CallbackInfoReturnable<Integer> cir) {
        if ((Entity) (Object) this instanceof AbstractClientPlayerEntity entity) {
            if (FriendsListMod.kosEntities.contains(entity)) {
                cir.setReturnValue(16711680);
                return;
            } else if (FriendsListMod.teammateEntities.contains(entity)) {
                cir.setReturnValue(65280);
                return;
            }
        }
    }
}
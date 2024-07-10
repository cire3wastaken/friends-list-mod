package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT", args = "intValue=-1", ordinal = 1))
    public int friendslistmod$changetablistnamecolor(int value, @Local GameProfile profile) {
        if (FriendsListMod.enabled) {
            String username = profile.getName();
            for (String teammate : FriendsListMod.teammates)
                if (teammate.equals(username))
                    return 65280;

            for (String kos : FriendsListMod.kos)
                if (kos.equals(username))
                    return 16711680;
        }

        return value;
    }
}

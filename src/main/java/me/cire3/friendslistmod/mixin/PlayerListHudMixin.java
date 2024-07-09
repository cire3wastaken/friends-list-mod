package me.cire3.friendslistmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import me.cire3.friendslistmod.FriendsListMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT", args = "intValue=-1", ordinal = 1))
    public int friendslistmod$changetablistnamecolor(int value, @Local GameProfile profile) {
        String username = profile.getName();
        for (String teammate : FriendsListMod.teammates)
            if (teammate.equals(username))
                return 65280;

        for (String kos : FriendsListMod.kos)
            if (kos.equals(username))
                return 16711680;

        return value;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;collectPlayerEntries()Ljava/util/List;"))
    public List<PlayerListEntry> friendslistmod$reorderplayerlist(PlayerListHud instance) {
        List<PlayerListEntry> originalList = MinecraftClient.getInstance().getNetworkHandler().getListedPlayerListEntries().stream().limit(80).toList();

        int ptr = 0;
        List<PlayerListEntry> newList = new ArrayList<>(originalList.size());

        reorderLoop:
        for (PlayerListEntry entry : originalList) {
            String username = entry.getProfile().getName();
            String displayname = FriendsListMod.getDisplayName(entry).asTruncatedString(50);
            Style originalStyle = FriendsListMod.getDisplayName(entry).getStyle();
            int idx = displayname.indexOf(username);

            MutableText p1 = Text.literal(displayname.substring(0, idx)).setStyle(originalStyle);
            MutableText p2 = Text.literal(displayname.substring(idx + username.length() - 1, displayname.length() - 1)).setStyle(originalStyle);

            Formatting formatting = Formatting.WHITE;

            for (String teammate : FriendsListMod.teammates)
                if (teammate.equals(username)) {
                    formatting = Formatting.GREEN;
                    break;
                }

            for (String kos : FriendsListMod.kos)
                if (kos.equals(username)) {
                    formatting = Formatting.RED;
                    break;
                }

            p1.append(Text.literal(username).formatted(formatting)).append(p2);

            for (String teammate : FriendsListMod.teammates) {
                if (teammate.equals(username)) {
                    entry.setDisplayName(p1);
                    newList.add(0, entry);
                    continue reorderLoop;
                }
            }

            for (String kos : FriendsListMod.kos) {
                if (kos.equals(username)) {
                    entry.setDisplayName(p1);
                    newList.add(entry);
                    continue reorderLoop;
                }
            }

            newList.add(ptr++, entry);
        }

        return newList;
    }
}

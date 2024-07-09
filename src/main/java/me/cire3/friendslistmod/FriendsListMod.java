package me.cire3.friendslistmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendsListMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("friendslistmod");
    public static final String DATA_URL = "https://raw.githubusercontent.com/cire3wastaken/friends-list-mod/1_21/src/main/resources/assets/friendslistmod/data.json";
    public static JsonObject jsonData = null;
    public static String[] teammates;
    public static String[] kos;
    public final static Set<AbstractClientPlayerEntity> teammateEntities = new HashSet<>();
    public final static Set<AbstractClientPlayerEntity> kosEntities = new HashSet<>();
    private final static Set<AbstractClientPlayerEntity> alertedKosEntities = new HashSet<>();

    private static int lastCount = -1;
    private static long lastRun = -1;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            this.update(trackedEntity);
        });

        EntityTrackingEvents.STOP_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof AbstractClientPlayerEntity) {
                kosEntities.remove((AbstractClientPlayerEntity) trackedEntity);
                teammateEntities.remove((AbstractClientPlayerEntity) trackedEntity);
                alertedKosEntities.remove((AbstractClientPlayerEntity) trackedEntity);
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register((minecraftClient) -> {
            // run every 15 seconds
            if (lastRun == -1 || (System.currentTimeMillis() - lastRun >= 15000)) {
                lastRun = System.currentTimeMillis();

                if (jsonData == null || teammates == null || kos == null)
                    this.setupData();

                // we will mostly use spawn packets for registering ppl as enemies or teammates
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world != null && mc.player != null)
                    this.update();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void setupData() {
        try (BufferedInputStream in = new BufferedInputStream(new URL(DATA_URL).openStream())) {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = in.read(buffer, 0, 1024)) != -1) {
                bao.write(buffer, 0, count);
            }
            jsonData = JsonParser.parseString(bao.toString()).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("shoot", e);
            // use hard coded list instead
            jsonData = null;
        }

        try {
            if (jsonData == null) {
                Resource resource;
                try {
                    resource = MinecraftClient.getInstance().getResourceManager().getResourceOrThrow(Identifier.of("friendslistmod", "data.json"));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (resource == null)
                    throw new RuntimeException("Could not find friends data!");

                byte[] bytes = resource.getInputStream().readAllBytes();
                String json = new String(bytes);
                jsonData = JsonParser.parseString(json).getAsJsonObject();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (jsonData == null)
            throw new RuntimeException("Could not load friends data!");

        JsonArray teammatesJson = jsonData.getAsJsonArray("teammates");
        teammates = new String[teammatesJson.size()];
        for (int i = 0; i < teammates.length; i++)
            teammates[i] = teammatesJson.get(i).getAsString();

        JsonArray kosJson = jsonData.getAsJsonArray("kos");
        kos = new String[kosJson.size()];
        for (int i = 0; i < kos.length; i++)
            kos[i] = kosJson.get(i).getAsString();
    }

    public void update() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null)
            return;

        List<AbstractClientPlayerEntity> entities = mc.world.getPlayers();
        if (entities.size() == lastCount)
            return;

        lastCount = entities.size();

        kosEntities.clear();
        teammateEntities.clear();

        for (AbstractClientPlayerEntity player : entities) {
            String username = player.getGameProfile().getName();
            this.update(username, player);
        }
    }

    public void update(Entity entity) {
        if (entity instanceof AbstractClientPlayerEntity) {
            this.update(((AbstractClientPlayerEntity) entity).getGameProfile().getName(), (AbstractClientPlayerEntity) entity, true);
        }
    }

    public void update(String username, AbstractClientPlayerEntity player) {
        this.update(username, player, false);
    }

    public void update(String username, AbstractClientPlayerEntity player, boolean wasFromPacket) {
        for (String teammate : teammates) {
            // cracked servers suck balls otherwise this be a UUID list kms
            if (teammate.equalsIgnoreCase(username)) { // must becuz cracked servers suck my balls bro
                MutableText newUsername = preparePlayerAndGetNewUsername(username, player);
                Style style = newUsername.getStyle().withColor(Formatting.GREEN);

                player.setCustomName(newUsername.setStyle(style));

                teammateEntities.add(player);

                return;
            }
        }

        for (String kos : kos) {
            // cracked servers suck balls otherwise this be a UUID list kms
            if (username.toLowerCase().contains(kos.toLowerCase())) {
                MutableText newUsername = preparePlayerAndGetNewUsername(username, player);
                Style style = newUsername.getStyle().withColor(Formatting.RED);

                player.setCustomName(newUsername.setStyle(style));

                if (wasFromPacket || !alertedKosEntities.contains(player)) {
                    Entity clientPlayer = MinecraftClient.getInstance().player;

                    int left = -42;
                    int right = 43;
                    int top = 43;
                    int bottom = -42;

                    double targetX = player.getX();
                    double targetY = player.getY();
                    double targetZ = player.getZ();

                    double selfX = clientPlayer.getX();
                    double selfY = clientPlayer.getY();
                    double selfZ = clientPlayer.getZ();

                    if (!inBounds(left, top, right, bottom, targetX, targetZ) && !inBounds(left, top, right, bottom, selfX, selfZ)) {
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("clans chat FLM: Found KOS: %player% at %x%, %y%, %z%"
                                .replace("%player%", username)
                                .replace("%x%", (int) targetX + "")
                                .replace("%y%", (int) targetY + "")
                                .replace("%z%", (int) targetZ + ""));

                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("clans chat FLM: I am at %x%, %y%, %z%"
                                .replace("%x%", (int) selfX + "")
                                .replace("%y%", (int) selfY + "")
                                .replace("%z%", (int) selfZ + ""));

                        alertedKosEntities.add(player);
                    }
                }

                kosEntities.add(player);

                return;
            }
        }
    }

    private MutableText preparePlayerAndGetNewUsername(String username, AbstractClientPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY))
            player.removeStatusEffect(StatusEffects.INVISIBILITY);

        if (!player.hasStatusEffect(StatusEffects.GLOWING)) {
            final RegistryEntry<StatusEffect> EFFECT = StatusEffects.GLOWING;
            player.addStatusEffect(new StatusEffectInstance(EFFECT, -1, 1));
            player.setGlowing(true);
        }

        return Text.literal(username);
    }

    private static boolean inBounds(double left, double top, double right, double bottom, double posX, double posY) {
        return left <= posX && posX <= right && bottom <= posY && posY <= top;
    }

    public static Text getDisplayName(PlayerListEntry entry) {
        return entry.getDisplayName() != null
                ? applyGameModeFormatting(entry, entry.getDisplayName().copy())
                : applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName())));
    }

    private static Text applyGameModeFormatting(PlayerListEntry entry, MutableText name) {
        return entry.getGameMode() == GameMode.SPECTATOR ? name.formatted(Formatting.ITALIC) : name;
    }
}

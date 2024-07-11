package me.cire3.friendslistmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.cire3.friendslistmod.commands.ForceReloadDataCommand;
import me.cire3.friendslistmod.commands.ToggleAntiArchLagForFlyingMachinesCommand;
import me.cire3.friendslistmod.commands.ToggleArchOnlyMessagesCommand;
import me.cire3.friendslistmod.commands.TogglePlayerOutlinesCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.Resource;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.LiteralText;
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
import java.net.URL;
import java.util.*;

public class FriendsListMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("friendslistmod");
    public static final String FALLBACK_DATA_URL = "https://raw.githubusercontent.com/cire3wastaken/friends-list-mod/1_21/src/main/resources/assets/friendslistmod/data.json";
    public static final String POINTER_DATA_URL = "https://raw.githubusercontent.com/cire3wastaken/friends-list-mod/1_20_4/pointer.txt";
    public static JsonObject jsonData = null;
    public static String[] teammates;
    public static String[] kos;
    public static boolean sendOnArchOnly = true;
    public static boolean outlinesEnabled = true;
    public static boolean antiArchLagForFlyingMachine = false;
    public static final Set<AbstractClientPlayerEntity> teammateEntities = new HashSet<>();
    public static final Set<AbstractClientPlayerEntity> kosEntities = new HashSet<>();
    private static final Set<AbstractClientPlayerEntity> alertedKosEntities = new HashSet<>();

    private static int lastCount = -1;
    private static long lastRun = -1;

    private static Map<Runnable, Long> tasks = new HashMap<>();

    public static void scheduleTask(int ticks, Runnable runnable) {
        tasks.put(runnable, System.currentTimeMillis() + ticks * 50L);
    }

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
            Map<Runnable, Long> map = new HashMap<>(tasks.size());

            for (Map.Entry<Runnable, Long> entry : tasks.entrySet()) {
                if (System.currentTimeMillis() >= entry.getValue()) {
                    entry.getKey().run();
                } else {
                    map.put(entry.getKey(), entry.getValue());
                }
            }

            tasks = map;

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

        ForceReloadDataCommand.register(this);
        ToggleArchOnlyMessagesCommand.register();
        TogglePlayerOutlinesCommand.register(this);
        ToggleAntiArchLagForFlyingMachinesCommand.register(this);
    }

    @SuppressWarnings("deprecation")
    public void setupData() {
        try (BufferedInputStream in = new BufferedInputStream(new URL(POINTER_DATA_URL).openStream())) {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = in.read(buffer, 0, 1024)) != -1) {
                bao.write(buffer, 0, count);
            }

            // read the URL the pointer url points to
            try (BufferedInputStream in2 = new BufferedInputStream(new URL(bao.toString()).openStream())) {
                ByteArrayOutputStream bao1 = new ByteArrayOutputStream();
                while ((count = in2.read(buffer, 0, 1024)) != -1) {
                    bao1.write(buffer, 0, count);
                }

                try {
                    jsonData = JsonParser.parseString(bao1.toString()).getAsJsonObject();
                } catch (JsonSyntaxException e) {
                    // silently swallow, use fallback
                    jsonData = null;
                }
            }
        } catch (Exception e) {
            LOGGER.error("POINTER url failed!", e);
            jsonData = null;
        }

        if (jsonData == null) {
            // try using fallback url instead
            try (BufferedInputStream in2 = new BufferedInputStream(new URL(FALLBACK_DATA_URL).openStream())) {
                ByteArrayOutputStream bao1 = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count = 0;
                while ((count = in2.read(buffer, 0, 1024)) != -1) {
                    bao1.write(buffer, 0, count);
                }

                try {
                    jsonData = JsonParser.parseString(bao1.toString()).getAsJsonObject();
                } catch (JsonSyntaxException e) {
                    // silently swallow, use hardcoded
                    jsonData = null;
                }
            } catch (Exception e) {
                LOGGER.error("FALLBACK url failed!", e);
                jsonData = null;
            }
        }

        try {
            // use hardcoded
            if (jsonData == null) {
                Resource resource;
                resource = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("friendslistmod", "data.json"));

                if (resource == null)
                    throw new RuntimeException("Could not find friends data!");

                byte[] bytes = resource.getInputStream().readAllBytes();
                String json = new String(bytes);
                jsonData = JsonParser.parseString(json).getAsJsonObject();
            }
        } catch (Exception e) {
            LOGGER.error("Hardcoded JSON failed!", e);
            jsonData = null;
        }

        // at this point, if jsonData is null we are cooked
        if (jsonData == null)
            throw new RuntimeException("Could not load data!");

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
                if (outlinesEnabled) {
                    Style style = newUsername.getStyle().withColor(Formatting.GREEN);

                    player.setCustomName(newUsername.setStyle(style));
                }

                teammateEntities.add(player);

                return;
            }
        }

        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
        boolean shouldSend = (server != null && server.address.contains("mc.arch.lol")) || !sendOnArchOnly;
        for (String kos : kos) {
            // cracked servers suck balls otherwise this be a UUID list kms
            if (username.toLowerCase().contains(kos.toLowerCase())) {
                MutableText newUsername = preparePlayerAndGetNewUsername(username, player);
                if (outlinesEnabled) {
                    Style style = newUsername.getStyle().withColor(Formatting.RED);

                    player.setCustomName(newUsername.setStyle(style));
                }

                if (wasFromPacket || !alertedKosEntities.contains(player)) {
                    ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;

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

                    if (!inBounds(left, top, right, bottom, targetX, targetZ)) {
                        if (shouldSend) {
                            clientPlayer.sendChatMessage("/clans chat FLM: Found KOS: %player% at %x%, %y%, %z%"
                                    .replace("%player%", username)
                                    .replace("%x%", (int) targetX + "")
                                    .replace("%y%", (int) targetY + "")
                                    .replace("%z%", (int) targetZ + ""));

                            clientPlayer.sendChatMessage("/clans chat FLM: I am at %x%, %y%, %z%"
                                    .replace("%x%", (int) selfX + "")
                                    .replace("%y%", (int) selfY + "")
                                    .replace("%z%", (int) selfZ + "") + (inBounds(left, top, right, bottom, selfX, selfZ) ? ". I am in spawn." : "."));

                            for (String teammate : teammates) {
                                if (MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream().anyMatch(
                                        (entry) -> teammate.equals(entry.getProfile().getName()))) {
                                    clientPlayer.sendChatMessage("/tpahere %teammate%"
                                            .replace("%teammate%", teammate));
                                }
                            }
                        }
                        alertedKosEntities.add(player);
                    }
                }

                kosEntities.add(player);

                return;
            }
        }
    }

    private MutableText preparePlayerAndGetNewUsername(String username, AbstractClientPlayerEntity player) {
        if (outlinesEnabled) {
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                player.removeStatusEffect(StatusEffects.INVISIBILITY);
                player.setInvisible(false);
            }

            if (!player.hasStatusEffect(StatusEffects.GLOWING)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 1));
                player.setGlowing(true);
            }
        }

        return new LiteralText(username);
    }

    private static boolean inBounds(double left, double top, double right, double bottom, double posX, double posY) {
        return left <= posX && posX <= right && bottom <= posY && posY <= top;
    }

    public static Text getDisplayName(PlayerListEntry entry) {
        return entry.getDisplayName() != null
                ? applyGameModeFormatting(entry, entry.getDisplayName().copy())
                : applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), new LiteralText(entry.getProfile().getName())));
    }

    private static Text applyGameModeFormatting(PlayerListEntry entry, MutableText name) {
        return entry.getGameMode() == GameMode.SPECTATOR ? name.formatted(Formatting.ITALIC) : name;
    }
}
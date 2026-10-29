package dev.qtiers;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class QTiers implements ClientModInitializer {
    public static final String MOD_ID = "qtiers";
    public static final Logger LOGGER = QTiersLog.LOGGER;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static final Map<TierSource, KeyMapping> CYCLE_KEYS = new EnumMap<>(TierSource.class);
    private static KeyMapping toggleKey;
    private static KeyMapping nearestKey;
    private static KeyMapping configKey;
    private static KeyMapping lobbyKey;
    private static KeyMapping leaderboardKey;

    /** Screens opened from chat commands must wait a tick, or closing chat replaces them. */
    private static Supplier<Screen> pendingScreen;

    @Override
    public void onInitializeClient() {
        QTiersConfig.load();
        TierApi.startDiskCache();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TierApi.flushDiskCache());

        CYCLE_KEYS.put(TierSource.MCTIERS, key("cycle_mctiers", InputConstants.UNKNOWN.getValue()));
        CYCLE_KEYS.put(TierSource.PVPTIERS, key("cycle_pvptiers", InputConstants.UNKNOWN.getValue()));
        CYCLE_KEYS.put(TierSource.SUBTIERS, key("cycle_subtiers", InputConstants.UNKNOWN.getValue()));
        CYCLE_KEYS.put(TierSource.MCPVP, key("cycle_mcpvp", InputConstants.UNKNOWN.getValue()));
        CYCLE_KEYS.put(TierSource.PVPHQ, key("cycle_pvphq", InputConstants.UNKNOWN.getValue()));
        CYCLE_KEYS.put(TierSource.TIERMC, key("cycle_tiermc", InputConstants.UNKNOWN.getValue()));
        toggleKey = key("toggle", InputConstants.UNKNOWN.getValue());
        nearestKey = key("nearest", InputConstants.KEY_H);
        configKey = key("config", InputConstants.UNKNOWN.getValue());
        lobbyKey = key("lobby", InputConstants.UNKNOWN.getValue());
        leaderboardKey = key("leaderboards", InputConstants.UNKNOWN.getValue());

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "opponent"), OpponentHud::render);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && entity instanceof Player target) OpponentHud.onAttack(target);
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(QTiers::onTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("qtiers")
                        .executes(ctx -> {
                            pendingScreen = () -> new QTiersConfigScreen(null);
                            return 1;
                        })
                        .then(literal("settings").executes(ctx -> {
                            pendingScreen = () -> new QTiersConfigScreen(null);
                            return 1;
                        }))
                        .then(literal("debug").executes(ctx -> {
                            DebugReport.run(Minecraft.getInstance());
                            return 1;
                        }))
                        .then(literal("list").executes(ctx -> {
                            pendingScreen = () -> new QTiersLobbyScreen(null);
                            return 1;
                        }))
                        .then(literal("top").executes(ctx -> {
                            pendingScreen = () -> new QTiersLeaderboardScreen(null);
                            return 1;
                        }))
                        .then(literal("compare")
                                .then(argument("first", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(onlinePlayerNames(), builder))
                                        .then(argument("second", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(onlinePlayerNames(), builder))
                                                .executes(ctx -> {
                                                    String a = StringArgumentType.getString(ctx, "first");
                                                    String b = StringArgumentType.getString(ctx, "second");
                                                    pendingScreen = () -> new QTiersCompareScreen(a, b, null);
                                                    return 1;
                                                }))))
                        .then(literal("reload").executes(ctx -> {
                            QTiersConfig.load();
                            TierApi.clearCache();
                            ctx.getSource().sendFeedback(prefix().append(Component.literal("Config reloaded and cache cleared.")));
                            return 1;
                        }))
                        .then(argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(onlinePlayerNames(), builder))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "player");
                                    pendingScreen = () -> new QTiersProfileScreen(name);
                                    return 1;
                                }))));
    }

    private static KeyMapping key(String name, int defaultKey) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.qtiers." + name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY));
    }

    private static void onTick(Minecraft client) {
        if (pendingScreen != null) {
            Supplier<Screen> screen = pendingScreen;
            pendingScreen = null;
            client.gui.setScreen(screen.get());
        }
        if (client.player == null) return;

        QTiersConfig config = QTiersConfig.get();
        for (Map.Entry<TierSource, KeyMapping> e : CYCLE_KEYS.entrySet()) {
            while (e.getValue().consumeClick()) {
                QTiersConfig.SiteSettings site = config.site(e.getKey());
                site.gamemode = GameModes.next(e.getKey(), site.gamemode);
                QTiersConfig.save();
                client.gui.hud.setOverlayMessage(Component.literal(e.getKey().displayName + ": ")
                        .withStyle(s -> s.withColor(e.getKey().color))
                        .append(ModeTexts.styledName(e.getKey(), site.gamemode)), false);
            }
        }
        while (toggleKey.consumeClick()) {
            config.enabled = !config.enabled;
            QTiersConfig.save();
            client.gui.hud.setOverlayMessage(Component.literal("QTiers ")
                    .append(config.enabled ? Component.literal("enabled").withStyle(ChatFormatting.GREEN)
                            : Component.literal("disabled").withStyle(ChatFormatting.RED)), false);
        }
        while (configKey.consumeClick()) {
            client.gui.setScreen(new QTiersConfigScreen(null));
        }
        while (lobbyKey.consumeClick()) {
            client.gui.setScreen(new QTiersLobbyScreen(null));
        }
        while (leaderboardKey.consumeClick()) {
            client.gui.setScreen(new QTiersLeaderboardScreen(null));
        }
        while (nearestKey.consumeClick()) {
            AbstractClientPlayer nearest = client.level.players().stream()
                    .filter(p -> p != client.player)
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(client.player)))
                    .orElse(null);
            if (nearest == null) {
                client.gui.hud.setOverlayMessage(Component.literal("No players nearby").withStyle(ChatFormatting.GRAY), false);
            } else {
                client.gui.setScreen(new QTiersProfileScreen(nearest.getGameProfile().name()));
            }
        }
    }

    private static List<String> onlinePlayerNames() {
        var handler = Minecraft.getInstance().getConnection();
        if (handler == null) return List.of();
        return handler.getOnlinePlayers().stream().map(PlayerInfo::getProfile).map(p -> p.name()).toList();
    }

    public static MutableComponent prefix() {
        return Component.literal("[QTiers] ").withStyle(s -> s.withColor(0xE8BA3A));
    }
}

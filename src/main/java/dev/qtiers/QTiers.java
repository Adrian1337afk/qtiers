package dev.qtiers;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class QTiers implements ClientModInitializer {
    public static final String MOD_ID = "qtiers";
    public static final Logger LOGGER = LoggerFactory.getLogger("QTiers");

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    private static final Map<TierSource, KeyBinding> CYCLE_KEYS = new EnumMap<>(TierSource.class);
    private static KeyBinding toggleKey;
    private static KeyBinding nearestKey;
    private static KeyBinding configKey;

    /** Screens opened from chat commands must wait a tick, or closing chat replaces them. */
    private static Supplier<Screen> pendingScreen;

    @Override
    public void onInitializeClient() {
        QTiersConfig.load();

        CYCLE_KEYS.put(TierSource.PVPTIERS, key("cycle_pvptiers", GLFW.GLFW_KEY_UNKNOWN));
        CYCLE_KEYS.put(TierSource.SUBTIERS, key("cycle_subtiers", GLFW.GLFW_KEY_UNKNOWN));
        CYCLE_KEYS.put(TierSource.MCPVP, key("cycle_mcpvp", GLFW.GLFW_KEY_UNKNOWN));
        CYCLE_KEYS.put(TierSource.PVPHQ, key("cycle_pvphq", GLFW.GLFW_KEY_UNKNOWN));
        toggleKey = key("toggle", GLFW.GLFW_KEY_UNKNOWN);
        nearestKey = key("nearest", GLFW.GLFW_KEY_H);
        configKey = key("config", GLFW.GLFW_KEY_UNKNOWN);

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
                        .then(literal("reload").executes(ctx -> {
                            QTiersConfig.load();
                            TierApi.clearCache();
                            ctx.getSource().sendFeedback(prefix().append(Text.literal("Config reloaded and cache cleared.")));
                            return 1;
                        }))
                        .then(argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(onlinePlayerNames(), builder))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "player");
                                    pendingScreen = () -> new QTiersProfileScreen(name);
                                    return 1;
                                }))));
    }

    private static KeyBinding key(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.qtiers." + name, InputUtil.Type.KEYSYM, defaultKey, CATEGORY));
    }

    private static void onTick(MinecraftClient client) {
        if (pendingScreen != null) {
            Supplier<Screen> screen = pendingScreen;
            pendingScreen = null;
            client.setScreen(screen.get());
        }
        if (client.player == null) return;

        QTiersConfig config = QTiersConfig.get();
        for (Map.Entry<TierSource, KeyBinding> e : CYCLE_KEYS.entrySet()) {
            while (e.getValue().wasPressed()) {
                QTiersConfig.SiteSettings site = config.site(e.getKey());
                site.gamemode = GameModes.next(e.getKey(), site.gamemode);
                QTiersConfig.save();
                client.player.sendMessage(Text.literal(e.getKey().displayName + ": ")
                        .styled(s -> s.withColor(e.getKey().color))
                        .append(GameModes.styledName(e.getKey(), site.gamemode)), true);
            }
        }
        while (toggleKey.wasPressed()) {
            config.enabled = !config.enabled;
            QTiersConfig.save();
            client.player.sendMessage(Text.literal("QTiers ")
                    .append(config.enabled ? Text.literal("enabled").formatted(Formatting.GREEN)
                            : Text.literal("disabled").formatted(Formatting.RED)), true);
        }
        while (configKey.wasPressed()) {
            client.setScreen(new QTiersConfigScreen(null));
        }
        while (nearestKey.wasPressed()) {
            AbstractClientPlayerEntity nearest = client.world.getPlayers().stream()
                    .filter(p -> p != client.player)
                    .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(client.player)))
                    .orElse(null);
            if (nearest == null) {
                client.player.sendMessage(Text.literal("No players nearby").formatted(Formatting.GRAY), true);
            } else {
                client.setScreen(new QTiersProfileScreen(nearest.getGameProfile().name()));
            }
        }
    }

    private static List<String> onlinePlayerNames() {
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return List.of();
        return handler.getPlayerList().stream().map(PlayerListEntry::getProfile).map(p -> p.name()).toList();
    }

    public static MutableText prefix() {
        return Text.literal("[QTiers] ").styled(s -> s.withColor(0xE8BA3A));
    }
}

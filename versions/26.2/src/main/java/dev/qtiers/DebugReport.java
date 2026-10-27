package dev.qtiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;

/**
 * /qtiers debug: writes what the server sends for names (tab entries, nearby players, their
 * teams and any text entities floating above them) to the log, to find out why tiers don't show.
 */
final class DebugReport {
    private DebugReport() {}

    static void run(Minecraft client) {
        var handler = client.getConnection();
        if (handler == null || client.level == null || client.player == null) return;
        log("==== QTiers debug ====");
        log("server=" + (handler.getServerData() == null ? "?" : handler.getServerData().ip)
                + " tabEntries=" + handler.getOnlinePlayers().size() + " listed=" + handler.getListedOnlinePlayers().size());

        int shown = 0;
        for (PlayerInfo e : handler.getListedOnlinePlayers()) {
            if (shown++ >= 30) break;
            UUID id = e.getProfile().id();
            log("TAB name=" + e.getProfile().name() + " uuid=" + id + " v" + id.version()
                    + " display=" + (e.getTabListDisplayName() == null ? "null" : quote(e.getTabListDisplayName().getString()))
                    + " tiers=" + cached(id));
        }

        for (AbstractClientPlayer p : client.level.players()) {
            if (p.distanceToSqr(client.player) > 32 * 32) continue;
            UUID id = p.getUUID();
            PlayerTeam team = p.getTeam();
            log("PLAYER name=" + p.getGameProfile().name() + " uuid=" + id + " v" + id.version()
                    + " inTab=" + (handler.getPlayerInfo(id) != null)
                    + " listed=" + handler.getListedOnlinePlayers().stream().anyMatch(e -> e.getProfile().id().equals(id))
                    + " team=" + (team == null ? "none" : team.getName() + "/" + team.getNameTagVisibility())
                    + " invisible=" + p.isInvisible() + " passengers=" + p.getPassengers().size()
                    + " tiers=" + cached(id)
                    + " decorated=" + quote(TierDisplay.decorate(id, p.getDisplayName()).getString()));
            for (Entity e : client.level.getEntities(p, p.getBoundingBox().inflate(1, 3, 1))) {
                String text = e instanceof Display.TextDisplay td ? td.getText().getString()
                        : e.hasCustomName() ? e.getCustomName().getString() : null;
                if (text != null) {
                    log("  NEAR " + e.getType().toShortString() + " riding=" + (e.getVehicle() == p)
                            + " nameVisible=" + e.isCustomNameVisible() + " text=" + quote(text));
                }
            }
        }
        log("==== end ====");
        client.gui.hud.getChat().addClientSystemMessage(QTiers.prefix().append(Component.literal("Debug info written to the log.").withStyle(ChatFormatting.GRAY)));
    }

    private static String cached(UUID id) {
        StringBuilder sb = new StringBuilder();
        for (TierSource s : TierSource.values()) {
            String state = TierApi.getCached(id, s).map(p -> p.isRanked() ? "ranked" : "unranked").orElse("none");
            sb.append(s.shortName).append(':').append(state).append(' ');
        }
        return sb.toString().trim();
    }

    private static String quote(String s) {
        return "\"" + s.replace("\n", "\\n") + "\"";
    }

    private static void log(String line) {
        QTiers.LOGGER.info("[debug] {}", line);
    }
}

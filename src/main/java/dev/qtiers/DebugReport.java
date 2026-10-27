package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * /qtiers debug: writes what the server sends for names (tab entries, nearby players, their
 * teams and any text entities floating above them) to the log, to find out why tiers don't show.
 */
final class DebugReport {
    private DebugReport() {}

    static void run(MinecraftClient client) {
        var handler = client.getNetworkHandler();
        if (handler == null || client.world == null || client.player == null) return;
        log("==== QTiers debug ====");
        log("server=" + (handler.getServerInfo() == null ? "?" : handler.getServerInfo().address)
                + " tabEntries=" + handler.getPlayerList().size() + " listed=" + handler.getListedPlayerListEntries().size());

        int shown = 0;
        for (PlayerListEntry e : handler.getListedPlayerListEntries()) {
            if (shown++ >= 30) break;
            UUID id = e.getProfile().id();
            log("TAB name=" + e.getProfile().name() + " uuid=" + id + " v" + id.version()
                    + " display=" + (e.getDisplayName() == null ? "null" : quote(e.getDisplayName().getString()))
                    + " tiers=" + cached(id));
        }

        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            if (p.squaredDistanceTo(client.player) > 32 * 32) continue;
            UUID id = p.getUuid();
            Team team = p.getScoreboardTeam();
            log("PLAYER name=" + p.getGameProfile().name() + " uuid=" + id + " v" + id.version()
                    + " inTab=" + (handler.getPlayerListEntry(id) != null)
                    + " listed=" + handler.getListedPlayerListEntries().stream().anyMatch(e -> e.getProfile().id().equals(id))
                    + " team=" + (team == null ? "none" : team.getName() + "/" + team.getNameTagVisibilityRule())
                    + " invisible=" + p.isInvisible() + " passengers=" + p.getPassengerList().size()
                    + " tiers=" + cached(id)
                    + " decorated=" + quote(TierDisplay.decorate(id, p.getDisplayName()).getString()));
            for (Entity e : client.world.getOtherEntities(p, p.getBoundingBox().expand(1, 3, 1))) {
                String text = e instanceof DisplayEntity.TextDisplayEntity td ? td.getText().getString()
                        : e.hasCustomName() ? e.getCustomName().getString() : null;
                if (text != null) {
                    log("  NEAR " + e.getType().getUntranslatedName() + " riding=" + (e.getVehicle() == p)
                            + " nameVisible=" + e.isCustomNameVisible() + " text=" + quote(text));
                }
            }
        }
        log("==== end ====");
        client.player.sendMessage(QTiers.prefix().append(Text.literal("Debug info written to the log.").formatted(Formatting.GRAY)), false);
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

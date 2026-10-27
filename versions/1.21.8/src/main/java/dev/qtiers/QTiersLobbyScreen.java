package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** /qtiers list: everyone on the server, strongest first, with each site's tier in its own column. */
public class QTiersLobbyScreen extends QTiersListScreen {
    private static final int NAME_W = 100;

    private record Player(UUID uuid, String name, List<Optional<Ranking>> tiers, int best) {}

    private List<Player> players = List.of();
    private int refreshTicks;

    public QTiersLobbyScreen(@Nullable Screen parent) {
        super(Text.literal("Online players"), parent);
    }

    @Override
    protected void init() {
        super.init();
        refresh();
    }

    /** Re-reads the player list and cached tiers; called every second so tiers fill in as they load. */
    private void refresh() {
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) {
            players = List.of();
            return;
        }
        QTiersConfig config = QTiersConfig.get();
        List<Player> list = new ArrayList<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            // Skip a server's fake tab rows (blank names); its real players are still in the list, just unlisted
            if (!ServerNames.isRealPlayer(entry)) continue;
            UUID uuid = entry.getProfile().getId();
            List<Optional<Ranking>> tiers = new ArrayList<>();
            int best = Integer.MAX_VALUE;
            for (TierSource source : TierSource.values()) {
                Optional<Ranking> r = TierApi.getCached(uuid, source)
                        .flatMap(p -> TierDisplay.pick(p, config.site(source).gamemode, config));
                tiers.add(r);
                // Retired tiers count, but below every active one
                if (r.isPresent()) best = Math.min(best, r.get().score() + (r.get().retired() ? 1000 : 0));
            }
            list.add(new Player(uuid, entry.getProfile().getName(), tiers, best));
        }
        list.sort(Comparator.comparingInt(Player::best).thenComparing(p -> p.name().toLowerCase()));
        players = list;
    }

    @Override
    public void tick() {
        if (++refreshTicks % 20 == 0) refresh();
    }

    @Override
    protected int rowCount() {
        return players.size();
    }

    private int columnWidth(int width) {
        return (width - NAME_W) / TierSource.values().length;
    }

    @Override
    protected void renderHeader(DrawContext context, int x, int y, int width) {
        context.drawTextWithShadow(textRenderer, Text.literal(players.size() + " online"), x, y, 0xFFAAAAAA);
        int col = columnWidth(width);
        TierSource[] sources = TierSource.values();
        for (int i = 0; i < sources.length; i++) {
            TierSource s = sources[i];
            drawClipped(context, Text.literal(s.displayName).styled(st -> st.withColor(s.color)), x + NAME_W + i * col, y, col - 4, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderRow(DrawContext context, int index, int x, int y, int width, boolean hovered) {
        Player p = players.get(index);
        drawClipped(context, Text.literal(p.name()), x, y, NAME_W - 6, hovered ? 0xFFFFFF55 : 0xFFFFFFFF);
        int col = columnWidth(width);
        TierSource[] sources = TierSource.values();
        for (int i = 0; i < sources.length; i++) {
            TierSource source = sources[i];
            Optional<Ranking> r = p.tiers().get(i);
            Text cell;
            if (r.isPresent()) {
                Ranking ranking = r.get();
                MutableText t = Text.empty();
                if (QTiersConfig.get().showIcons) t.append(ModeTexts.icon(source, ranking.mode()));
                cell = t.append(Text.literal(ranking.label()).styled(s -> s.withColor(ranking.color())));
            } else {
                cell = Text.literal("-").styled(s -> s.withColor(0x555555));
            }
            context.drawTextWithShadow(textRenderer, cell, x + NAME_W + i * col, y, 0xFFFFFFFF);
        }
    }

    @Override
    protected Text emptyText() {
        return Text.literal("Join a server to see who's online");
    }

    @Override
    protected boolean rowClicked(int index) {
        openProfile(players.get(index).name());
        return true;
    }
}

package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.EnemyStat;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.EnemyButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EnemyStatMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemy;

    private final TreasureHolder treasure;
    private final EnemyStat.EnemyStatConfig configData;

    public EnemyStatMessageHolder(ArrayList<Enemy> enemy, @Nullable Message author, String userID, String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, TreasureHolder treasure, EnemyStat.EnemyStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.enemy = enemy;

        this.treasure = treasure;
        this.configData = configData;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = chunk * page; i < chunk * (page + 1); i++) {
            if (i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(e.id.id);

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(e.id.id) + "`";

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(e.id.id);
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(e, lang);

                    if (text == null) {
                        text = Data.trio(e.id.id);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(e.id.id);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        try {
            EntityHandler.generateEnemyEmbed(enemy.get(index), event, hasAuthorMessage() ? getAuthorMessage() : null, treasure, configData, true, lang, msg -> {
                end(true);

                StaticStore.putHolder(userID, new EnemyButtonHolder(hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, enemy.get(index), treasure, configData, lang));
            });
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EnemyStatMessageHolder::onSelected - Failed to upload enemy embed");
        }
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }
}

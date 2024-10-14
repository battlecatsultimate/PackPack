package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.EnemyStat;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
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

    public EnemyStatMessageHolder(ArrayList<Enemy> enemy, @Nullable Message author, String userID, String channelID, @Nonnull Message message, TreasureHolder treasure, EnemyStat.EnemyStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.enemy = enemy;

        this.treasure = treasure;
        this.configData = configData;
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for (int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if (i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id) + " ";

            if (MultiLangCont.get(e, lang) != null)
                ename += MultiLangCont.get(e, lang);

            data.add(ename);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        try {
            EntityHandler.showEnemyEmb(enemy.get(id), event, hasAuthorMessage() ? getAuthorMessage() : null, treasure, configData, true, lang, msg -> {
                end(true);

                StaticStore.putHolder(userID, new EnemyButtonHolder(hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, enemy.get(id), treasure, configData, lang));
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

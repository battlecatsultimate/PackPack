package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class EnemySpriteMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemy;

    private final int mode;

    public EnemySpriteMessageHolder(ArrayList<Enemy> enemy, Message author, String userID, String channelID, Message message, int mode, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.enemy = enemy;
        this.mode = mode;
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
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        try {
            Enemy e = enemy.get(id);

            EntityHandler.getEnemySprite(e, ch, getAuthorMessage(), mode, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EnemySpriteMessageHolder::onSelected - Failed to upload enemy sprite/icon");
        }

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }
}

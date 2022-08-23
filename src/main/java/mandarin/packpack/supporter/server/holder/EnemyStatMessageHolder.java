package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.bc.EntityHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class EnemyStatMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemy;

    private final boolean isFrame;
    private final boolean isExtra;
    private final boolean isCompact;
    private final int[] magnification;

    public EnemyStatMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int[] magnification, boolean isFrame, boolean isExtra, boolean isCompact, int lang) {
        super(msg, author, channelID, lang);

        this.enemy = enemy;

        this.magnification = magnification;
        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.isCompact = isCompact;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for (int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if (i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if (MultiLangCont.get(e) != null)
                ename += MultiLangCont.get(e);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(ename);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        msg.delete().queue();

        try {
            EntityHandler.showEnemyEmb(enemy.get(id), ch, isFrame, isExtra, isCompact, magnification, lang);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }
}

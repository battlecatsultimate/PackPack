package mandarin.packpack.supporter.server.holder.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.holder.Holder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AliasEnemyHolder extends Holder<MessageCreateEvent> {
    private final ArrayList<Enemy> enemy;
    private final Message msg;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private final int lang;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasEnemyHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, AliasHolder.MODE mode, int lang, @Nullable String aliasName) {
        super(MessageCreateEvent.class);

        this.enemy = enemy;
        this.msg = msg;
        this.mode = mode;
        this.channelID = channelID;
        this.aliasName = aliasName;

        this.lang = lang;

        registerAutoFinish(this, msg, author, lang, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return RESULT_STILL;

            page++;

            showPage();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            showPage();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= enemy.size())
                return RESULT_STILL;

            msg.delete().subscribe();

            String eName = StaticStore.safeMultiLangGet(enemy.get(id), lang);

            if(eName == null || eName.isBlank())
                eName = enemy.get(id).name;

            if(eName == null || eName.isBlank())
                eName = Data.trio(enemy.get(id).id.id);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.ENEMY, lang, enemy.get(id));

            switch (mode) {
                case GET:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", eName));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias_enemalias", lang).replace("_EEE_", eName).replace("_NNN_", alias.size()+""));
                        result.append("\n\n");

                        for(int i = 0; i < alias.size(); i++) {
                            String temp = " - " + alias.get(i);

                            if(result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias_etc", lang));
                                break;
                            }

                            result.append(temp);

                            if(i < alias.size() - 1) {
                                result.append("\n");
                            }
                        }

                        createMessageWithNoPings(ch, result.toString());
                    }
                    break;
                case ADD:
                    if(alias == null)
                        alias = new ArrayList<>();

                    if(aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }

                    if(alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_contain", lang).replace("_", eName));
                        break;
                    }

                    alias.add(aliasName);

                    AliasHolder.EALIAS.put(AliasHolder.getLangCode(lang), enemy.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_added", lang).replace("_DDD_", eName).replace("_AAA_", aliasName));
                    break;
                case REMOVE:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", eName));
                        break;
                    }

                    if(aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }

                    if(!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang).replace("_", eName));
                        break;
                    }

                    alias.remove(aliasName);

                    AliasHolder.EALIAS.put(AliasHolder.getLangCode(lang), enemy.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", eName).replace("_AAA_", aliasName));
                    break;
            }

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            Command.editMessage(msg, m -> {
                m.content(wrap(LangID.getStringByID("formst_cancel", lang)));
                expired = true;
            });

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if (StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) - 1;

                    if (p < 0 || p * 20 >= enemy.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    showPage();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));
    }

    private void showPage() {
        Command.editMessage(msg, m -> {
            String check;

            if(enemy.size() <= 20)
                check = "";
            else if(page == 0)
                check = LangID.getStringByID("formst_next", lang);
            else if((page + 1) * 20 >= enemy.size())
                check = LangID.getStringByID("formst_pre", lang);
            else
                check = LangID.getStringByID("formst_nexpre", lang);

            StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

            for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                if(i >= enemy.size())
                    break;

                Enemy e = enemy.get(i);

                String ename = Data.trio(e.id.id)+" ";

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                if(MultiLangCont.get(e) != null)
                    ename += MultiLangCont.get(e);

                CommonStatic.getConfig().lang = oldConfig;

                sb.append(i+1).append(". ").append(ename).append("\n");
            }

            if(enemy.size() > 20)
                sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

            sb.append(LangID.getStringByID("formst_can", lang));
            sb.append("```");

            m.content(wrap(sb.toString()));
        });
    }
}

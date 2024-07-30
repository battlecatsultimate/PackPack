package mandarin.packpack.supporter.server.holder.message.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.message.MessageHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class AliasEnemyMessageHolder extends MessageHolder {
    private final ArrayList<Enemy> enemy;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasEnemyMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, AliasHolder.MODE mode, CommonStatic.Lang.Locale lang, @Nullable String aliasName) {
        super(author, channelID, msg, lang);

        this.enemy = enemy;
        this.mode = mode;
        this.channelID = channelID;
        this.aliasName = aliasName;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        MessageChannel ch = event.getMessage().getChannel();

        if(!ch.getId().equals(channelID))
            return STATUS.WAIT;

        String content = event.getMessage().getContentRaw();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return STATUS.WAIT;

            page++;

            showPage();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return STATUS.WAIT;

            page--;

            showPage();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= enemy.size())
                return STATUS.WAIT;

            message.delete().queue();

            String eName = StaticStore.safeMultiLangGet(enemy.get(id), lang);

            if(eName == null || eName.isBlank())
                eName = enemy.get(id).names.toString();

            if(eName.isBlank())
                eName = Data.trio(enemy.get(id).id.id);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.ENEMY, lang, enemy.get(id));

            switch (mode) {
                case GET -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.noAlias.unit", lang).replace("_", eName));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias.aliases.enemy", lang).replace("_EEE_", eName).replace("_NNN_", String.valueOf(alias.size())));
                        result.append("\n\n");

                        for (int i = 0; i < alias.size(); i++) {
                            String temp = "- " + alias.get(i);

                            if (result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias.etc", lang));
                                break;
                            }

                            result.append(temp);

                            if (i < alias.size() - 1) {
                                result.append("\n");
                            }
                        }

                        createMessageWithNoPings(ch, result.toString());
                    }
                }
                case ADD -> {
                    if (alias == null)
                        alias = new ArrayList<>();
                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.failed.noName", lang));
                        break;
                    }
                    if (alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.contain", lang).replace("_", eName));
                        break;
                    }
                    alias.add(aliasName);
                    AliasHolder.EALIAS.put(lang, enemy.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias.added", lang).replace("_DDD_", eName).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias added\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
                case REMOVE -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.noAlias.unit", lang).replace("_", eName));
                        break;
                    }
                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.failed.noName", lang));
                        break;
                    }
                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.failed.removeFail", lang).replace("_", eName));
                        break;
                    }
                    alias.remove(aliasName);
                    AliasHolder.EALIAS.put(lang, enemy.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias.removed", lang).replace("_DDD_", eName).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias removed\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
            }

            cleaner.add(event.getMessage());

            end(true);

            return STATUS.FINISH;
        } else if(content.equals("c")) {
            message.editMessage(LangID.getStringByID("ui.search.canceled", lang)).queue();

            cleaner.add(event.getMessage());

            end(true);

            return STATUS.FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if (StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) - 1;

                    if (p < 0 || p * 20 >= enemy.size()) {
                        return STATUS.WAIT;
                    }

                    page = p;

                    showPage();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return STATUS.WAIT;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if (m == null)
                continue;

            if (m.getChannel() instanceof PrivateChannel)
                return;

            m.delete().queue();
        }
    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("ui.search.expired", lang))
                .mentionRepliedUser(false)
                .queue();
    }

    private void showPage() {
        String check;

        if(enemy.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("ui.search.old.page.nextOnly", lang);
        else if((page + 1) * 20 >= enemy.size())
            check = LangID.getStringByID("ui.search.old.page.previousOnly", lang);
        else
            check = LangID.getStringByID("ui.search.old.page.nextPrevious", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang)).append(check);

        for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id)+" ";

            if(MultiLangCont.get(e, lang) != null)
                ename += MultiLangCont.get(e, lang);

            sb.append(i+1).append(". ").append(ename).append("\n");
        }

        if(enemy.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = enemy.size() / SearchHolder.PAGE_CHUNK;

            if(enemy.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
        }

        sb.append(LangID.getStringByID("ui.search.old.page.cancel", lang));
        sb.append("```");

        message.editMessage(sb.toString()).queue();
    }
}

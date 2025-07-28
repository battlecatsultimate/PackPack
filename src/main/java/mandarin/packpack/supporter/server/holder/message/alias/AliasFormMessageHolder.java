package mandarin.packpack.supporter.server.holder.message.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.message.MessageHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class AliasFormMessageHolder extends MessageHolder {
    private final ArrayList<Form> form;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private int page = 0;
    private boolean expired = false;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasFormMessageHolder(ArrayList<Form> form, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, AliasHolder.MODE mode, CommonStatic.Lang.Locale lang, @Nullable String aliasName) {
        super(author, userID, channelID, msg, lang);

        this.form = form;
        this.channelID = channelID;
        this.mode = mode;
        this.aliasName = aliasName;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        if(expired) {
            System.out.println("Expired at AliasFormHolder!!");

            return STATUS.FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel();

        if(!ch.getId().equals(channelID))
            return STATUS.WAIT;

        String content = event.getMessage().getContentRaw();

        if(content.equals("n")) {
            if(20 * (page + 1) >= form.size())
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

            if(id < 0 || id >= form.size())
                return STATUS.WAIT;

            message.delete().queue();

            String fname = StaticStore.safeMultiLangGet(form.get(id), lang);

            if(fname == null || fname.isBlank())
                fname = form.get(id).names.toString();

            if(fname.isBlank())
                fname = Data.trio(form.get(id).unit.id.id)+"-"+Data.trio(form.get(id).fid);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.FORM, lang, form.get(id));

            switch (mode) {
                case GET -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.noAlias.unit", lang).replace("_", fname));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias.aliases.unit", lang).replace("_FFF_", fname).replace("_NNN_", String.valueOf(alias.size())));
                        result.append("\n\n");

                        for (int i = 0; i < alias.size(); i++) {
                            String temp = "- " + alias.get(i);

                            if (result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias.etc", lang));
                                break;
                            }

                            result.append(temp);

                            if (i < alias.size() - 1)
                                result.append("\n");
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
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.contain", lang).replace("_", fname));
                        break;
                    }
                    alias.add(aliasName);
                    AliasHolder.FALIAS.put(lang, form.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias.added", lang).replace("_DDD_", fname).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias added\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
                case REMOVE -> {
                    if (alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.noAlias.unit", lang).replace("_", fname));
                        break;
                    }
                    if (aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.failed.noName", lang));
                        break;
                    }
                    if (!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias.failed.removeFail", lang));
                        break;
                    }
                    alias.remove(aliasName);
                    AliasHolder.FALIAS.put(lang, form.get(id), alias);
                    createMessageWithNoPings(ch, LangID.getStringByID("alias.removed", lang).replace("_DDD_", fname).replace("_AAA_", aliasName));
                    StaticStore.logger.uploadLog("Alias removed\n\nUnit : " + fname + "\nAlias : " + aliasName + "\nBy : " + event.getAuthor().getAsMention());
                }
            }

            expired = true;

            cleaner.add(event.getMessage());

            return STATUS.FINISH;
        } else if(content.equals("c")) {
            message.editMessage(LangID.getStringByID("ui.search.canceled", lang)).queue();

            expired = true;

            cleaner.add(event.getMessage());

            return STATUS.FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= form.size()) {
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

        if(form.size() <= 20)
            check = "";
        else if(page == 0)
            check = LangID.getStringByID("ui.search.old.page.nextOnly", lang);
        else if((page + 1) * 20 >= form.size())
            check = LangID.getStringByID("ui.search.old.page.previousOnly", lang);
        else
            check = LangID.getStringByID("ui.search.old.page.nextPrevious", lang);

        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("ui.search.selectData", lang)).append(check);

        for(int i = 20 * page; i < 20 * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            sb.append(i+1).append(". ").append(fname).append("\n");
        }

        if(form.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = form.size() / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

            if(form.size() % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
                totalPage++;

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
        }

        sb.append(LangID.getStringByID("ui.search.old.page.cancel", lang));
        sb.append("```");

        message.editMessage(sb.toString()).queue();
    }
}

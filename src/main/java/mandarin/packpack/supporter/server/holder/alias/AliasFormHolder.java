package mandarin.packpack.supporter.server.holder.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.holder.Holder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AliasFormHolder extends Holder<MessageCreateEvent> {
    private final ArrayList<Form> form;
    private final Message msg;
    private final String channelID;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private final int lang;

    private int page = 0;
    private boolean expired = false;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public AliasFormHolder(ArrayList<Form> form, Message author, Message msg, String channelID, AliasHolder.MODE mode, int lang, @Nullable String aliasName) {
        super(MessageCreateEvent.class);

        this.form = form;
        this.msg = msg;
        this.channelID = channelID;
        this.mode = mode;
        this.aliasName = aliasName;

        this.lang = lang;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), AliasFormHolder.this));

                msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired at AliasFormHolder!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= form.size())
                return RESULT_STILL;

            page++;

            msg.edit(m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            msg.edit(m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= form.size())
                return RESULT_STILL;

            msg.delete().subscribe();

            String fname = StaticStore.safeMultiLangGet(form.get(id), lang);

            if(fname == null || fname.isBlank())
                fname = form.get(id).name;

            if(fname == null || fname.isBlank())
                fname = Data.trio(form.get(id).unit.id.id)+"-"+Data.trio(form.get(id).fid);

            ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.FORM, lang, form.get(id));

            switch (mode) {
                case GET:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", fname));
                    } else {
                        StringBuilder result = new StringBuilder(LangID.getStringByID("alias_formalias", lang).replace("_FFF_", fname).replace("_NNN_", alias.size()+""));
                        result.append("\n\n");

                        for(int i = 0; i < alias.size(); i++) {
                            String temp = " - "+alias.get(i);

                            if(result.length() + temp.length() > 1900) {
                                result.append("\n")
                                        .append(LangID.getStringByID("alias_etc", lang));
                                break;
                            }

                            result.append(temp);

                            if(i < alias.size() - 1)
                                result.append("\n");
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
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_contain", lang).replace("_", fname));
                        break;
                    }

                    alias.add(aliasName);

                    AliasHolder.FALIAS.put(AliasHolder.getLangCode(lang), form.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_added", lang).replace("_DDD_", fname).replace("_AAA_", aliasName));
                    break;
                case REMOVE:
                    if(alias == null || alias.isEmpty()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_noalias", lang).replace("_", fname));
                        break;
                    }

                    if(aliasName.isBlank()) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_aliasblank", lang));
                        break;
                    }

                    if(!alias.contains(aliasName)) {
                        createMessageWithNoPings(ch, LangID.getStringByID("alias_nosuch", lang));
                        break;
                    }

                    alias.remove(aliasName);

                    AliasHolder.FALIAS.put(AliasHolder.getLangCode(lang), form.get(id), alias);

                    createMessageWithNoPings(ch, LangID.getStringByID("alias_removed", lang).replace("_DDD_", fname).replace("_AAA_", aliasName));
                    break;
            }

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            msg.edit(m -> {
                m.setContent(LangID.getStringByID("formst_cancel", lang));
                expired = true;
            }).subscribe();

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= form.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    msg.edit(m -> {
                        String check;

                        if(form.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= form.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(check);

                        for(int i = 20 * page; i < 20 * (page +1); i++) {
                            if(i >= form.size())
                                break;

                            Form f = form.get(i);

                            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                            int oldConfig = CommonStatic.getConfig().lang;
                            CommonStatic.getConfig().lang = lang;

                            if(MultiLangCont.get(f) != null)
                                fname += MultiLangCont.get(f);

                            CommonStatic.getConfig().lang = oldConfig;

                            sb.append(i+1).append(". ").append(fname).append("\n");
                        }

                        if(form.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                        sb.append(LangID.getStringByID("formst_can", lang));
                        sb.append("```");

                        m.setContent(sb.toString());
                    }).subscribe();

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

        msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
    }
}

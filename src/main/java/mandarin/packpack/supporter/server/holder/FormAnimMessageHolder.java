package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class FormAnimMessageHolder extends MessageHolder<MessageCreateEvent> {
    private final ArrayList<Form> form;
    private final Message msg;
    private final String channelID;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final int lang;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    private int page = 0;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public FormAnimMessageHolder(ArrayList<Form> form, Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw, boolean gifMode) {
        super(MessageCreateEvent.class);

        this.form = form;
        this.msg = msg;
        this.channelID = channelID;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.lang = lang;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
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
            if(20 * (page + 1) >= form.size())
                return RESULT_STILL;

            page++;

            Command.editMessage(msg, m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

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

                m.content(wrap(sb.toString()));
            });

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            Command.editMessage(msg, m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

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

                m.content(wrap(sb.toString()));
            });

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= form.size())
                return RESULT_STILL;

            try {
                Form f = form.get(id);

                if(gif) {
                    TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                    if(timeBoolean == null || StaticStore.canDo.get("gif").canDo) {
                        new Thread(() -> {
                            Guild g = event.getGuild().block();

                            if(g == null)
                                return;

                            try {
                                boolean result = EntityHandler.generateFormAnim(f, ch, g.getPremiumTier().getValue(), mode, debug, frame, lang, raw, gif);

                                if(result) {
                                    long time = raw ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(30);

                                    StaticStore.canDo.put("gif", new TimeBoolean(false, time));

                                    Timer timer = new Timer();

                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            System.out.println("Remove Process : gif");
                                            StaticStore.canDo.put("gif", new TimeBoolean(true));
                                        }
                                    }, time);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        ch.createMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).subscribe();
                    }
                } else {
                    event.getMember().ifPresent(m -> {
                        try {
                            if(StaticStore.timeLimit.containsKey(m.getId().asString()) && StaticStore.timeLimit.get(m.getId().asString()).containsKey(StaticStore.COMMAND_FORMIMAGE_ID)) {
                                long time = StaticStore.timeLimit.get(m.getId().asString()).get(StaticStore.COMMAND_FORMIMAGE_ID);

                                if(System.currentTimeMillis() - time > 10000) {
                                    EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                                    StaticStore.timeLimit.get(m.getId().asString()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                                } else {
                                    ch.createMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).subscribe();
                                }
                            } else if(StaticStore.timeLimit.containsKey(m.getId().asString())) {
                                EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                                StaticStore.timeLimit.get(m.getId().asString()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                            } else {
                                EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                                Map<String, Long> memberLimit = new HashMap<>();

                                memberLimit.put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());

                                StaticStore.timeLimit.put(m.getId().asString(), memberLimit);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            msg.delete().subscribe();

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
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= form.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    Command.editMessage(msg, m -> {
                        String check;

                        if(form.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= form.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

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

                        m.content(wrap(sb.toString()));
                    });

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
}

package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class FormAnimMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    public FormAnimMessageHolder(ArrayList<Form> form, Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw, boolean gifMode) {
        super(msg, author, channelID, lang);

        this.form = form;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(f) != null)
                fname += MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Guild g = event.getGuild();

        if(g == null)
            return;

        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            if(gif) {
                TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                if(timeBoolean == null || StaticStore.canDo.get("gif").canDo) {
                    new Thread(() -> {
                        try {
                            boolean result = EntityHandler.generateFormAnim(f, ch, g.getBoostTier().getKey(), mode, debug, frame, lang, raw, gifMode);

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
                    ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).queue();
                }
            } else {
                Member m = event.getMember();

                if(m != null) {
                    try {
                        if(StaticStore.timeLimit.containsKey(m.getId()) && StaticStore.timeLimit.get(m.getId()).containsKey(StaticStore.COMMAND_FORMIMAGE_ID)) {
                            long time = StaticStore.timeLimit.get(m.getId()).get(StaticStore.COMMAND_FORMIMAGE_ID);

                            if(System.currentTimeMillis() - time > 10000) {
                                EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                                StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                            } else {
                                ch.sendMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).queue();
                            }
                        } else if(StaticStore.timeLimit.containsKey(m.getId())) {
                            EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                            StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                        } else {
                            EntityHandler.generateFormImage(f, ch, mode, frame, transparent, debug, lang);

                            Map<String, Long> memberLimit = new HashMap<>();

                            memberLimit.put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());

                            StaticStore.timeLimit.put(m.getId(), memberLimit);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        msg.delete().queue();
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}

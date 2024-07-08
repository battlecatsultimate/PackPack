package mandarin.packpack.supporter.server.holder.component.search;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormGif;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
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

    private final String command;

    public FormAnimMessageHolder(ArrayList<Form> form, @Nonnull Message author, Message msg, String channelID, int mode, int frame, boolean transparent, boolean debug, int lang, boolean isGif, boolean raw, boolean gifMode) {
        super(author, msg, channelID, lang);

        this.form = form;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        this.command = author.getContentRaw();

        registerAutoFinish(this, msg, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            if(FormGif.forbidden.contains(f.unit.id.id)) {
                message.delete().queue();

                ch.sendMessage(LangID.getStringByID("gif_dummy", lang)).queue();

                return;
            }

            if(gif) {
                TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                if(timeBoolean == null || StaticStore.canDo.get("gif").canDo) {
                    RecordableThread t = new RecordableThread(() -> {
                        Guild g;

                        if(ch instanceof GuildChannel) {
                            g = event.getGuild();
                        } else {
                            g = null;
                        }

                        EntityHandler.generateFormAnim(f, ch, getAuthorMessage(), g == null ? 0 : g.getBoostTier().getKey(), mode, debug, frame, lang, raw, gifMode, () -> {
                            if(!StaticStore.conflictedAnimation.isEmpty()) {
                                StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + command);
                                StaticStore.conflictedAnimation.clear();
                            }

                            User u = event.getUser();

                            if(raw) {
                                StaticStore.logger.uploadLog("Generated mp4 by user " + u.getName() + " for unit ID " + Data.trio(f.unit.id.id) + " with mode of " + mode);
                            }

                            long time = raw ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(30);

                            StaticStore.canDo.put("gif", new TimeBoolean(false, time));

                            StaticStore.executorHandler.postDelayed(time, () -> {
                                System.out.println("Remove Process : gif");

                                StaticStore.canDo.put("gif", new TimeBoolean(true));
                            });
                        }, () -> {

                        });
                    }, e -> StaticStore.logger.uploadErrorLog(e, "E/FormAnimMessageHolder::onSelected - Failed to generate animation"));

                    t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + getAuthorMessage().getContentRaw());
                    t.start();
                } else {
                    ch.sendMessage(LangID.getStringByID("single_wait", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).queue();
                }
            } else {
                User u = event.getUser();

                try {
                    if(StaticStore.timeLimit.containsKey(u.getId()) && StaticStore.timeLimit.get(u.getId()).containsKey(StaticStore.COMMAND_FORMIMAGE_ID)) {
                        long time = StaticStore.timeLimit.get(u.getId()).get(StaticStore.COMMAND_FORMIMAGE_ID);

                        if(System.currentTimeMillis() - time > 10000) {
                            EntityHandler.generateFormImage(f, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

                            StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                        } else {
                            ch.sendMessage(LangID.getStringByID("command_timelimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).queue();
                        }
                    } else if(StaticStore.timeLimit.containsKey(u.getId())) {
                        EntityHandler.generateFormImage(f, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

                        StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());
                    } else {
                        EntityHandler.generateFormImage(f, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FORMIMAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(u.getId(), memberLimit);
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormAnimMessageHolder::onSelected - Failed to generate form image");
                }
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormAnimMessageHolder::onSelected - Failed to handle form image/animation");
        }

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}

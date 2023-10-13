package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrintStageEvent extends ConstraintCommand {
    public PrintStageEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        boolean now = isNow(loader.getContent());
        int t = 0;

        if(now) {
            User u = loader.getUser();

            t = StaticStore.timeZones.getOrDefault(u.getId(), 0);

            String content;

            if(t >= 0)
                content = "+" + t;
            else
                content = String.valueOf(t);

            ch.sendMessage(LangID.getStringByID("printevent_time", lang).replace("_", content)).queue();
        }

        boolean full = isFull(loader.getContent());

        User u = loader.getUser();

        if(full && !StaticStore.contributors.contains(u.getId())) {
            full = false;

            createMessageWithNoPings(ch, LangID.getStringByID("event_ignorefull", lang));
        }

        Map<EventFactor.SCHEDULE, List<String>> stage = StaticStore.event.printStageEvent(getLocale(loader.getContent()), lang, full, isRaw(loader.getContent()), now, t);

        if(stage.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("chevent_noup", lang)).queue();

            return;
        }

        boolean started = false;

        for(EventFactor.SCHEDULE type : stage.keySet()) {
            List<String> data = stage.get(type);

            if(data == null || data.isEmpty())
                continue;

            boolean initial = false;

            while(!data.isEmpty()) {
                StringBuilder builder = new StringBuilder();

                if(!started) {
                    started = true;

                    builder.append(LangID.getStringByID("event_stage", lang)).append("\n\n");
                }

                if(!initial) {
                    initial = true;

                    builder.append(builder.length() == 0 ? "** **\n" : "");

                    switch (type) {
                        case DAILY ->
                                builder.append(LangID.getStringByID("printstage_daily", lang)).append("\n\n```ansi\n");
                        case WEEKLY ->
                                builder.append(LangID.getStringByID("printstage_weekly", lang)).append("\n\n```ansi\n");
                        case MONTHLY ->
                                builder.append(LangID.getStringByID("printstage_monthly", lang)).append("\n\n```ansi\n");
                        case YEARLY ->
                                builder.append(LangID.getStringByID("printstage_yearly", lang)).append("\n\n```ansi\n");
                        case MISSION ->
                                builder.append(LangID.getStringByID("event_mission", lang)).append("\n\n```ansi\n");
                        default -> builder.append("```ansi\n");
                    }
                } else {
                    builder.append("```ansi\n");
                }

                while(builder.length() < 1950 && !data.isEmpty()) {
                    String line = data.get(0);

                    if(line.length() > 1950) {
                        data.remove(0);

                        continue;
                    }

                    if(builder.length() + line.length() > 1950)
                        break;

                    builder.append(line).append("\n");

                    if(type == EventFactor.SCHEDULE.MISSION)
                        builder.append("\n");

                    data.remove(0);
                }

                builder.append("```");

                ch.sendMessage(builder.toString())
                        .setAllowedMentions(new ArrayList<>())
                        .queue();
            }
        }
    }

    private int getLang() {
        if(lang >= 1 && lang < 4) {
            return lang;
        } else {
            return 0;
        }
    }

    private int getLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-en" -> {
                    return LangID.EN;
                }
                case "-tw" -> {
                    return LangID.ZH;
                }
                case "-kr" -> {
                    return LangID.KR;
                }
                case "-jp" -> {
                    return LangID.JP;
                }
            }
        }

        return getLang();
    }

    private boolean isFull(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-f") || contents[i].equals("-full"))
                return true;
        }

        return false;
    }

    private boolean isRaw(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-r") || contents[i].equals("-raw"))
                return true;
        }

        return false;
    }

    private boolean isNow(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if (contents[i].equals("-n") || contents[i].equals("-now")) {
                return true;
            }
        }

        return false;
    }
}

package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrintEvent extends ConstraintCommand {
    public PrintEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        int loc = getLocale(loader.getContent());
        int l = followServerLocale(loader.getContent()) ? holder.config.lang : lang;
        boolean full = isFull(loader.getContent());
        boolean raw = isRaw(loader.getContent());

        Member m = loader.getMember();

        if(full && !StaticStore.contributors.contains(m.getId())) {
            full = false;

            createMessageWithNoPings(ch, LangID.getStringByID("event_ignorefull", lang));
        }

        List<String> gacha = StaticStore.event.printGachaEvent(loc, l , full, raw, false, 0);
        List<String> item = StaticStore.event.printItemEvent(loc, l, full, raw, false, 0);
        Map<EventFactor.SCHEDULE, List<String>> stage = StaticStore.event.printStageEvent(loc, l, false, holder.eventRaw, false, 0);

        if(gacha.isEmpty() && item.isEmpty() && stage.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("chevent_noup", lang)).queue();

            return;
        }

        boolean done = false;
        boolean eventDone = false;

        for(int j = 0; j < 3; j++) {
            if(j == EventFactor.SALE) {
                if(stage.isEmpty())
                    continue;

                boolean wasDone = done;

                done = true;

                if(!eventDone) {
                    eventDone = true;
                    ch.sendMessage(LangID.getStringByID("event_loc"+loc, l)).queue();
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

                            if(wasDone) {
                                builder.append("** **\n");
                            }

                            builder.append(LangID.getStringByID("event_stage", l)).append("\n\n");
                        }

                        if(!initial) {
                            initial = true;

                            builder.append(builder.length() == 0 ? "** **\n" : "");

                            switch (type) {
                                case DAILY ->
                                        builder.append(LangID.getStringByID("printstage_daily", l)).append("\n\n```ansi\n");
                                case WEEKLY ->
                                        builder.append(LangID.getStringByID("printstage_weekly", l)).append("\n\n```ansi\n");
                                case MONTHLY ->
                                        builder.append(LangID.getStringByID("printstage_monthly", l)).append("\n\n```ansi\n");
                                case YEARLY ->
                                        builder.append(LangID.getStringByID("printstage_yearly", l)).append("\n\n```ansi\n");
                                case MISSION ->
                                        builder.append(LangID.getStringByID("event_mission", l)).append("\n\n```ansi\n");
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

                            if (type == EventFactor.SCHEDULE.MISSION)
                                builder.append("\n");

                            data.remove(0);
                        }

                        builder.append("```");

                        ch.sendMessage(builder.toString())
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            } else {
                List<String> result;

                if(j == EventFactor.GATYA)
                    result = gacha;
                else
                    result = item;

                if(result.isEmpty())
                    continue;

                boolean wasDone = done;

                done = true;

                if(!eventDone) {
                    eventDone = true;

                    ch.sendMessage(LangID.getStringByID("event_loc"+loc, l)).queue();
                }

                boolean started = false;

                while(!result.isEmpty()) {
                    StringBuilder builder = new StringBuilder();

                    if(!started) {
                        started = true;

                        if(wasDone) {
                            builder.append("** **\n");
                        }

                        if(j == EventFactor.GATYA) {
                            builder.append(LangID.getStringByID("event_gacha", l)).append("\n\n");
                        } else {
                            builder.append(LangID.getStringByID("event_item", l)).append("\n\n");
                        }
                    }

                    builder.append("```ansi\n");

                    while(builder.length() < (j == EventFactor.GATYA ? 1800 : 1950) && !result.isEmpty()) {
                        String line = result.get(0);

                        if(line.length() > 1950) {
                            result.remove(0);

                            continue;
                        }

                        if(builder.length() + line.length() > (j == EventFactor.GATYA ? 1800 : 1950))
                            break;

                        builder.append(line).append("\n");

                        result.remove(0);
                    }

                    if(result.isEmpty() && j == EventFactor.GATYA) {
                        builder.append("\n")
                                .append(LangID.getStringByID("printgacha_g", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_gua", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_s", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_step", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_l", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_lucky", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_p", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_plat", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_n", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_neneko", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_gr", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_gran", l))
                                .append(" | ")
                                .append(LangID.getStringByID("printgacha_r", l))
                                .append(" : ")
                                .append(LangID.getStringByID("printgacha_rein", l))
                                .append("\n```");
                    } else {
                        builder.append("```");
                    }

                    ch.sendMessage(builder.toString())
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
        }

        ch.sendMessage(LangID.getStringByID("event_warning", l)).queue();
    }

    private boolean followServerLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-server")) {
                return true;
            }
        }

        return false;
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
}

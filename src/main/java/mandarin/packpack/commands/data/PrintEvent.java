package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrintEvent extends ConstraintCommand {
    public PrintEvent(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        CommonStatic.Lang.Locale loc = getLocale(loader.getContent());
        CommonStatic.Lang.Locale l = followServerLocale(loader.getContent()) ? holder.config.lang : lang;
        boolean full = isFull(loader.getContent());
        boolean raw = isRaw(loader.getContent());

        Member m = loader.getMember();

        if(full && !StaticStore.contributors.contains(m.getId())) {
            full = false;

            createMessageWithNoPings(ch, LangID.getStringByID("event.ignoreFull", lang));
        }

        List<String> gacha = StaticStore.event.printGachaEvent(loc, l , full, raw, false, 0);
        List<String> item = StaticStore.event.printItemEvent(loc, l, full, raw, false, 0);
        Map<EventFactor.SCHEDULE, List<String>> stage = StaticStore.event.printStageEvent(loc, l, false, holder.eventRaw, false, 0);

        if(gacha.isEmpty() && item.isEmpty() && stage.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("checkEvent.noEvent", lang)).queue();

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
                    ch.sendMessage(LangID.getStringByID("event.title." + loc.code, l)).queue();
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

                            builder.append(LangID.getStringByID("event.section.stage", l)).append("\n\n");
                        }

                        if(!initial) {
                            initial = true;

                            builder.append(builder.isEmpty() ? "** **\n" : "");

                            switch (type) {
                                case DAILY ->
                                        builder.append(LangID.getStringByID("event.permanentSchedule.daily", l)).append("\n\n```ansi\n");
                                case WEEKLY ->
                                        builder.append(LangID.getStringByID("event.permanentSchedule.weekly", l)).append("\n\n```ansi\n");
                                case MONTHLY ->
                                        builder.append(LangID.getStringByID("event.permanentSchedule.monthly", l)).append("\n\n```ansi\n");
                                case YEARLY ->
                                        builder.append(LangID.getStringByID("event.permanentSchedule.yearly", l)).append("\n\n```ansi\n");
                                case MISSION ->
                                        builder.append(LangID.getStringByID("event.section.mission", l)).append("\n\n```ansi\n");
                                default -> builder.append("```ansi\n");
                            }
                        } else {
                            builder.append("```ansi\n");
                        }

                        while(builder.length() < 1950 && !data.isEmpty()) {
                            String line = data.getFirst();

                            if(line.length() > 1950) {
                                data.removeFirst();

                                continue;
                            }

                            if(builder.length() + line.length() > 1950)
                                break;

                            builder.append(line).append("\n");

                            if (type == EventFactor.SCHEDULE.MISSION)
                                builder.append("\n");

                            data.removeFirst();
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

                    ch.sendMessage(LangID.getStringByID("event.title." + loc.code, l)).queue();
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
                            builder.append(LangID.getStringByID("event.section.gacha", l)).append("\n\n");
                        } else {
                            builder.append(LangID.getStringByID("event.section.item", l)).append("\n\n");
                        }
                    }

                    builder.append("```ansi\n");

                    while(builder.length() < (j == EventFactor.GATYA ? 1800 : 1950) && !result.isEmpty()) {
                        String line = result.getFirst();

                        if(line.length() > 1950) {
                            result.removeFirst();

                            continue;
                        }

                        if(builder.length() + line.length() > (j == EventFactor.GATYA ? 1800 : 1950))
                            break;

                        builder.append(line).append("\n");

                        result.removeFirst();
                    }

                    if(result.isEmpty() && j == EventFactor.GATYA) {
                        builder.append("\n")
                                .append(LangID.getStringByID("event.gachaCode.guaranteed.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.guaranteed.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.stepUp.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.stepUp.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.luckyTicket.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.luckyTicket.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.platinumShard.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.platinumShard.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.nenekoGang.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.nenekoGang.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.grandon.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.grandon.fullName", l))
                                .append(" | ")
                                .append(LangID.getStringByID("event.gachaCode.reinforcement.code", l))
                                .append(" : ")
                                .append(LangID.getStringByID("event.gachaCode.reinforcement.fullName", l))
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

        ch.sendMessage(LangID.getStringByID("event.warning", l)).queue();
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

    private CommonStatic.Lang.Locale getLang() {
        int index = ArrayUtils.indexOf(EventFactor.supportedVersions, lang);

        if(index != -1) {
            return lang;
        } else {
            return CommonStatic.Lang.Locale.EN;
        }
    }

    private CommonStatic.Lang.Locale getLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-en" -> {
                    return CommonStatic.Lang.Locale.EN;
                }
                case "-tw" -> {
                    return CommonStatic.Lang.Locale.ZH;
                }
                case "-kr" -> {
                    return CommonStatic.Lang.Locale.KR;
                }
                case "-jp" -> {
                    return CommonStatic.Lang.Locale.JP;
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

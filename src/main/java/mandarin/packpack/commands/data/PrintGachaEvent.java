package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PrintGachaEvent extends ConstraintCommand {
    public PrintGachaEvent(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }


    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
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

            ch.sendMessage(LangID.getStringByID("printEvent.timeZone", lang).replace("_", content)).queue();
        }

        boolean full = isFull(loader.getContent());

        User u = loader.getUser();

        if(full && !StaticStore.contributors.contains(u.getId())) {
            full = false;

            createMessageWithNoPings(ch, LangID.getStringByID("event.ignoreFull", lang));
        }

        List<String> result = StaticStore.event.printGachaEvent(getLocale(loader.getContent()), lang, full, isRaw(loader.getContent()), now, t);

        if(result.isEmpty()) {
            ch.sendMessage(LangID.getStringByID("checkEvent.noEvent", lang)).queue();

            return;
        }

        boolean started = false;

        while(!result.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            if(!started) {
                started = true;

                builder.append(LangID.getStringByID("event.section.gacha", lang)).append("\n\n");
            }

            builder.append("```ansi\n");

            while(builder.length() < 1800 && !result.isEmpty()) {
                String line = result.getFirst();

                if(line.length() > 1950) {
                    result.removeFirst();

                    continue;
                }

                if(builder.length() + line.length() > 1800)
                    break;

                builder.append(line).append("\n");

                result.removeFirst();
            }

            if(result.isEmpty()) {
                builder.append("\n").append(EventFactor.getGachaCodeExplanation(lang)).append("\n```");
            } else {
                builder.append("```");
            }

            ch.sendMessage(builder.toString())
                    .setAllowedMentions(new ArrayList<>())
                    .queue();
        }
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

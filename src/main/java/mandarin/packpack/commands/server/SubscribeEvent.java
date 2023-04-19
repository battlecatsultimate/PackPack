package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.*;

public class SubscribeEvent extends ConstraintCommand {
    public SubscribeEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        List<Integer> locales = getLocales(getContent(event).replaceAll("[ ]+,[ ]+|,[ ]+|[ ]+,", ","));

        if(locales.isEmpty()) {
            replyToMessageSafely(ch, LangID.getStringByID("subevent_noloc", lang), getMessage(event), a -> a);

            return;
        }

        String channel = getChannelID(getContent(event));

        StringBuilder result = new StringBuilder();

        if(channel == null && removable(locales, holder.eventMap)) {
            for(int i : locales) {
                if(holder.eventMap.containsKey(i)) {
                    holder.eventMap.remove(i);

                    result.append(getLocaleFrom(i)).append(LangID.getStringByID("subevent_remove", lang)).append("\n\n");
                } else {
                    result.append(getLocaleFrom(i)).append(LangID.getStringByID("subevent_notassigned", lang)).append("\n\n");
                }
            }

            StaticStore.idHolder.put(g.getId(), holder);

            replyToMessageSafely(ch, LangID.getStringByID("subevent_set", lang) + result, getMessage(event), a -> a);

            return;
        } else if(channel == null) {
            ch.sendMessage(LangID.getStringByID("watdm_nochan", lang)).queue();

            return;
        } else if(!StaticStore.isNumeric(channel)) {
            ch.sendMessage(LangID.getStringByID("watdm_nonum" ,lang)).queue();

            return;
        } else if(!isValidChannel(g, channel)) {
            ch.sendMessage(LangID.getStringByID("watdm_invalid", lang)).queue();

            return;
        }

        holder.eventRaw = isRaw(getContent(event));

        for(int i : locales) {
            String previous = holder.eventMap.put(i, channel);

            if(previous != null) {
                result.append(getLocaleFrom(i)).append(String.format(LangID.getStringByID("subevent_replace", lang), previous, previous, channel, channel)).append("\n\n");
            } else {
                result.append(getLocaleFrom(i)).append(String.format(LangID.getStringByID("subevent_add", lang), channel, channel)).append("\n\n");
            }
        }

        replyToMessageSafely(ch, LangID.getStringByID("subevent_set", lang) + result, getMessage(event), a -> a);

        StaticStore.idHolder.put(g.getId(), holder);
    }

    private String getChannelID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].matches("\\d+")) {
                return contents[i];
            } else if(contents[i].matches("<#\\d+>")) {
                return contents[i].replace("<#", "").replace(">", "");
            }
        }

        return null;
    }

    private boolean isValidChannel(Guild g, String id) {
        List<GuildChannel> channels = g.getChannels();

        for(GuildChannel gc : channels) {
            if((gc.getType() == ChannelType.TEXT || gc.getType() == ChannelType.NEWS) && id.equals(gc.getId())) {
                return true;
            }
        }

        return false;
    }

    private List<Integer> getLocales(String content) {
        Set<Integer> set = new HashSet<>();

        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-en":
                    set.add(LangID.EN);
                    break;
                case "-tw":
                    set.add(LangID.ZH);
                    break;
                case "-kr":
                    set.add(LangID.KR);
                    break;
                case "-jp":
                    set.add(LangID.JP);
                    break;
            }
        }

        return new ArrayList<>(set);
    }

    private boolean isRaw(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-r") || contents[i].equals("-raw"))
                return true;
        }

        return false;
    }

    private boolean removable(List<Integer> locales, Map<Integer, String> map) {
        for(int i : locales) {
            if(map.containsKey(i))
                return true;
        }

        return false;
    }

    private String getLocaleFrom(int loc) {
        switch (loc) {
            case LangID.ZH:
                return LangID.getStringByID("subevent_zh", lang);
            case LangID.KR:
                return LangID.getStringByID("subevent_kr", lang);
            case LangID.JP:
                return LangID.getStringByID("subevent_jp", lang);
            default:
                return LangID.getStringByID("subevent_en", lang);
        }
    }
}

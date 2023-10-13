package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.*;

public class SubscribeEvent extends ConstraintCommand {
    public SubscribeEvent(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        List<Integer> locales = getLocales(loader.getContent().replaceAll(" +, +|, +| +,", ","));

        if(locales.isEmpty()) {
            replyToMessageSafely(ch, LangID.getStringByID("subevent_noloc", lang), loader.getMessage(), a -> a);

            return;
        }

        String channel = getChannelID(loader.getContent());

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

            replyToMessageSafely(ch, LangID.getStringByID("subevent_set", lang) + result, loader.getMessage(), a -> a);

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

        holder.eventRaw = isRaw(loader.getContent());

        for(int i : locales) {
            String previous = holder.eventMap.put(i, channel);

            if(previous != null) {
                result.append(getLocaleFrom(i)).append(String.format(LangID.getStringByID("subevent_replace", lang), previous, previous, channel, channel)).append("\n\n");
            } else {
                result.append(getLocaleFrom(i)).append(String.format(LangID.getStringByID("subevent_add", lang), channel, channel)).append("\n\n");
            }
        }

        replyToMessageSafely(ch, LangID.getStringByID("subevent_set", lang) + result, loader.getMessage(), a -> a);

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
                case "-en" -> set.add(LangID.EN);
                case "-tw" -> set.add(LangID.ZH);
                case "-kr" -> set.add(LangID.KR);
                case "-jp" -> set.add(LangID.JP);
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
        return switch (loc) {
            case LangID.ZH -> LangID.getStringByID("subevent_zh", lang);
            case LangID.KR -> LangID.getStringByID("subevent_kr", lang);
            case LangID.JP -> LangID.getStringByID("subevent_jp", lang);
            default -> LangID.getStringByID("subevent_en", lang);
        };
    }
}

package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        String channel = getChannelID(getContent(event));

        if(channel == null && holder.event != null) {
            holder.event = null;
            holder.eventLocale.clear();
            StaticStore.idHolder.put(g.getId(), holder);

            ch.sendMessage(LangID.getStringByID("subevent_remove", lang)).queue();

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

        List<Integer> locales = getLocales(getContent(event).replaceAll("[ ]+,[ ]+|,[ ]+|[ ]+,", ","));

        if(locales.isEmpty()) {
            locales.add(filterLocale());
        }

        holder.eventLocale.clear();

        for(int i = 0; i < locales.size(); i++) {
            int l = locales.get(i);

            if(l >= 0 && l <= 3) {
                holder.eventLocale.add(l);
            }
        }

        holder.eventRaw = isRaw(getContent(event));

        holder.eventLocale.sort(Integer::compare);

        holder.event = channel;

        StaticStore.idHolder.put(g.getId(), holder);

        ch.sendMessage(LangID.getStringByID("subevent_set", lang).replace("_CCC_", channel).replace("_BC_", getBCList(holder.eventLocale))).queue();
    }

    private String getChannelID(String content) {
        String[] contents = content.split(" ");

        if(contents.length >= 2) {
            return contents[1].replace("<#", "").replace(">", "");
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
        List<Integer> result = new ArrayList<>();

        String[] contents = content.split(" ");

        if(contents.length >= 3) {
            String[] numbers = contents[2].split(",");

            for(int i = 0; i < numbers.length; i++) {
                if(!StaticStore.isNumeric(numbers[i]))
                    continue;

                int loc = StaticStore.safeParseInt(numbers[i]) - 1;

                if(loc >= 0 && loc <= 3 && !result.contains(loc)) {
                    result.add(loc);
                }
            }
        }

        return result;
    }

    private int filterLocale() {
        switch (Objects.requireNonNull(holder).config.lang) {
            case LangID.ZH:
                return LangID.ZH;
            case LangID.KR:
                return LangID.KR;
            case LangID.JP:
                return LangID.JP;
            default:
                return LangID.EN;
        }
    }

    private String getBCList(List<Integer> locales) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < locales.size(); i++) {
            switch (locales.get(i)) {
                case LangID.EN:
                    builder.append(LangID.getStringByID("subevent_en", lang));
                    break;
                case LangID.ZH:
                    builder.append(LangID.getStringByID("subevent_zh", lang));
                    break;
                case LangID.KR:
                    builder.append(LangID.getStringByID("subevent_kr", lang));
                    break;
                case LangID.JP:
                    builder.append(LangID.getStringByID("subevent_jp", lang));
                    break;
            }

            if(i < locales.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
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

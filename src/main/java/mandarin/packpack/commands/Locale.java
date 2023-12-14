package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class Locale extends ConstraintCommand {

    public Locale(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length == 2) {
            if(StaticStore.isNumeric(list[1])) {
                int lan = StaticStore.safeParseInt(list[1]) - 1;

                if(lan >= 0 && lan <= StaticStore.langIndex.length - 1) {
                    int loc = StaticStore.langIndex[lan];

                    User u = loader.getUser();

                    ConfigHolder holder;

                    if(StaticStore.config.containsKey(u.getId()))
                        holder = StaticStore.config.get(u.getId());
                    else
                        holder = new ConfigHolder();

                    holder.lang = loc;

                    StaticStore.config.put(u.getId(), holder);

                    String locale = switch (loc) {
                        case LangID.EN -> LangID.getStringByID("lang_en", loc);
                        case LangID.JP -> LangID.getStringByID("lang_jp", loc);
                        case LangID.KR -> LangID.getStringByID("lang_kr", loc);
                        case LangID.ZH -> LangID.getStringByID("lang_zh", loc);
                        case LangID.FR -> LangID.getStringByID("lang_fr", loc);
                        case LangID.IT -> LangID.getStringByID("lang_it", loc);
                        case LangID.ES -> LangID.getStringByID("lang_es", loc);
                        case LangID.DE -> LangID.getStringByID("lang_de", loc);
                        default -> LangID.getStringByID("lang_th", loc);
                    };

                    replyToMessageSafely(ch, LangID.getStringByID("locale_set", lan).replace("_", locale), loader.getMessage(), a -> a);
                } else if(lan == -1) {
                    User u = loader.getUser();

                    if (StaticStore.config.containsKey(u.getId())) {
                        ConfigHolder holder = StaticStore.config.get(u.getId());

                        holder.lang = -1;

                        StaticStore.config.put(u.getId(), holder);
                    }

                    if (ch instanceof GuildChannel) {
                        Guild g = loader.getGuild();

                        IDHolder holder = StaticStore.idHolder.get(g.getId());

                        if(holder != null) {
                            replyToMessageSafely(ch, LangID.getStringByID("locale_auto", holder.config.lang), loader.getMessage(), a -> a);
                        } else {
                            replyToMessageSafely(ch, LangID.getStringByID("locale_auto", lang), loader.getMessage(), a -> a);
                        }
                    } else {
                        replyToMessageSafely(ch, LangID.getStringByID("locale_auto", lang), loader.getMessage(), a -> a);
                    }
                } else {
                    replyToMessageSafely(ch, LangID.getStringByID("locale_incorrect", lan), loader.getMessage(), a -> a);
                }
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("locale_number", lang), loader.getMessage(), a -> a);
            }
        } else {
            replyToMessageSafely(ch, LangID.getStringByID("locale_argu", lang), loader.getMessage(), a -> a);
        }
    }
}

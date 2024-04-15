package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveStatusChannel extends ConstraintCommand {
    public RemoveStatusChannel(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        if(holder == null)
            return;

        String content = loader.getContent();

        String[] contents = content.split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("statuschan_channel", lang), loader.getMessage(), a -> a);

            return;
        }

        Pattern pattern = Pattern.compile("(\\d+|<#\\d+>)");
        Matcher matcher = pattern.matcher(content);

        StringBuilder builder = new StringBuilder();

        while(matcher.find()) {
            builder.append(matcher.group()).append(",");
        }

        if(builder.isEmpty()) {
            replyToMessageSafely(ch, LangID.getStringByID("statuschan_channel", lang), loader.getMessage(), a -> a);

            return;
        }

        String[] filteredID = builder.substring(0, builder.length() - 1).split(",");

        StringBuilder result = new StringBuilder("---------- RESULTS ----------\n\n");

        for(int i = 0; i < filteredID.length; i++) {
            try {
                String filtered = filteredID[i];

                if(filtered.startsWith("<#") && filtered.endsWith(">"))
                    filtered = filtered.replace("<#", "").replace(">", "");

                if(!StaticStore.isNumeric(filtered))
                    continue;

                GuildChannel channel = g.getGuildChannelById(filtered);

                if(!(channel instanceof MessageChannel)) {
                    result.append(String.format(LangID.getStringByID("statuschan_invalid", lang), filteredID[i])).append("\n");
                } else if(!holder.status.contains(filtered)) {
                    result.append(String.format(LangID.getStringByID("statuschan_remalready", lang), filteredID[i], filtered)).append("\n");
                } else {
                    holder.status.remove(filtered);

                    result.append(String.format(LangID.getStringByID("statuschan_removed", lang), filteredID[i], filtered)).append("\n");
                }
            } catch (Exception ignored) {
                result.append(String.format(LangID.getStringByID("statuschan_invalid", lang), filteredID[i])).append("\n");
            }
        }

        result.append("\n---------- RESULTS ----------");

        replyToMessageSafely(ch, result.toString(), loader.getMessage(), a -> a);
    }
}

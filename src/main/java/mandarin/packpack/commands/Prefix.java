package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class Prefix extends ConstraintCommand {
    private static final int ERR_CANT_FIND_MEMBER = 0;

    public Prefix(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("prefix_space", lang), loader.getMessage(), a -> a);

                return;
            }

            if(list[1].matches("(.+)?http(s)?://(.+)?")) {
                replyToMessageSafely(ch, LangID.getStringByID("prefix_nourl", lang), loader.getMessage(), a -> a);

                return;
            }

            User u = loader.getUser();

            StaticStore.prefix.put(u.getId(), list[1]);

            String result = String.format(LangID.getStringByID("prefix_set", lang), list[1]);

            if(result.length() < 2000) {
                replyToMessageSafely(ch, result, loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("prefix_setnone", lang), loader.getMessage(), a -> a);
            }
        } else if(list.length == 1) {
            replyToMessageSafely(ch, LangID.getStringByID("prefix_tooarg", lang), loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(ch, LangID.getStringByID("prefix_tooarg", lang), loader.getMessage(), a -> a);
        }
    }

    @Override
    public void onFail(CommandLoader loader, int error) {
        StaticStore.executed--;

        MessageChannel ch = loader.getChannel();

        switch (error) {
            case DEFAULT_ERROR, SERVER_ERROR -> ch.sendMessage("`INTERNAL_ERROR`").queue();
            case ERR_CANT_FIND_MEMBER -> ch.sendMessage("Couldn't get member info").queue();
        }
    }
}

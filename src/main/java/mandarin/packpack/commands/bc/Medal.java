package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.MedalMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Medal extends ConstraintCommand {
    private final ConfigHolder config;

    public Medal(ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("medal.failed.noParameter", lang));

            return;
        }

        String[] realContents = loader.getContent().split(" ", 2);

        ArrayList<Integer> id = EntityFilter.findMedalByName(realContents[1], lang);

        if(id.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("medal.failed.noEnemy", lang).formatted(getSearchKeyword(realContents[1])));
        } else if(id.size() == 1) {
            EntityHandler.generateMedalEmbed(id.getFirst(), ch, loader.getMessage(), lang);
        } else {
            replyToMessageSafely(ch, loader.getMessage(), msg -> {
                User u = loader.getUser();

                StaticStore.putHolder(u.getId(), new MedalMessageHolder(id, loader.getMessage(), u.getId(), ch.getId(), msg, realContents[1], config.searchLayout, lang));
            }, getSearchComponents(id.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(realContents[1], id.size()), id, this::accumulateTextData, config.searchLayout, lang));
        }
    }

    public List<String> accumulateTextData(List<Integer> id, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(id.size(), config.searchLayout.chunkSize) ; i++) {
            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(id.get(i));

                        String name = StaticStore.MEDNAME.getCont(id.get(i), lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(id.get(i)) + "`";

                        String name = StaticStore.MEDNAME.getCont(id.get(i), lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(id.get(i));
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.MEDNAME.getCont(id.get(i), lang);

                    if (text == null) {
                        text = Data.trio(id.get(i));
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(id.get(i));
            }

            data.add(text);

            String medalName = Data.trio(id.get(i)) + " " + StaticStore.MEDNAME.getCont(id.get(i), lang);

            data.add(medalName);
        }

        return data;
    }

    private String getSearchKeyword(String command) {
        if(command.length() > 1500)
            command = command.substring(0, 1500) + "...";

        return command;
    }
}

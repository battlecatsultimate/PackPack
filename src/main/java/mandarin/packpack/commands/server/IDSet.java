package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.IDManagerHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IDSet extends ConstraintCommand {

    public IDSet(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();
        Guild g = loader.getGuild();

        if(holder == null)
            return;

        replyToMessageSafely(ch, generateIDData(g), loader.getMessage(), this::registerComponents, msg ->
                StaticStore.putHolder(u.getId(), new IDManagerHolder(loader.getMessage(), ch.getId(), msg, holder, g))
        );
    }

    private String generateIDData(Guild g) {
        if(holder == null)
            throw new IllegalStateException("E/IDSet::generateIDData - IDHolder is required to perform IDHolder");

        String[] data = { "moderator", "member", "booster" };
        String[] ids = { holder.MOD, holder.MEMBER, holder.BOOSTER };

        StringBuilder result = new StringBuilder();

        for(int i = 0; i < data.length; i++) {
            result.append("**")
                    .append(LangID.getStringByID("idset_" + data[i], lang))
                    .append("** : ");

            if(ids[i] == null) {
                result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
            } else {
                Role r = getRoleSafelyWithID(ids[i], g);

                if (r == null) {
                    result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
                } else {
                    result.append(r.getId())
                            .append(" [")
                            .append(r.getAsMention())
                            .append("]");
                }
            }

            if(i < data.length - 1) {
                result.append("\n\n");
            }
        }

        return result.toString();
    }

    private Role getRoleSafelyWithID(String id, Guild g) {
        try {
            return g.getRoleById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private MessageCreateAction registerComponents(MessageCreateAction m) {
        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

        return m.addComponents(
                ActionRow.of(
                        EntitySelectMenu.create("mod", EntitySelectMenu.SelectTarget.ROLE)
                                .setRequiredRange(1, 1)
                                .setPlaceholder(LangID.getStringByID("idset_modselect", lang))
                                .build()),
                ActionRow.of(
                        EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                .setRequiredRange(0, 1)
                                .setPlaceholder(LangID.getStringByID("idset_memberselect", lang))
                                .build()
                ),
                ActionRow.of(
                        EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                .setRequiredRange(0, 1)
                                .setPlaceholder(LangID.getStringByID("idset_boosterselect", lang))
                                .build()
                ),
                ActionRow.of(pages),
                ActionRow.of(Button.primary("confirm", LangID.getStringByID("button_confirm", lang)))
        );
    }
}

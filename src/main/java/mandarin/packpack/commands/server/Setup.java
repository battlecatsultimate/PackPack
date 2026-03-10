package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import mandarin.packpack.supporter.server.holder.component.SetupModButtonHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class Setup extends ConstraintCommand {
    public Setup(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        Guild g = loader.getGuild();
        MessageChannel ch = loader.getChannel();

        if(alreadySet(g)) {
            List<Button> components = new ArrayList<>();

            components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
            components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

            ch.sendMessage(LangID.getStringByID("setup.confirmation", lang))
                    .setComponents(ActionRow.of(components))
                    .queue(m -> {
                        if(m == null)
                            return;

                        Message author = loader.getMessage();
                        Member member = loader.getMember();

                        StaticStore.putHolder(member.getIdLong(), new ConfirmButtonHolder(author, member.getIdLong(), ch.getIdLong(), m, lang, () -> initializeSetup(ch, author)));
                    });
        } else {
            Message author = loader.getMessage();

            initializeSetup(ch, author);
        }
    }

    private void initializeSetup(MessageChannel ch, Message author) {
        MessageCreateAction action = ch.sendMessage(LangID.getStringByID("setup.select.moderator", lang));

        action = action.setComponents(
                ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setPlaceholder(LangID.getStringByID("setup.selectRole", lang)).setRequiredRange(1, 1).build()),
                ActionRow.of(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).asDisabled(), Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)))
        ).setAllowedMentions(new ArrayList<>());

        action.queue(m -> {
            if(m == null)
                return;

            StaticStore.putHolder(author.getAuthor().getIdLong(), new SetupModButtonHolder(author, author.getAuthor().getIdLong(), ch.getIdLong(), m, holder, lang));
        });
    }

    private boolean alreadySet(Guild g) {
        if(holder == null)
            throw new IllegalStateException("E/Setup::alreadySet - IDHolder must not be null");

        if(holder.moderator != -1L) {
            Role r = g.getRoleById(holder.moderator);

            if(r == null) {
                StaticStore.logger.uploadLog("W/Setup::alreadySet | Role was null");

                return false;
            }

            return !r.getName().equals("PackPackMod") || holder.member != -1L;
        } else {
            StaticStore.logger.uploadLog("Invalid ID holder data found, moderator role ID was null\nServer ID : "+g.getIdLong()+" | "+g.getName()+"\n-----ID Holder-----\n\n"+holder);
        }

        return false;
    }
}

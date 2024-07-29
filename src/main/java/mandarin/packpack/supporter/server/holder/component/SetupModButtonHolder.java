package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SetupModButtonHolder extends ComponentHolder {
    private final String channelID;
    private final String memberID;

    private final IDHolder holder;

    private String roleID;

    public SetupModButtonHolder(Message author, Message msg, String channelID, IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);
        
        this.channelID = channelID;
        this.memberID = author.getAuthor().getId();
        this.holder = holder;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = message.getChannel();

        switch (event.getComponentId()) {
            case "role" -> {
                EntitySelectInteractionEvent es = (EntitySelectInteractionEvent) event;

                if (es.getValues().size() != 1)
                    return;

                roleID = es.getValues().getFirst().getId();

                event.deferEdit()
                        .setContent(LangID.getStringByID("setup.selected.moderator", lang).replace("_RRR_", es.getValues().getFirst().getId()))
                        .setComponents(getComponents())
                        .setAllowedMentions(new ArrayList<>())
                        .queue();
            }
            case "confirm" -> Command.replyToMessageSafely(ch, LangID.getStringByID("setup.select.member", lang), message, a -> a.setComponents(
                    ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setPlaceholder(LangID.getStringByID("setup.selectRole", lang)).setRequiredRange(1, 1).build()),
                    ActionRow.of(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).asDisabled(), Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)))
            ), m -> {
                StaticStore.removeHolder(memberID, this);

                StaticStore.putHolder(memberID, new SetupMemberButtonHolder(m, getAuthorMessage(), channelID, holder, roleID, lang));

                event.deferEdit()
                        .setContent(LangID.getStringByID("setup.selected.moderator", lang).replace("_RRR_", roleID))
                        .setComponents()
                        .queue();

                end();
            });
            case "cancel" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("setup.canceled", lang))
                        .setComponents()
                        .queue();

                end();
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("setup.expired", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }

    private List<ActionRow> getComponents() {
        List<ActionRow> result = new ArrayList<>();

        result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setRequiredRange(1, 1).build()));

        Button confirm;

        if(roleID != null) {
            confirm = Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).asEnabled();
        } else {
            confirm = Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).asDisabled();
        }

        result.add(ActionRow.of(confirm, Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return result;
    }
}

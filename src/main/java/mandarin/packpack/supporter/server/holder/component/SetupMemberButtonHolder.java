package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class SetupMemberButtonHolder extends ComponentHolder {
    private final IDHolder holder;
    private final String modID;

    private String roleID;

    public SetupMemberButtonHolder(Message author, String userID, String channelID, Message message, IDHolder holder, String modID, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
        this.modID = modID;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = message.getChannel();
        
        Guild g = message.getGuild();

        switch (event.getComponentId()) {
            case "role" -> {
                EntitySelectInteractionEvent es = (EntitySelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                roleID = es.getValues().getFirst().getId();
                
                if (roleID.equals(modID)) {
                    roleID = null;

                    event.deferEdit()
                            .setContent(LangID.getStringByID("setup.already", lang).replace("_RRR_", es.getValues().getFirst().getId()))
                            .setComponents(getComponents(false))
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                } else {
                    event.deferEdit()
                            .setContent(LangID.getStringByID("setup.selected.member", lang).replace("_RRR_", es.getValues().getFirst().getId()))
                            .setComponents(getComponents(true))
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
            case "confirm" -> {
                holder.moderator = modID;
                holder.member = roleID;
                
                StaticStore.idHolder.put(g.getId(), holder);
                
                Command.replyToMessageSafely(ch, LangID.getStringByID("setup.done", lang).replace("_MOD_", modID).replace("_MEM_", roleID), message, a -> a);
                
                event.deferEdit()
                        .setContent(LangID.getStringByID("setup.selected.member", lang).replace("_RRR_", roleID))
                        .setComponents()
                        .queue();

                end(true);
            }
            case "cancel" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("setup.canceled", lang))
                        .setComponents()
                        .queue();

                end(true);
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

    private List<ActionRow> getComponents(boolean done) {
        List<ActionRow> result = new ArrayList<>();

        if(done) {
            result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setRequiredRange(1, 1).build()));
        } else {
            result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setPlaceholder(LangID.getStringByID("setup.selectRole", lang)).setRequiredRange(1, 1).build()));
        }

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

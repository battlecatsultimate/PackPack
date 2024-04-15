package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
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

public class SetupMemberButtonHolder extends ComponentHolder {
    private final Message msg;

    private final IDHolder holder;
    private final String modID;
    private final int lang;

    private String roleID;

    public SetupMemberButtonHolder(Message msg, Message author, String channelID, IDHolder holder, String modID, int lang) {
        super(author, channelID, msg.getId());

        this.msg = msg;

        this.holder = holder;
        this.modID = modID;
        this.lang = lang;
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();
        
        Guild g = msg.getGuild();

        switch (event.getComponentId()) {
            case "role" -> {
                EntitySelectInteractionEvent es = (EntitySelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                roleID = es.getValues().getFirst().getId();
                
                if (roleID.equals(modID)) {
                    roleID = null;

                    event.deferEdit()
                            .setContent(LangID.getStringByID("setup_already", lang).replace("_RRR_", es.getValues().getFirst().getId()))
                            .setComponents(getComponents(false))
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                } else {
                    event.deferEdit()
                            .setContent(LangID.getStringByID("setup_memsele", lang).replace("_RRR_", es.getValues().getFirst().getId()))
                            .setComponents(getComponents(true))
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
            case "confirm" -> {
                expired = true;
                
                StaticStore.removeHolder(userID, this);
                
                holder.MOD = modID;
                holder.MEMBER = roleID;
                
                StaticStore.idHolder.put(g.getId(), holder);
                
                Command.replyToMessageSafely(ch, LangID.getStringByID("setup_done", lang).replace("_MOD_", modID).replace("_MEM_", roleID), msg, a -> a);
                
                event.deferEdit()
                        .setContent(LangID.getStringByID("setup_memsele", lang).replace("_RRR_", roleID))
                        .setComponents()
                        .queue();
            }
            case "cancel" -> {
                expired = true;
                
                StaticStore.removeHolder(userID, this);
                
                event.deferEdit()
                        .setContent(LangID.getStringByID("setup_cancel", lang))
                        .setComponents()
                        .queue();
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("setup_expire", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }

    private List<ActionRow> getComponents(boolean done) {
        List<ActionRow> result = new ArrayList<>();

        if(done) {
            result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setRequiredRange(1, 1).build()));
        } else {
            result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE).setPlaceholder(LangID.getStringByID("setup_select", lang)).setRequiredRange(1, 1).build()));
        }

        Button confirm;

        if(roleID != null) {
            confirm = Button.success("confirm", LangID.getStringByID("button_confirm", lang)).asEnabled();
        } else {
            confirm = Button.success("confirm", LangID.getStringByID("button_confirm", lang)).asDisabled();
        }

        result.add(ActionRow.of(confirm, Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

        return result;
    }
}

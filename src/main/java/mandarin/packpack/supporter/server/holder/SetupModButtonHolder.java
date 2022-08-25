package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SetupModButtonHolder extends InteractionHolder<GenericComponentInteractionCreateEvent> {
    private final Message msg;
    private final String channelID;
    private final String memberID;
    private final int lang;

    private final IDHolder holder;

    private String roleID;

    public SetupModButtonHolder(Message msg, Message author, String channelID, IDHolder holder, int lang) {
        super(GenericComponentInteractionCreateEvent.class, author);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = author.getAuthor().getId();
        this.lang = lang;

        this.holder = holder;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), SetupModButtonHolder.this);

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();

        if(!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member m = event.getInteraction().getMember();

        if(!m.getId().equals(memberID))
            return RESULT_STILL;

        Message me = event.getMessage();

        if(!me.getId().equals(msg.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();
        Guild g = msg.getGuild();

        switch (event.getComponentId()) {
            case "role":
                SelectMenuInteractionEvent es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                roleID = es.getValues().get(0);

                event.deferEdit()
                        .setContent(LangID.getStringByID("setup_modsele", lang).replace("_RRR_", es.getValues().get(0)))
                        .setComponents(getComponents(g))
                        .queue();

                break;
            case "confirm":
                List<Role> roles = g.getRoles();

                List<SelectOption> options = new ArrayList<>();

                for(int i = 0; i < roles.size(); i++) {
                    if(roles.get(i).isPublicRole() || roles.get(i).isManaged())
                        continue;

                    if(options.size() == 25)
                        break;

                    options.add(SelectOption.of(roles.get(i).getName(), roles.get(i).getId()).withDescription(roles.get(i).getId()));
                }

                Message m = ch.sendMessage(LangID.getStringByID("setup_mem", lang))
                        .setMessageReference(msg)
                        .setAllowedMentions(new ArrayList<>())
                        .setComponents(
                                ActionRow.of(SelectMenu.create("role").addOptions(options).setPlaceholder(LangID.getStringByID("setup_select", lang)).build()),
                                ActionRow.of(Button.success("confirm", LangID.getStringByID("button_confirm", lang)).asDisabled(), Button.danger("cancel", LangID.getStringByID("button_cancel", lang)))
                        ).complete();

                expired = true;

                StaticStore.removeHolder(memberID, this);

                StaticStore.putHolder(memberID, new SetupMemberButtonHolder(m, getAuthorMessage(), memberID, holder, roleID, lang));

                event.deferEdit()
                        .setContent(LangID.getStringByID("setup_modsele", lang).replace("_RRR_", roleID))
                        .setComponents()
                        .queue();

                break;
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                event.deferEdit()
                        .setContent(LangID.getStringByID("setup_cancel", lang))
                        .setComponents()
                        .queue();

                break;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("setup_expire", lang))
                .setComponents()
                .queue();
    }

    private List<ActionRow> getComponents(Guild g) {
        List<Role> roles = g.getRoles();

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < roles.size(); i++) {
            if(roles.get(i).isPublicRole() || roles.get(i).isManaged())
                continue;

            if(options.size() == 25)
                break;

            if(roleID != null && roles.get(i).getId().equals(roleID)) {
                options.add(SelectOption.of(roles.get(i).getName(), roles.get(i).getId()).withDescription(roles.get(i).getId()).withDefault(true));
            } else {
                options.add(SelectOption.of(roles.get(i).getName(), roles.get(i).getId()).withDescription(roles.get(i).getId()));
            }
        }

        List<ActionRow> result = new ArrayList<>();

        result.add(ActionRow.of(SelectMenu.create("role").addOptions(options).setPlaceholder(LangID.getStringByID("setup_select", lang)).build()));

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

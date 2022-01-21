package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.AllowedMentionsData;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SetupMemberButtonHolder extends InteractionHolder<ComponentInteractionEvent> {
    private final Message msg;
    private final String channelID;
    private final String memberID;

    private final IDHolder holder;
    private final String modID;
    private final int lang;

    private String roleID;

    public SetupMemberButtonHolder(Message msg, String channelID, String memberID, IDHolder holder, String modID, int lang) {
        super(ComponentInteractionEvent.class);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = memberID;

        this.holder = holder;
        this.modID = modID;
        this.lang = lang;
    }

    @Override
    public int handleEvent(ComponentInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member m = event.getInteraction().getMember().get();

        if(!m.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message me = event.getMessage().get();

        if(!me.getId().asString().equals(msg.getId().asString()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public Mono<?> getInteraction(ComponentInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();
        Guild g = msg.getGuild().block();

        if(ch == null || g == null)
            return Mono.empty();

        switch (event.getCustomId()) {
            case "role":
                SelectMenuInteractionEvent es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                roleID = es.getValues().get(0);

                ((Button) msg.getComponents().get(1).getChildren().get(0)).disabled(false);

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("setup_memsele", lang).replace("_RRR_", es.getValues().get(0))))
                                .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().addAllRoles(new ArrayList<>()).build())))
                                .components(getComponents(g))
                                .build()
                ));
            case "confirm":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                holder.MOD = modID;
                holder.MEMBER = roleID;

                StaticStore.idHolder.put(g.getId().asString(), holder);

                Command.createMessage(ch, m -> {
                    m.messageReference(msg.getId());
                    m.allowedMentions(AllowedMentions.builder().build());
                    m.content(LangID.getStringByID("setup_done", lang).replace("_MOD_", modID).replace("_MEM_", roleID));
                });

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("setup_memsele", lang).replace("_RRR_", roleID)))
                                .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().addAllRoles(new ArrayList<>()).build())))
                                .components(new ArrayList<>())
                                .build()
                ));
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("setup_cancel", lang)))
                                .components(new ArrayList<>())
                                .build()
                ));
        }

        return Mono.empty();
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        Command.editMessage(msg, m -> {
            m.components(new ArrayList<>());
            m.content(wrap(LangID.getStringByID("setup_expire", lang)));
        });
    }

    private List<ComponentData> getComponents(Guild g) {
        List<Role> roles = g.getRoles().collectList().block();

        if(roles == null)
            return new ArrayList<>();

        List<SelectMenu.Option> options = new ArrayList<>();

        for(int i = 0; i < roles.size(); i++) {
            if(roles.get(i).isEveryone() || roles.get(i).isManaged())
                continue;

            if(roleID != null && roles.get(i).getId().asString().equals(roleID)) {
                options.add(SelectMenu.Option.ofDefault(roles.get(i).getName(), roles.get(i).getId().asString()).withDescription(roles.get(i).getId().asString()));
            } else {
                options.add(SelectMenu.Option.of(roles.get(i).getName(), roles.get(i).getId().asString()).withDescription(roles.get(i).getId().asString()));
            }
        }

        List<ComponentData> result = new ArrayList<>();

        result.add(ActionRow.of(SelectMenu.of("role", options).withPlaceholder(LangID.getStringByID("setup_select", lang))).getData());
        result.add(ActionRow.of(Button.success("confirm", LangID.getStringByID("button_confirm", lang)).disabled(roleID == null), Button.danger("cancel", LangID.getStringByID("button_cancel", lang))).getData());

        return result;
    }
}

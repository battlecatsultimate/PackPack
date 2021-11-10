package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;
import mandarin.packpack.supporter.server.holder.SetupModButtonHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Setup extends ConstraintCommand {
    public Setup(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        Guild g = getGuild(event).block();
        MessageChannel ch = getChannel(event);

        if(g == null || ch == null) {
            return;
        }

        if(alreadySet(g)) {
            Message m = createMessage(ch, b -> {
                b.content(LangID.getStringByID("setup_confirm", lang));

                List<ActionComponent> components = new ArrayList<>();

                components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
                components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

                b.addComponent(ActionRow.of(components));
            });

            if(m == null)
                return;

            Message author = getMessage(event);

            if(author == null)
                return;

            Optional<Member> om = getMember(event);

            if(om.isEmpty())
                return;

            Member member = om.get();

            StaticStore.putHolder(member.getId().asString(), new ConfirmButtonHolder(m, author, ch.getId().asString(), member.getId().asString(), () -> initializeSetup(ch, g, author), lang));
        } else {
            Message author = getMessage(event);

            if(author == null)
                return;

            initializeSetup(ch, g, author);
        }
    }

    private void initializeSetup(MessageChannel ch, Guild g, Message author) {
        Message m = createMessage(ch, b -> {
            b.content(LangID.getStringByID("setup_mod", lang));

            List<Role> roles = g.getRoles().collectList().block();

            if(roles == null)
                return;

            List<SelectMenu.Option> options = new ArrayList<>();

            for(int i = 0; i < roles.size(); i++) {
                if(roles.get(i).isEveryone() || roles.get(i).isManaged())
                    continue;

                options.add(SelectMenu.Option.of(roles.get(i).getName(), roles.get(i).getId().asString()).withDescription(roles.get(i).getId().asString()));
            }

            b.addComponent(ActionRow.of(SelectMenu.of("role", options).withPlaceholder(LangID.getStringByID("setup_select", lang))));
            b.addComponent(ActionRow.of(Button.success("confirm", LangID.getStringByID("button_confirm", lang)).disabled(true), Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));
            b.allowedMentions(AllowedMentions.builder().build());
        });

        if(m == null)
            return;

        author.getAuthor().ifPresent(u -> StaticStore.putHolder(u.getId().asString(), new SetupModButtonHolder(m, author, ch.getId().asString(), u.getId().asString(), holder, lang)));
    }

    private boolean alreadySet(Guild g) {
        if(holder.MOD != null) {
            Role r = g.getRoleById(Snowflake.of(holder.MOD)).block();

            if(r == null) {
                StaticStore.logger.uploadLog("W/Setup | Role was null while trying to perform `alreadySet`");

                return false;
            }

            return !r.getName().equals("PackPackMod") || holder.MEMBER != null;
        } else {
            StaticStore.logger.uploadLog("Invalid ID holder data found, moderator role ID was null\nServer ID : "+g.getId().asString()+" | "+g.getName()+"\n-----ID Holder-----\n\n"+holder);
        }

        return false;
    }
}

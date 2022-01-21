package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ScamLinkSubscriptionHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SubscribeScamLinkDetector extends ConstraintCommand {
    public SubscribeScamLinkDetector(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Guild g = getGuild(event).block();

        if(g == null) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_noguild", lang)));
            return;
        }

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_noid", lang)));
            return;
        }

        String channel = parseChannel(contents[1]);

        if(!StaticStore.isNumeric(channel)) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_invalidch", lang)));
            return;
        }

        if(!isValidChannel(g, channel)) {
            createMessage(ch, m -> m.content(LangID.getStringByID("subscam_nosuch", lang)));
            return;
        }

        System.out.println(getMute(g, getContent(event)));

        Message msg = createMessage(ch, m -> {
            m.content(LangID.getStringByID("subscam_decide", lang));

            List<SelectMenu.Option> options = new ArrayList<>();

            options.add(SelectMenu.Option.ofDefault(LangID.getStringByID("mute", lang), "mute"));
            options.add(SelectMenu.Option.of(LangID.getStringByID("kick", lang), "kick"));
            options.add(SelectMenu.Option.of(LangID.getStringByID("ban", lang), "ban"));

            m.addComponent(ActionRow.of(SelectMenu.of("action", options)));

            List<SelectMenu.Option> notices = new ArrayList<>();

            notices.add(SelectMenu.Option.ofDefault(LangID.getStringByID("noticex", lang), "noticeX"));
            notices.add(SelectMenu.Option.of(LangID.getStringByID("noticeall", lang), "noticeAll"));

            m.addComponent(ActionRow.of(SelectMenu.of("notice", notices)));

            List<ActionComponent> components = new ArrayList<>();

            components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
            components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

            m.addComponent(ActionRow.of(components));
        });

        getMember(event).ifPresent(m -> StaticStore.putHolder(m.getId().asString(), new ScamLinkSubscriptionHolder(msg, ch.getId().asString(), m.getId().asString(), lang, channel, getMute(g, getContent(event)))));
    }

    private String parseChannel(String content) {
        if(content.startsWith("<#")) {
            return content.replace("<#", "").replace(">", "");
        } else {
            return content;
        }
    }

    private boolean isValidChannel(Guild g, String id) {
        AtomicReference<Boolean> valid = new AtomicReference<>(false);

        g.getChannels().collectList().subscribe(l -> {
            for(GuildChannel gc : l) {
                if((gc.getType() == Channel.Type.GUILD_TEXT || gc.getType() == Channel.Type.GUILD_NEWS) && id.equals(gc.getId().asString())) {
                    valid.set(true);
                    return;
                }
            }
        });

        return valid.get();
    }

    private String getMute(Guild g, String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-m") || contents[i].equals("-mute")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]) && isValidID(g, contents[i + 1])) {
                return contents[i + 1];
            }
        }

        return null;
    }

    private boolean isValidID(Guild g, String id) {
        Set<Snowflake> ids = g.getRoleIds();

        for(Snowflake snow : ids) {
            if(snow.asString().equals(id))
                return true;
        }

        return false;
    }
}

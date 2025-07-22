package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ScamLinkSubscriptionHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class SubscribeScamLinkDetector extends ConstraintCommand {
    public SubscribeScamLinkDetector(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        Guild g = loader.getGuild();

        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            ch.sendMessage(LangID.getStringByID("subscribeScamDetector.failed.noChannel", lang)).queue();

            return;
        }

        String channel = parseChannel(contents[1]);

        if (!StaticStore.isNumeric(channel)) {
            ch.sendMessage(LangID.getStringByID("subscribeScamDetector.failed.invalidID", lang)).queue();

            return;
        }

        if (!isValidChannel(g, channel)) {
            ch.sendMessage(LangID.getStringByID("subscribeScamDetector.failed.invalidChannel", lang)).queue();

            return;
        }

        System.out.println(getMute(g, loader.getContent()));

        List<SelectOption> options = new ArrayList<>();

        options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.mute", lang), "scamDetector.action.mute").withDefault(true));
        options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.kick", lang), "scamDetector.action.kick"));
        options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.ban", lang), "scamDetector.action.ban"));

        List<SelectOption> notices = new ArrayList<>();

        notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.onlyMember", lang), "noticeX").withDefault(true));
        notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.allUsers", lang), "noticeAll"));

        List<Button> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        ch.sendMessage(LangID.getStringByID("subscribeScamDetector.decision.none", lang))
                .setComponents(
                        ActionRow.of(StringSelectMenu.create("action").addOptions(options).build()),
                        ActionRow.of(StringSelectMenu.create("notice").addOptions(notices).build()),
                        ActionRow.of(components)
                ).queue(msg -> {
                    Member m = loader.getMember();

                    StaticStore.putHolder(m.getId(), new ScamLinkSubscriptionHolder(loader.getMessage(), m.getId(), ch.getId(), msg, lang, channel, getMute(g, loader.getContent())));
                });
    }

    private String parseChannel(String content) {
        if(content.startsWith("<#")) {
            return content.replace("<#", "").replace(">", "");
        } else {
            return content;
        }
    }

    private boolean isValidChannel(Guild g, String id) {
        List<GuildChannel> channels = g.getChannels();

        for(GuildChannel gc : channels) {
            if((gc.getType() == ChannelType.TEXT || gc.getType() == ChannelType.NEWS) && id.equals(gc.getId())) {

                return true;
            }
        }

        return false;
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
        List<Role> roles = g.getRoles();

        for(Role role : roles) {
            if(role.getId().equals(id))
                return true;
        }

        return false;
    }
}

package mandarin.packpack.commands.server;

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
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SubscribeScamLinkDetector extends ConstraintCommand {
    public SubscribeScamLinkDetector(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        Guild g = loader.getGuild();

        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            ch.sendMessage(LangID.getStringByID("subscam_noid", lang)).queue();

            return;
        }

        String channel = parseChannel(contents[1]);

        if (!StaticStore.isNumeric(channel)) {
            ch.sendMessage(LangID.getStringByID("subscam_invalidch", lang)).queue();

            return;
        }

        if (!isValidChannel(g, channel)) {
            ch.sendMessage(LangID.getStringByID("subscam_nosuch", lang)).queue();

            return;
        }

        System.out.println(getMute(g, loader.getContent()));

        List<SelectOption> options = new ArrayList<>();

        options.add(SelectOption.of(LangID.getStringByID("mute", lang), "mute").withDefault(true));
        options.add(SelectOption.of(LangID.getStringByID("kick", lang), "kick"));
        options.add(SelectOption.of(LangID.getStringByID("ban", lang), "ban"));

        List<SelectOption> notices = new ArrayList<>();

        notices.add(SelectOption.of(LangID.getStringByID("noticex", lang), "noticeX").withDefault(true));
        notices.add(SelectOption.of(LangID.getStringByID("noticeall", lang), "noticeAll"));

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        ch.sendMessage(LangID.getStringByID("subscam_decide", lang))
                .setComponents(
                        ActionRow.of(StringSelectMenu.create("action").addOptions(options).build()),
                        ActionRow.of(StringSelectMenu.create("notice").addOptions(notices).build()),
                        ActionRow.of(components)
                ).queue(msg -> {
                    Member m = loader.getMember();

                    StaticStore.putHolder(m.getId(), new ScamLinkSubscriptionHolder(loader.getMessage(), msg, ch.getId(), lang, channel, getMute(g, loader.getContent())));
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

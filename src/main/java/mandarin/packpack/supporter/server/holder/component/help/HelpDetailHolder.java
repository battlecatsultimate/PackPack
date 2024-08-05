package mandarin.packpack.supporter.server.holder.component.help;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HelpDetailHolder extends ComponentHolder {
    private final String selectedCommand;

    private final int color;

    public HelpDetailHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang, @NotNull String selectedCommand, int color) {
        super(author, channelID, message, lang);

        this.selectedCommand = selectedCommand;

        this.color = color;
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (!event.getComponentId().equals("back"))
            return;

        goBack(event);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessageComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setEmbeds(getEmbed())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private MessageEmbed getEmbed() {
        String prefix;

        if (message.getChannel() instanceof GuildChannel) {
            IDHolder holder = StaticStore.idHolder.get(message.getGuild().getId());

            if (holder == null) {
                prefix = StaticStore.globalPrefix;
            } else {
                prefix = holder.config.prefix;
            }
        } else {
            prefix = StaticStore.globalPrefix;
        }

        String usage = LangID.getStringByID("help." + selectedCommand + ".usage", lang).replace("`", "").formatted(prefix);
        String command = usage.split(" ")[0];

        EmbedBuilder builder = new EmbedBuilder();

        if(LangID.hasID("help." + selectedCommand + ".url", lang)) {
            builder.setTitle(command, LangID.getStringByID("help." + selectedCommand + ".url", lang));
            builder.setDescription(LangID.getStringByID("help.format.guide", lang));
        } else {
            builder.setTitle(command);
        }

        builder.setColor(color);

        builder.addField(LangID.getStringByID("help.format.usage", lang), usage, false);

        if (LangID.hasID("help." + selectedCommand + ".alias", lang)) {
            builder.addField(LangID.getStringByID("help.format.alias", lang), LangID.getStringByID("help." + selectedCommand + ".alias", lang), false);
        }

        builder.addField(LangID.getStringByID("help.format.description", lang), LangID.getStringByID("help." + selectedCommand + ".description", lang), false);

        if (LangID.hasID("help." + selectedCommand + ".parameter", lang)) {
            builder.addField(LangID.getStringByID("help.format.parameter", lang), LangID.getStringByID("help." + selectedCommand + ".parameter", lang), false);
        }

        if (LangID.hasID("help." + selectedCommand + ".example", lang)) {
            builder.addField(LangID.getStringByID("help.format.example", lang), LangID.getStringByID("help." + selectedCommand + ".example", lang), false);
        }

        int tipIndex = 1;

        while (LangID.hasID("help." + selectedCommand + ".tip" + (tipIndex == 1 ? "" : String.valueOf(tipIndex)), lang)) {
            String id = "help." + selectedCommand + ".tip" + (tipIndex == 1 ? "" : String.valueOf(tipIndex));
            String tips = LangID.getStringByIDSuppressed(id, lang);

            builder.addField(tipIndex == 1 ? LangID.getStringByID("help.format.tip", lang) : "** **", tips, false);

            tipIndex++;
        }

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK)));

        return result;
    }
}

package mandarin.packpack.supporter.server.holder.component.help;

import common.CommonStatic;
import mandarin.packpack.commands.Help;
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
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HelpCommandHolder extends ComponentHolder {
    private final static int pageChunkSize = 5;

    private final static String[][] commandData = {
            {
                "analyze", "config", "donate", "locale", "optOut", "prefix", "timezone"
            },
            {
                "calculator", "differentiate", "integrate", "plot", "plotRTheta", "tPlot", "solve"
            },
            {
                "background", "castle", "catCombo", "enemyDps", "enemyGif", "enemyImage",
                "enemySprite", "enemyStat", "findReward", "findStage", "formDps", "formGif",
                "formSprite", "formStat", "medal", "soul", "soulImage", "soulSprite", "stageInfo",
                "talentInfo", "treasure"
            },
            {
                "boosterEmoji", "boosterEmojiRemove", "boosterRole", "boosterRoleRemove",
                "channelPermission", "hasRole", "idSet", "serverConfig", "serverPrefix",
                "serverStat", "setup", "subscribeEvent", "subscribeScamLinkDetector",
                "unsubscribeScamLinkDetector", "watchDM"
            },
            {
                "animationAnalyzer", "announcement", "checkEventUpdate", "comboAnalyzer",
                "downloadApk", "enemyStatAnalyzer", "eventDataArchive", "printEvent",
                "printGachaEvent", "printItemEvent", "printStageEvent", "stageImage",
                "stageStatAnalyzer", "statAnalyzer", "stageMapImage", "talentAnalyzer",
                "trueFormAnalyzer"
            },
            {
                "alias", "aliasAdd", "aliasRemove", "memory", "registerScamLink", "save",
                "serverJson", "statistic", "suggest", "unregisterScamLink"
            }
    };

    private final int color;

    private final Help.HelpCategory selectedCategory;
    private final String[] commandList;

    private int page = 0;

    public HelpCommandHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang, @NotNull Help.HelpCategory selectedCategory, int color) {
        super(author, channelID, message, lang);

        this.color = color;

        this.selectedCategory = selectedCategory;
        commandList = commandData[selectedCategory.ordinal()];
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "prev10" -> {
                page -= 10;

                applyResult(event);
            }
            case "prev" -> {
                page--;

                applyResult(event);
            }
            case "next" -> {
                page++;

                applyResult(event);
            }
            case "next10" -> {
                page += 10;

                applyResult(event);
            }
            case "command" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                String commandName = e.getValues().getFirst();

                connectTo(event, new HelpDetailHolder(getAuthorMessage(), channelID, message, lang, commandName, color));
            }
            case "back" -> goBack(event);
        }
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

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
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

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(LangID.getStringByID("help.main.category." + selectedCategory.name().toLowerCase(Locale.ENGLISH), lang));

        builder.setColor(color);

        builder.setDescription(LangID.getStringByID("help.main.commandDetail", lang));

        for (int i = page * pageChunkSize; i < Math.min(commandList.length, (page + 1) * pageChunkSize); i++) {
            String commandCode = commandList[i];

            String usage = LangID.getStringByID("help." + commandCode + ".usage", lang).replace("`", "").formatted(prefix);
            String command = usage.split(" ")[0];

            builder.addField(command, "** **\n" + LangID.getStringByID("help." + commandCode + ".description", lang) + "\n** **", false);
        }

        int totalPage = getTotalPage(commandList.length, pageChunkSize);

        builder.setFooter(LangID.getStringByID("ui.search.page", lang).trim().formatted(page + 1, totalPage));

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

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

        List<SelectOption> commandOptions = new ArrayList<>();

        for (int i = page * pageChunkSize; i < Math.min(commandList.length, (page + 1) * pageChunkSize); i++) {
            String commandCode = commandList[i];

            String usage = LangID.getStringByID("help." + commandCode + ".usage", lang).replace("`", "").formatted(prefix);
            String command = usage.split(" ")[0];

            commandOptions.add(SelectOption.of(command, commandCode).withDescription(usage));
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("command")
                        .addOptions(commandOptions)
                        .setPlaceholder(LangID.getStringByID("help.main.selectCommand", lang))
                        .build()
        ));

        int totalPage = getTotalPage(commandList.length, pageChunkSize);

        if (commandList.length > pageChunkSize) {
            List<Button> buttons = new ArrayList<>();

            if (totalPage > 10) {
                buttons.add(Button.secondary("prev10", LangID.getStringByID("ui.search.10Previous", lang)).withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if (totalPage > 10) {
                buttons.add(Button.secondary("next10", LangID.getStringByID("ui.search.10Next", lang)).withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK)
        ));

        return result;
    }
}

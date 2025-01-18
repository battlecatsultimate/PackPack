package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.RecordableThread;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.SpamPrevent;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class Command {
    public static MessageCreateAction registerConfirmButtons(MessageCreateAction m, CommonStatic.Lang.Locale lang) {
        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        return m.setActionRow(components);
    }

    public static MessageCreateAction registerSearchComponents(MessageCreateAction m, int dataSize, List<String> data, CommonStatic.Lang.Locale lang) {
        int totPage = dataSize / SearchHolder.PAGE_CHUNK;

        if(dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(dataSize > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(StaticStore.cutOffText(elements[1], 100), String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return m.setComponents(rows);
    }

    public static ReplyCallbackAction registerSearchComponents(ReplyCallbackAction m, int dataSize, List<String> data, CommonStatic.Lang.Locale lang) {
        int totPage = dataSize / SearchHolder.PAGE_CHUNK;

        if(dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(dataSize > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(StaticStore.cutOffText(elements[1], 100), String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return m.setComponents(rows);
    }

    public static void replyToMessageSafely(MessageChannel ch, String content, Message reference, Function<MessageCreateAction, MessageCreateAction> function) {
        MessageCreateAction action = ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>());

        action = function.apply(action);

        if(ch instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) ch).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY)) {
                action.setMessageReference(reference).mentionRepliedUser(false).queue();
            } else {
                action.queue();
            }
        } else {
            action.setMessageReference(reference).mentionRepliedUser(false).queue();
        }
    }

    public static void replyToMessageSafely(MessageChannel ch, String content, Message reference, Function<MessageCreateAction, MessageCreateAction> function, Consumer<Message> onSuccess) {
        MessageCreateAction action = ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>());

        action = function.apply(action);

        if(ch instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) ch).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY) && reference != null) {
                action.setMessageReference(reference).mentionRepliedUser(false).queue(onSuccess);
            } else {
                action.queue(onSuccess);
            }
        } else {
            action.setMessageReference(reference).mentionRepliedUser(false).queue(onSuccess);
        }
    }

    public static void replyToMessageSafely(GenericCommandInteractionEvent event, String content, Function<ReplyCallbackAction, ReplyCallbackAction> function) {
        ReplyCallbackAction action = event.deferReply()
                .setContent(content)
                .setAllowedMentions(new ArrayList<>());

        action = function.apply(action);

        if(event instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) event).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) event, Permission.MESSAGE_HISTORY)) {
                action.mentionRepliedUser(false).queue();
            } else {
                action.queue();
            }
        } else {
            action.mentionRepliedUser(false).queue();
        }
    }

    public static void replyToMessageSafely(GenericCommandInteractionEvent event, String content, Function<ReplyCallbackAction, ReplyCallbackAction> function, Consumer<Message> onSuccess) {
        ReplyCallbackAction action = event.deferReply()
                .setContent(content)
                .setAllowedMentions(new ArrayList<>());

        action = function.apply(action);

        if(event instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) event).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) event, Permission.MESSAGE_HISTORY)) {
                action.mentionRepliedUser(false).queue(hook -> hook.retrieveOriginal().queue(onSuccess));
            } else {
                action.queue(hook -> hook.retrieveOriginal().queue(onSuccess));
            }
        } else {
            action.mentionRepliedUser(false).queue(hook -> hook.retrieveOriginal().queue(onSuccess));
        }
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, File f, Message reference) {
        MessageCreateAction action = ch.sendMessage(content)
                .addFiles(FileUpload.fromData(f))
                .setAllowedMentions(new ArrayList<>());

        if(ch instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) ch).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY)) {
                action = action.setMessageReference(reference).mentionRepliedUser(false);
            }
        } else {
            action = action.setMessageReference(reference).mentionRepliedUser(false);
        }

        action.queue(m -> {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        }, e -> {
            StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        });
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, File f, String name, Message reference) {
        MessageCreateAction action = ch.sendMessage(content)
                .addFiles(FileUpload.fromData(f, name))
                .setAllowedMentions(new ArrayList<>());

        if(ch instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) ch).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY)) {
                action = action.setMessageReference(reference).mentionRepliedUser(false);
            }
        } else {
            action = action.setMessageReference(reference).mentionRepliedUser(false);
        }

        action.queue(m -> {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        }, e -> {
            StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        });
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, File f, String fileName) {
        ch.sendMessage(content)
                .addFiles(FileUpload.fromData(f, fileName))
                .setAllowedMentions(new ArrayList<>())
                .queue(m -> {
                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                });
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, MessageEmbed embed, File f, String name, Message reference) {
        MessageCreateAction action = ch.sendMessage(content)
                .setEmbeds(embed)
                .addFiles(FileUpload.fromData(f, name))
                .setAllowedMentions(new ArrayList<>());

        if(ch instanceof GuildMessageChannel) {
            Guild g = ((GuildMessageChannel) ch).getGuild();

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY)) {
                action = action.setMessageReference(reference).mentionRepliedUser(false);
            }
        } else {
            action = action.setMessageReference(reference).mentionRepliedUser(false);
        }

        action.queue(m -> {
            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        }, e -> {
            StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

            if(f.exists() && !f.delete()) {
                StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
            }
        });
    }

    public final int DEFAULT_ERROR = -1;
    public final int SERVER_ERROR = -2;

    public final CommonStatic.Lang.Locale lang;
    public final boolean requireGuild;

    protected final List<Permission> requiredPermission = new ArrayList<>();

    public Command(CommonStatic.Lang.Locale lang, boolean requireGuild) {
        this.lang = lang;
        this.requireGuild = requireGuild;
    }

    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, this::onLoaded);
    }

    public void execute(GenericInteractionCreateEvent event) {
        new CommandLoader().load(event, this::onLoaded);
    }

    public void prepare() throws Exception {

    }

    public abstract void doSomething(@Nonnull CommandLoader loader) throws Exception;

    public void onFail(CommandLoader loader, int error) {
        StaticStore.executed--;

        MessageChannel ch = loader.getChannel();

        if(error == DEFAULT_ERROR) {
            ch.sendMessage(StaticStore.ERROR_MSG).queue();
        } else if(error == SERVER_ERROR) {
            ch.sendMessage(LangID.getStringByID("bot.sendFailure.reason.apiError", lang)).queue();
        }
    }

    public void onSuccess(CommandLoader loader) {}

    public void onCancel(CommandLoader loader) {}

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    public void createMessageWithNoPings(MessageChannel ch, String content, Consumer<Message> onSuccess) {
        ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .queue(onSuccess);
    }

    public void registerRequiredPermission(@Nonnull Permission... permissions) {
        Collections.addAll(requiredPermission, permissions);
    }

    protected boolean hasProperPermission(GuildChannel ch, Member self) {
        return self.hasPermission(ch, requiredPermission);
    }

    protected List<Permission> getMissingPermissions(GuildChannel ch, Member self) {
        List<Permission> missing = new ArrayList<>();

        for(int i = 0; i < requiredPermission.size(); i++) {
            if(!self.hasPermission(ch, requiredPermission.get(i))) {
                missing.add(requiredPermission.get(i));
            }
        }

        return missing;
    }

    protected String parsePermissionAsList(List<Permission> missingPermissions) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < missingPermissions.size(); i++) {
            Permission permission = missingPermissions.get(i);

            String id = switch (permission.getName()) {
                case "Attach Files" -> "bot.permission.attachFile";
                case "Manage Messages" -> "bot.permission.manageMessage";
                case "Add Reactions" -> "bot.permission.addReaction";
                case "Manage Roles" -> "bot.permission.manageRole";
                case "Manage Emojis and Stickers" -> "bot.permission.manageEmoji";
                case "Use External Emojis" -> "bot.permission.useExternalEmoji";
                case "Embed Links" -> "bot.permission.addEmbed";
                default -> permission.getName();
            };

            builder.append(LangID.getStringByID(id, lang));

            if(i < missingPermissions.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private void onLoaded(CommandLoader loader) {
        try {
            prepare();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/Command::execute - Failed to prepare command : "+this.getClass().getName());

            return;
        }

        MessageChannel ch = loader.getChannel();

        if(requireGuild && !(ch instanceof GuildChannel)) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, LangID.getStringByID("bot.sendFailure.reason.serverRequired", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("bot.sendFailure.reason.serverRequired", lang), a -> a);
            }

            return;
        }

        User u = loader.getUser();

        SpamPrevent spam;

        if(StaticStore.spamData.containsKey(u.getId()) && loader.fromMessage) {
            spam = StaticStore.spamData.get(u.getId());

            if(spam.isPrevented(ch, lang, u.getId()))
                return;
        } else if(!StaticStore.spamData.containsKey(u.getId())) {
            spam = new SpamPrevent();

            StaticStore.spamData.put(u.getId(), spam);
        }

        StaticStore.executed++;

        boolean canTry = false;

        if(ch instanceof GuildMessageChannel tc) {
            Guild g = loader.getGuild();

            if(!tc.canTalk()) {
                String serverName = g.getName();
                String channelName = ch.getName();

                String content;

                content = LangID.getStringByID("bot.sendFailure.reason.noPermission.withChannel", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                u.openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(content))
                        .queue();

                return;
            }

            List<Permission> missingPermission = getMissingPermissions((GuildChannel) ch, g.getSelfMember());

            if(!missingPermission.isEmpty()) {
                u.openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(LangID.getStringByID("bot.sendFailure.reason.missingPermission", lang).replace("_PPP_", parsePermissionAsList(missingPermission)).replace("_SSS_", g.getName()).replace("_CCC_", ch.getName())))
                        .queue();

                return;
            }
        }

        try {
            RecordableThread t = new RecordableThread(() -> {
                doSomething(loader);

                if (StaticStore.logCommand) {
                    Logger.addLog(this.getClass() + " called : " + (loader.fromMessage ? loader.getContent() : loader.getInteractionEvent().getFullCommandName()));
                }
            }, e -> {
                String data;

                if (loader.fromMessage) {
                    data = "Command : " + loader.getContent() + "\n\n" +
                            "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
                } else {
                    data = "Command : " + loader.getInteractionEvent().getFullCommandName() + "\n\n" +
                            "Member : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
                }

                if (ch instanceof GuildChannel) {
                    Guild g = loader.getGuild();

                    data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
                }

                StaticStore.logger.uploadErrorLog(e, "Failed to perform command : "+this.getClass()+"\n\n" + data);

                if(e instanceof ErrorResponseException) {
                    onFail(loader, SERVER_ERROR);
                } else {
                    onFail(loader, DEFAULT_ERROR);
                }
            }, loader);

            t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime() + " | Content : " + (loader.fromMessage ? loader.getContent() : loader.getInteractionEvent().getFullCommandName()));
            t.start();
        } catch (Exception e) {
            String data;

            if (loader.fromMessage) {
                data = "Command : " + loader.getContent() + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            } else {
                data = "Command : " + loader.getInteractionEvent().getFullCommandName() + "\n\n" +
                        "Member : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + " (" + ch.getId() + "|" + ch.getType().name() + ")";
            }

            if (ch instanceof GuildChannel) {
                Guild g = loader.getGuild();

                data += "\n\nGuild : " + g.getName() + " (" + g.getId() + ")";
            }

            StaticStore.logger.uploadErrorLog(e, "Failed to perform command : "+this.getClass()+"\n\n" + data);

            if(e instanceof ErrorResponseException) {
                onFail(loader, SERVER_ERROR);
            } else {
                onFail(loader, DEFAULT_ERROR);
            }
        }
    }
}

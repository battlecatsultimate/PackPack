package mandarin.packpack.commands;

import mandarin.packpack.supporter.EmojiStore;
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
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class Command {
    public static MessageCreateAction registerConfirmButtons(MessageCreateAction m, int lang) {
        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        return m.setActionRow(components);
    }

    public static MessageCreateAction registerSearchComponents(MessageCreateAction m, int dataSize, List<String> data, int lang) {
        int totPage = dataSize / SearchHolder.PAGE_CHUNK;

        if(dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(dataSize > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT));

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:[^\\s]+?:\\d+>")) {
                    options.add(SelectOption.of(StaticStore.cutOffText(elements[1], 100), String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(StaticStore.cutOffText(element, 100), String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("search_list", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

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

            if(g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_HISTORY)) {
                action.setMessageReference(reference).mentionRepliedUser(false).queue(onSuccess);
            } else {
                action.queue(onSuccess);
            }
        } else {
            action.setMessageReference(reference).mentionRepliedUser(false).queue(onSuccess);
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

    public final int lang;
    public final boolean requireGuild;

    protected final List<Permission> requiredPermission = new ArrayList<>();

    public Command(int lang, boolean requireGuild) {
        this.lang = lang;
        this.requireGuild = requireGuild;
    }

    public void execute(GenericMessageEvent event) {
        new CommandLoader().load(event, loader -> {
            try {
                prepare();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/Command::execute - Failed to prepare command : "+this.getClass().getName());

                return;
            }

            MessageChannel ch = loader.getChannel();

            AtomicReference<Boolean> prevented = new AtomicReference<>(false);

            Message msg = loader.getMessage();

            if(requireGuild && !(ch instanceof GuildChannel)) {
                replyToMessageSafely(ch, LangID.getStringByID("require_server", lang), msg, a -> a);

                return;
            }

            User u = msg.getAuthor();

            SpamPrevent spam;

            if(StaticStore.spamData.containsKey(u.getId())) {
                spam = StaticStore.spamData.get(u.getId());

                prevented.set(spam.isPrevented(ch, lang, u.getId()));
            } else {
                spam = new SpamPrevent();

                StaticStore.spamData.put(u.getId(), spam);
            }

            if (prevented.get())
                return;

            StaticStore.executed++;

            boolean canTry = false;

            if(ch instanceof GuildMessageChannel tc) {
                Guild g = loader.getGuild();

                if(!tc.canTalk()) {
                    String serverName = g.getName();
                    String channelName = ch.getName();

                    String content;

                    content = LangID.getStringByID("no_permch", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                    u.openPrivateChannel()
                            .flatMap(pc -> pc.sendMessage(content))
                            .queue();

                    return;
                }

                List<Permission> missingPermission = getMissingPermissions((GuildChannel) ch, g.getSelfMember());

                if(!missingPermission.isEmpty()) {
                    u.openPrivateChannel()
                            .flatMap(pc -> pc.sendMessage(LangID.getStringByID("missing_permission", lang).replace("_PPP_", parsePermissionAsList(missingPermission)).replace("_SSS_", g.getName()).replace("_CCC_", ch.getName())))
                            .queue();

                    return;
                }
            }

            try {
                RecordableThread t = new RecordableThread(() -> doSomething(loader), e -> {
                    String data = "Command : " + loader.getContent() + "\n\n" +
                            "User  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                            "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

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
                });

                t.setName("RecordableThread - " + this.getClass().getName() + " - " + System.nanoTime());
                t.start();
            } catch (Exception e) {
                String data = "Command : " + loader.getContent() + "\n\n" +
                        "Member  : " + u.getName() + " (" + u.getId() + ")\n\n" +
                        "Channel : " + ch.getName() + "(" + ch.getId() + "|" + ch.getType().name() + ")";

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
        });
    }

    public void prepare() throws Exception {

    }

    public abstract void doSomething(CommandLoader loader) throws Exception;

    public void onFail(CommandLoader loader, int error) {
        StaticStore.executed--;

        MessageChannel ch = loader.getChannel();

        if(error == DEFAULT_ERROR) {
            ch.sendMessage(StaticStore.ERROR_MSG).queue();
        } else if(error == SERVER_ERROR) {
            ch.sendMessage(LangID.getStringByID("error_api", lang)).queue();
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
                case "Attach Files" -> "permission_file";
                case "Manage Messages" -> "permission_managemsg";
                case "Add Reactions" -> "permission_addreact";
                case "Manage Roles" -> "permission_addrole";
                case "Manage Emojis and Stickers" -> "permission_addemoji";
                case "Use External Emojis" -> "permission_externalemoji";
                case "Embed Links" -> "permission_embed";
                default -> permission.getName();
            };

            builder.append(LangID.getStringByID(id, lang));

            if(i < missingPermissions.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}

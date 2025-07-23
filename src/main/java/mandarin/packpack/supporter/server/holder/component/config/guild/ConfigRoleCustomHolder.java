package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.modal.CustomRoleNameModalHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleIcon;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigRoleCustomHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigRoleCustomHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "register" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                List<Role> roles = e.getMentions().getRoles();

                if (roles.isEmpty())
                    return;

                Role r = e.getMentions().getRoles().getFirst();

                if (roleAlreadyRegistered(event, r))
                    return;

                if (holder.ID.containsValue(r.getId())) {
                    String name = holder.ID.keySet().stream().filter(k -> {
                        String id = holder.ID.get(k);

                        if (id == null)
                            return false;

                        return id.equals(r.getId());
                    }).findAny().orElse("UNKNOWN");

                    event.deferReply()
                            .setContent(LangID.getStringByID("serverConfig.general.custom.alreadyExist", lang).formatted(name))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();
                } else {
                    TextInput input = TextInput.create("name", LangID.getStringByID("serverConfig.general.custom.name", lang), TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder(LangID.getStringByID("serverConfig.general.custom.decideName", lang))
                            .build();

                    Modal modal = Modal.create("register", LangID.getStringByID("serverConfig.general.custom.customRoleName", lang))
                            .addComponents(ActionRow.of(input))
                            .build();

                    event.replyModal(modal).queue();

                    connectTo(new CustomRoleNameModalHolder(getAuthorMessage(), userID, channelID, message, holder, lang, name -> {
                        holder.ID.put(name, r.getId());

                        applyResult();
                    }));
                }
            }
            case "unregister" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                List<String> values = e.getValues();

                if (values.isEmpty())
                    return;

                String id = values.getFirst();

                registerPopUp(e, LangID.getStringByID("serverConfig.general.custom.unregisterConfirm", lang).formatted(id));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, ev -> {
                    holder.ID.entrySet().removeIf(entry -> {
                        String targetID = entry.getValue();

                        boolean remove = targetID != null && targetID.equals(id);

                        if (remove) {
                            holder.channel.remove(targetID);
                        }

                        StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), this);

                        applyResult();

                        return remove;
                    });

                    ev.deferReply()
                            .setContent(LangID.getStringByID("serverConfig.general.custom.unregistered", lang).formatted(id))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();
                }, lang));
            }
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
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end(true);
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull Holder child) {
        applyResult();
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    @Override
    public void clean() {

    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        List<Role> roles = getAuthorMessage().getGuild().getRoles();

        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.general.custom.documentation.title", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.custom.documentation.description", lang))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.custom.documentation.list", lang))
                .append("\n");

        if (holder.ID.isEmpty()) {
            builder.append("- ").append(LangID.getStringByID("data.none", lang));
        } else {
            int i = page * SearchHolder.PAGE_CHUNK + 1;

            for (String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if (id == null)
                    continue;

                Role role = roles.stream().filter(r -> r.getId().equals(id)).findAny().orElse(null);

                if (role != null) {
                    builder.append(LangID.getStringByID("serverConfig.general.custom.documentation.format.default", lang).formatted(i, name, role.getAsMention(), id));
                } else {
                    builder.append(LangID.getStringByID("serverConfig.general.custom.documentation.format.unknown", lang).formatted(i, name, id));
                }

                builder.append("\n");

                i++;
            }

            if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = (int) Math.ceil(1.0 * holder.ID.size() / SearchHolder.PAGE_CHUNK);

                builder.append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
            }
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        Guild g = getAuthorMessage().getGuild();

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("register", EntitySelectMenu.SelectTarget.ROLE)
                                .setRequiredRange(1, 1)
                                .build()
                )
        );

        List<SelectOption> roleOptions = new ArrayList<>();

        if (holder.ID.isEmpty()) {
            roleOptions.add(SelectOption.of("A", "A"));
        } else {
            List<Role> roles = g.getRoles();

            for (String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if (id == null)
                    continue;

                Role role = roles.stream().filter(r -> r.getId().equals(id)).findAny().orElse(null);

                String description;
                Emoji e;

                if (role != null) {
                    description = LangID.getStringByID("serverConfig.general.custom.listFormat", lang).formatted(role.getName(), id);

                    RoleIcon icon = role.getIcon();

                    if (icon != null) {
                        String emoji = icon.getEmoji();

                        if (emoji != null) {
                            e = Emoji.fromFormatted(emoji);
                        } else {
                            e = null;
                        }
                    } else {
                        e = null;
                    }
                } else {
                    description = LangID.getStringByID("serverConfig.general.custom.deleted", lang);
                    e = null;
                }

                roleOptions.add(SelectOption.of(name, id).withDescription(description).withEmoji(e));
            }
        }

        result.add(
                ActionRow.of(
                        StringSelectMenu.create("unregister")
                                .addOptions(roleOptions)
                                .setPlaceholder(LangID.getStringByID("serverConfig.general.custom.unregister", lang))
                                .setDisabled(holder.ID.isEmpty())
                                .build()
                )
        );

        if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(1.0 * holder.ID.size() / SearchHolder.PAGE_CHUNK);

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }

    private boolean roleAlreadyRegistered(GenericComponentInteractionCreateEvent event, Role role) {
        String id = role.getId();

        if (id.equals(holder.moderator)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.custom.already.moderator", lang))
                    .setAllowedMentions(new ArrayList<>())
                    .setEphemeral(true)
                    .queue();

            return true;
        } else if (id.equals(holder.member)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.custom.already.member", lang))
                    .setAllowedMentions(new ArrayList<>())
                    .setEphemeral(true)
                    .queue();

            return true;
        } else if (id.equals(holder.booster)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.custom.already.booster", lang))
                    .setAllowedMentions(new ArrayList<>())
                    .setEphemeral(true)
                    .queue();

            return true;
        }

        return false;
    }
}

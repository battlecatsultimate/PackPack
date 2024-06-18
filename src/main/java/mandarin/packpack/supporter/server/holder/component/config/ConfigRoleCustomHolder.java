package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigRoleCustomHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigRoleCustomHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
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
                            .setContent(LangID.getStringByID("sercon_customexist", lang).formatted(name))
                            .setEphemeral(true)
                            .queue();
                } else {
                    TextInput input = TextInput.create("name", LangID.getStringByID("sercon_customname", lang), TextInputStyle.SHORT)
                            .setRequired(true)
                            .setPlaceholder(LangID.getStringByID("sercon_customnamedummy", lang))
                            .build();

                    Modal modal = Modal.create("register", LangID.getStringByID("sercon_customnametit", lang))
                            .addActionRow(input)
                            .build();

                    event.replyModal(modal).queue();

                    connectTo(new CustomRoleNameModalHolder(getAuthorMessage(), channelID, message, holder, lang, name -> {
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

                registerPopUp(e, LangID.getStringByID("sercon_customunreg", lang).formatted(id), lang);

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, ev -> {
                    holder.ID.entrySet().removeIf(entry -> {
                        String targetID = entry.getValue();

                        return targetID != null && targetID.equals(id);
                    });

                    ev.deferReply()
                            .setContent(LangID.getStringByID("sercon_customremoved", lang).formatted(id))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();

                    StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), this);

                    applyResult();
                }, lang));
            }
            case "prev10" -> {
                page -= 10;

                applyResult();
            }
            case "prev" -> {
                page--;

                applyResult();
            }
            case "next" -> {
                page++;

                applyResult();
            }
            case "next10" -> {
                page += 10;

                applyResult();
            }
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang), lang);

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("sercon_cancel", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void clean() {

    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
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

        builder.append(LangID.getStringByID("sercon_customtit", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("sercon_customdesc", lang))
                .append("\n")
                .append(LangID.getStringByID("sercon_customlisttit", lang))
                .append("\n");

        if (holder.ID.isEmpty()) {
            builder.append("- ").append(LangID.getStringByID("data_none", lang));
        } else {
            int i = page * SearchHolder.PAGE_CHUNK + 1;

            for (String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if (id == null)
                    continue;

                Role role = roles.stream().filter(r -> r.getId().equals(id)).findAny().orElse(null);

                if (role != null) {
                    builder.append(LangID.getStringByID("sercon_customlistelem", lang).formatted(i, name, role.getAsMention(), id));
                } else {
                    builder.append(LangID.getStringByID("sercon_customlistdel", lang).formatted(i, name, id));
                }

                builder.append("\n");

                i++;
            }

            if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = (int) Math.ceil(1.0 * holder.ID.size() / SearchHolder.PAGE_CHUNK);

                builder.append(LangID.getStringByID("formst_page", lang).replace("_", Integer.toString(page + 1)).replace("-", Integer.toString(totalPage)));
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        Guild g = getAuthorMessage().getGuild();

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("register", EntitySelectMenu.SelectTarget.ROLE)
                                .setRequiredRange(1, 1)
                                .build()
                )
        );

        List<SelectOption> roleOptions = new ArrayList<>();

        List<Role> roles = g.getRoles();

        for (String name : holder.ID.keySet()) {
            String id = holder.ID.get(name);

            if (id == null)
                continue;

            Role role = roles.stream().filter(r -> r.getId().equals(id)).findAny().orElse(null);

            String description;
            Emoji e;

            if (role != null) {
                description = LangID.getStringByID("sercon_customroledesc", lang).formatted(role.getName(), id);

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
                description = LangID.getStringByID("sercon_customnone", lang);
                e = null;
            }

            roleOptions.add(SelectOption.of(name, id).withDescription(description).withEmoji(e));
        }

        result.add(
                ActionRow.of(
                        StringSelectMenu.create("unregister").addOptions(roleOptions).setPlaceholder(LangID.getStringByID("sercon_customdel", lang)).build()
                )
        );

        if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(1.0 * holder.ID.size() / SearchHolder.PAGE_CHUNK);

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }

    private boolean roleAlreadyRegistered(GenericComponentInteractionCreateEvent event, Role role) {
        String id = role.getId();

        if (id.equals(holder.MOD)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_rolemodalready", lang))
                    .setEphemeral(true)
                    .queue();

            return true;
        } else if (id.equals(holder.MEMBER)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_rolememalready", lang))
                    .setEphemeral(true)
                    .queue();

            return true;
        } else if (id.equals(holder.BOOSTER)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_rolebooalready", lang))
                    .setEphemeral(true)
                    .queue();

            return true;
        }

        return false;
    }
}

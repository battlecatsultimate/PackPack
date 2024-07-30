package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigUserBanHolder extends ServerConfigHolder {
    private int page = 0;

    public ConfigUserBanHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "user" -> {
                if (!(event instanceof EntitySelectInteractionEvent e))
                    return;

                List<String> cantBan = new ArrayList<>();
                List<User> users = e.getMentions().getUsers();
                List<String> ids = new ArrayList<>();

                for (User u : users) {
                    if (u.isBot()) {
                        cantBan.add("- <@" + u.getId() + "> [" + u.getId() + "] : " + LangID.getStringByID("serverConfig.commandBan.cantBan.reason.bot", lang));
                    } else if (u.getId().equals(getAuthorMessage().getAuthor().getId())) {
                        cantBan.add("- <@" + u.getId() + "> [" + u.getId() + "] : " + LangID.getStringByID("serverConfig.commandBan.cantBan.reason.self", lang));
                    } else {
                        ids.add(u.getId());
                    }
                }

                StringBuilder invalidUser = new StringBuilder();

                if (!cantBan.isEmpty()) {
                    if (cantBan.size() == 1) {
                        invalidUser.append(LangID.getStringByID("serverConfig.commandBan.cantBan.singular", lang));
                    } else {
                        invalidUser.append(LangID.getStringByID("serverConfig.commandBan.cantBan.plural", lang).formatted(cantBan.size()));
                    }

                    invalidUser.append("\n");

                    for (int i = 0; i < cantBan.size(); i++) {
                        invalidUser.append(cantBan.get(i));

                        if (i < cantBan.size() - 1) {
                            invalidUser.append("\n");
                        }
                    }
                }

                for (String id : ids) {
                    if (holder.banned.contains(id)) {
                        holder.banned.remove(id);
                    } else {
                        holder.banned.add(id);
                    }
                }

                if (!invalidUser.isEmpty()) {
                    event.deferReply()
                            .setContent(invalidUser.toString())
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();

                    applyResult();
                } else {
                    applyResult(event);
                }
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

                end();
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end();
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("serverConfig.permission.documentation.title", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.permission.documentation.commandBan.title", lang).formatted(Emoji.fromUnicode("🔨"))).append("\n")
                .append(LangID.getStringByID("serverConfig.commandBan.description", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.commandBan.bannedUser", lang)).append("\n");

        if (holder.banned.isEmpty()) {
            builder.append(LangID.getStringByID("serverConfig.commandBan.noBanned", lang));
        } else {
            int size = Math.min(holder.banned.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

            for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                builder.append(i + 1).append(". ").append("<@").append(holder.banned.get(i)).append("> [").append(holder.banned.get(i)).append("]");

                if (i < size - 1) {
                    builder.append("\n");
                }
            }

            if (holder.banned.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = (int) Math.ceil(holder.banned.size() * 1.0 / SearchHolder.PAGE_CHUNK);

                builder.append("\n").append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        result.add(ActionRow.of(
                EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder(LangID.getStringByID("serverConfig.commandBan.selectUser", lang))
                        .setRequiredRange(1, EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                        .build()
        ));

        if (holder.banned.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = holder.banned.size() / SearchHolder.PAGE_CHUNK;

            if(holder.banned.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS));
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
                }
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
}

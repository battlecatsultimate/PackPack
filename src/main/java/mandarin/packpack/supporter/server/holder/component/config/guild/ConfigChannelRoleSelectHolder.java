package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigChannelRoleSelectHolder extends ServerConfigHolder {
    private final List<Long> roles;

    private int page = 0;

    public ConfigChannelRoleSelectHolder(@Nullable Message author, long userID, long channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);

        roles = new ArrayList<>();

        roles.add(holder.member);

        roles.add(holder.booster);

        roles.addAll(holder.ID.values());
    }

    public ConfigChannelRoleSelectHolder(@Nullable Message author, long userID, long channelID, @Nonnull Message message, @Nonnull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, lang);

        roles = new ArrayList<>();

        roles.add(holder.member);

        roles.add(holder.booster);

        roles.addAll(holder.ID.values());
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "role" -> {
                if (!(event instanceof StringSelectInteractionEvent e)) {
                    return;
                }

                long id = StaticStore.safeParseLong(e.getValues().getFirst());

                connectTo(event, new ConfigChannelManageHolder(getAuthorMessage(), userID, channelID, message, holder, backup, id, lang));
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
    public void clean() {

    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
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
                .append(LangID.getStringByID("serverConfig.permission.documentation.channelPermission.title", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.description", lang)).append("\n")
                .append(LangID.getStringByID("serverConfig.channelPermission.assignedRole", lang)).append("\n");

        if (roles.isEmpty()) {
            builder.append(LangID.getStringByID("serverConfig.channelPermission.noRoleAssigned", lang));
        } else {
            int size = Math.min(roles.size(), (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            int index = 0;

            for (int i = page * ConfigHolder.SearchLayout.COMPACTED.chunkSize; i < size; i++) {
                long id = roles.get(i);
                boolean isCustom = true;

                builder.append(index + 1).append(". ");

                if (i == 0 && id != -1L) {
                    index++;

                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.member.text", lang));
                } else if (i == 1 && id != -1L) {
                    index++;

                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.booster.text", lang));
                } else {
                    index++;

                    final long finalID = id;

                    String foundRoleName = holder.ID.keySet().stream().filter(k -> {
                        long v = holder.ID.get(k);

                        return v != -1L && v == finalID;
                    }).findAny().orElse("UNKNOWN");

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.custom.text", lang).formatted(foundRoleName));
                }

                if (id == -1L) {
                    builder.append("@everyone");
                } else {
                    builder.append("<@&").append(id).append("> [").append(id).append("]");
                }

                if (isCustom) {
                    builder.append(" <").append(LangID.getStringByID("serverConfig.channelPermission.role.custom.type", lang)).append(">");
                }

                if (i < size - 1) {
                    builder.append("\n");
                }
            }
        }

        if (roles.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = (int) Math.ceil(roles.size() * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            builder.append("\n").append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
        }

        return builder.toString();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        List<SelectOption> roleOptions = new ArrayList<>();

        if (roles.isEmpty()) {
            roleOptions.add(SelectOption.of("A", "A"));
        } else {
            int size = Math.min(roles.size(), (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            for (int i = page * ConfigHolder.SearchLayout.COMPACTED.chunkSize; i < size; i++) {
                long id = roles.get(i);
                String label;
                long value;

                if (i == 0) {
                    if (id == -1L) {
                        value = IDHolder.MEMBER_INDICATOR;
                    } else {
                        value = id;
                    }

                    label = LangID.getStringByID("serverConfig.channelPermission.role.member.type", lang);
                } else if (i == 1) {
                    if (id == -1L) {
                        continue;
                    }

                    value = id;

                    label = LangID.getStringByID("serverConfig.channelPermission.role.booster.type", lang);
                } else {
                    value = id;

                    label = holder.ID.keySet().stream().filter(k -> {
                        long v = holder.ID.get(k);

                        return v != -1L && v == value;
                    }).findAny().orElse("UNKNOWN") + " <" + LangID.getStringByID("serverConfig.channelPermission.role.custom.type", lang) + ">";
                }

                if (id == -1L) {
                    roleOptions.add(SelectOption.of(label, String.valueOf(value)).withDescription("@everyone"));
                } else {
                    roleOptions.add(SelectOption.of(label, String.valueOf(value)).withDescription(String.valueOf(id)));
                }
            }
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("role")
                        .addOptions(roleOptions)
                        .setPlaceholder(LangID.getStringByID("serverConfig.channelPermission.selectRole", lang))
                        .setDisabled(roles.isEmpty())
                        .build()
        ));

        if (roles.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = (int) Math.ceil(roles.size() * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize);

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

        if (parent != null) {
            result.add(
                    ActionRow.of(
                            Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                            Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        } else {
            result.add(
                    ActionRow.of(
                            Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        }

        return result;
    }
}

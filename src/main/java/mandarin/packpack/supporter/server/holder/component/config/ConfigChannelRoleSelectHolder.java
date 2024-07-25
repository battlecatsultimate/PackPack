package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigChannelRoleSelectHolder extends ServerConfigHolder {
    private final List<String> roles;

    private int page = 0;

    public ConfigChannelRoleSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);

        roles = new ArrayList<>();

        if (holder.member != null) {
            roles.add("MEMBER|" + holder.member);
        } else {
            roles.add("MEMBER|");
        }

        if (holder.booster != null) {
            roles.add("BOOSTER|" + holder.booster);
        }

        roles.addAll(holder.ID.values());
    }

    public ConfigChannelRoleSelectHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, lang);

        roles = new ArrayList<>();

        if (holder.member != null) {
            roles.add("MEMBER|" + holder.member);
        } else {
            roles.add("MEMBER|");
        }

        if (holder.booster != null) {
            roles.add("BOOSTER|" + holder.booster);
        }

        roles.addAll(holder.ID.values());
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "role" -> {
                if (!(event instanceof StringSelectInteractionEvent e)) {
                    return;
                }

                String id = e.getValues().getFirst();

                connectTo(event, new ConfigChannelManageHolder(getAuthorMessage(), channelID, message, holder, backup, id, lang));
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

                expired = true;
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

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, @NotNull Holder child) {
        applyResult(event);
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
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
            int size = Math.min(roles.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

            for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                String id = roles.get(i);
                boolean isCustom = true;

                builder.append(i + 1).append(". ");

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.member.text", lang));
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.booster.text", lang));
                } else {
                    final String finalID = id;

                    String foundRoleName = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(finalID);
                    }).findAny().orElse("UNKNOWN");

                    builder.append(LangID.getStringByID("serverConfig.channelPermission.role.custom.text", lang).formatted(foundRoleName));
                }

                if (id.isEmpty()) {
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

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n").append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage));
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> roleOptions = new ArrayList<>();

        if (roles.isEmpty()) {
            roleOptions.add(SelectOption.of("A", "A"));
        } else {
            int size = Math.min(roles.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

            for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                String id = roles.get(i);
                String label;
                String value;

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");

                    if (id.isEmpty()) {
                        value = "Member";
                    } else {
                        value = id;
                    }

                    label = LangID.getStringByID("serverConfig.channelPermission.role.member.type", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    value = id;
                    label = LangID.getStringByID("serverConfig.channelPermission.role.booster.type", lang);
                } else {
                    value = id;

                    label = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(value);
                    }).findAny().orElse("UNKNOWN") + " <" + LangID.getStringByID("serverConfig.channelPermission.role.custom.type", lang) + ">";
                }

                if (id.isEmpty()) {
                    roleOptions.add(SelectOption.of(label, value).withDescription("@everyone"));
                } else {
                    roleOptions.add(SelectOption.of(label, value).withDescription(id));
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

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

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

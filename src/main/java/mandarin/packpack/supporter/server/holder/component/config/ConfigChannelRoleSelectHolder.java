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
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang));

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

        builder.append(LangID.getStringByID("sercon_permission", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannel", lang).formatted(Emoji.fromUnicode("📜"))).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannelrole", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionchannellist", lang)).append("\n");

        if (roles.isEmpty()) {
            builder.append(LangID.getStringByID("sercon_permissionchannelnorole", lang));
        } else {
            int size = Math.min(roles.size(), (page + 1) * SearchHolder.PAGE_CHUNK);

            for (int i = page * SearchHolder.PAGE_CHUNK; i < size; i++) {
                String id = roles.get(i);
                boolean isCustom = true;

                builder.append(i + 1).append(". ");

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("sercon_permissionrolemember", lang));
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    isCustom = false;

                    builder.append(LangID.getStringByID("sercon_permissionrolelbooster", lang));
                } else {
                    final String finalID = id;

                    String foundRoleName = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(finalID);
                    }).findAny().orElse("UNKNOWN");

                    builder.append(LangID.getStringByID("sercon_permissionrolecustom", lang).formatted(foundRoleName));
                }

                if (id.isEmpty()) {
                    builder.append("@everyone");
                } else {
                    builder.append("<@&").append(id).append("> [").append(id).append("]");
                }

                if (isCustom) {
                    builder.append(" <").append(LangID.getStringByID("sercon_permissionrolecustomtype", lang)).append(">");
                }

                if (i < size - 1) {
                    builder.append("\n");
                }
            }
        }

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(page + 1, totalPage));
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

                    label = LangID.getStringByID("sercon_permissionrolemembertype", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    value = id;
                    label = LangID.getStringByID("sercon_permissionrolelboostertype", lang);
                } else {
                    value = id;

                    label = holder.ID.keySet().stream().filter(k -> {
                        String v = holder.ID.get(k);

                        return v != null && v.equals(value);
                    }).findAny().orElse("UNKNOWN") + " <" + LangID.getStringByID("sercon_permissionrolecustomtype", lang) + ">";
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
                        .setPlaceholder(LangID.getStringByID("sercon_permissionchannelselect", lang))
                        .setDisabled(roles.isEmpty())
                        .build()
        ));

        if (roles.size() > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = (int) Math.ceil(roles.size() * 1.0 / SearchHolder.PAGE_CHUNK);

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

        if (parent != null) {
            result.add(
                    ActionRow.of(
                            Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                            Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        } else {
            result.add(
                    ActionRow.of(
                            Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                            Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                    )
            );
        }

        return result;
    }
}

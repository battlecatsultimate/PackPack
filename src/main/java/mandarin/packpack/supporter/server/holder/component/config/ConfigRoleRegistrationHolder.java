package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigRoleRegistrationHolder extends ServerConfigHolder {
    public ConfigRoleRegistrationHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "moderator" -> {
                if (!(event instanceof EntitySelectInteractionEvent ee))
                    return;

                List<Role> roles = ee.getMentions().getRoles();

                if (roles.isEmpty())
                    return;

                Role role = roles.getFirst();

                if (role == null)
                    return;

                if (roleAlreadyRegistered(event, role)) {
                    return;
                }

                holder.MOD = role.getId();

                event.deferReply()
                        .setContent(LangID.getStringByID("sercon_modset", lang).formatted("<@&" + role.getId() + ">"))
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "member", "booster" -> {
                if (!(event instanceof EntitySelectInteractionEvent ee))
                    return;

                List<Role> roles = ee.getMentions().getRoles();

                Role role;

                if (roles.isEmpty()) {
                    role = null;
                } else {
                    role = roles.getFirst();
                }

                if (role != null && roleAlreadyRegistered(event, role)) {
                    return;
                }

                String roleID;

                if (role == null) {
                    roleID = null;
                } else {
                    roleID = role.getId();
                }

                String id;
                String mention;

                if (event.getComponentId().equals("member")) {
                    holder.MEMBER = roleID;
                    id = "sercon_memset";

                    if (holder.MEMBER == null)
                        mention = "@everyone";
                    else
                        mention = "<@&" + roleID + ">";
                } else {
                    holder.BOOSTER = roleID;
                    id = "sercon_booset";

                    if (holder.BOOSTER == null)
                        mention = LangID.getStringByID("data_none", lang);
                    else
                        mention = "<@&" + roleID + ">";
                }

                event.deferReply()
                        .setContent(LangID.getStringByID(id, lang).formatted(mention))
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "custom" -> connectTo(event, new ConfigRoleCustomHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
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
    public void onBack() {
        applyResult();
    }

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
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

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder(LangID.getStringByID("sercon_roletit", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("sercon_roledesc", lang))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolemod", lang).formatted(EmojiStore.MODERATOR.getFormatted(), "<@&" + holder.MOD + ">"))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolemoddesc", lang))
                .append("\n");

        String memberRole;

        if (holder.MEMBER == null) {
            memberRole = "@everyone";
        } else {
            memberRole = "<@&" + holder.MEMBER + ">";
        }

        builder.append(LangID.getStringByID("sercon_rolemem", lang).formatted(EmojiStore.MEMBER.getFormatted(), memberRole))
                .append("\n")
                .append(LangID.getStringByID("sercon_rolememdesc", lang))
                .append("\n");

        String boosterRole;

        if (holder.BOOSTER == null) {
            boosterRole = LangID.getStringByID("data_none", lang);
        } else {
            boosterRole = "<@&" + holder.BOOSTER + ">";
        }

        builder.append(LangID.getStringByID("sercon_roleboo", lang).formatted(EmojiStore.BOOSTER.getFormatted(), boosterRole))
                .append("\n")
                .append(LangID.getStringByID("sercon_roleboodesc", lang));

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        Guild g = getAuthorMessage().getGuild();

        EntitySelectMenu.DefaultValue moderator = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.MOD))
                .findAny()
                .map(role -> EntitySelectMenu.DefaultValue.role(role.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue member = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.MEMBER))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue booster = g.getRoles()
                .stream()
                .filter(r -> r.getId().equals(holder.BOOSTER))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("moderator", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(moderator)
                                .setRequiredRange(1, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                .setPlaceholder(LangID.getStringByID("sercon_roleevery", lang))
                                .setDefaultValues(member == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { member })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(booster == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { booster })
                                .setPlaceholder(LangID.getStringByID("sercon_rolenone", lang))
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("custom", LangID.getStringByID("sercon_custom", lang)).withEmoji(Emoji.fromUnicode("⚙️"))
                )
        );

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
        } else if (holder.ID.containsValue(id)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_customalready", lang))
                    .setEphemeral(true)
                    .queue();

            return true;
        }

        return false;
    }
}

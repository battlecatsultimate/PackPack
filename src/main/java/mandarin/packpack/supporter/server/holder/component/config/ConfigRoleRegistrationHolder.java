package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigRoleRegistrationHolder extends ServerConfigHolder {
    public ConfigRoleRegistrationHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    public ConfigRoleRegistrationHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "moderator" -> {
                if (!(event instanceof EntitySelectInteractionEvent ee))
                    return;

                List<Role> roles = ee.getMentions().getRoles();

                if (roles.isEmpty()) {
                    holder.moderator = null;

                    applyResult(event);
                } else {
                    Role role = roles.getFirst();

                    if (role == null)
                        return;

                    if (roleAlreadyRegistered(event, role)) {
                        return;
                    }

                    holder.moderator = role.getId();

                    event.deferReply()
                            .setContent(LangID.getStringByID("serverConfig.general.role.set.moderator", lang).formatted("<@&" + role.getId() + ">"))
                            .setAllowedMentions(new ArrayList<>())
                            .setEphemeral(true)
                            .queue();

                    applyResult();
                }
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
                    String oldID = holder.member;
                    holder.member = roleID;
                    id = "serverConfig.general.role.set.member";

                    if (holder.member == null)
                        mention = "@everyone";
                    else
                        mention = "<@&" + roleID + ">";

                    if (holder.member != null && holder.channel.containsKey("Member")) {
                        holder.channel.put(holder.member, holder.channel.get("Member"));
                        holder.channel.remove("Member");
                    } else if (holder.member == null && oldID != null && holder.channel.containsKey(oldID)) {
                        holder.channel.put("Member", holder.channel.get(oldID));
                        holder.channel.remove(oldID);
                    }
                } else {
                    String oldID = holder.booster;
                    holder.booster = roleID;
                    id = "serverConfig.general.role.set.booster";

                    if (holder.booster == null)
                        mention = LangID.getStringByID("data.none", lang);
                    else
                        mention = "<@&" + roleID + ">";

                    if (oldID != null && holder.booster == null) {
                        holder.channel.remove(oldID);
                    }
                }

                event.deferReply()
                        .setContent(LangID.getStringByID(id, lang).formatted(mention))
                        .setAllowedMentions(new ArrayList<>())
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "custom" -> connectTo(event, new ConfigRoleCustomHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
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
    public void onBack(@Nonnull Holder child) {
        applyResult();
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
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

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        String moderatorRole;

        if (holder.moderator == null) {
            moderatorRole = LangID.getStringByID("serverConfig.general.role.anyManager", lang);
        } else {
            moderatorRole = "<@&" + holder.moderator + ">";
        }

        StringBuilder builder = new StringBuilder(LangID.getStringByID("serverConfig.general.role.documentation.title", lang).formatted(EmojiStore.ROLE.getFormatted()))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.description", lang))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.moderator.title", lang).formatted(EmojiStore.MODERATOR.getFormatted(), moderatorRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.moderator.description", lang))
                .append("\n");

        String memberRole;

        if (holder.member == null) {
            memberRole = "@everyone";
        } else {
            memberRole = "<@&" + holder.member + ">";
        }

        builder.append(LangID.getStringByID("serverConfig.general.role.documentation.member.title", lang).formatted(EmojiStore.MEMBER.getFormatted(), memberRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.member.description", lang))
                .append("\n");

        String boosterRole;

        if (holder.booster == null) {
            boosterRole = LangID.getStringByID("data.none", lang);
        } else {
            boosterRole = "<@&" + holder.booster + ">";
        }

        builder.append(LangID.getStringByID("serverConfig.general.role.documentation.booster.title", lang).formatted(EmojiStore.BOOSTER.getFormatted(), boosterRole))
                .append("\n")
                .append(LangID.getStringByID("serverConfig.general.role.documentation.booster.description", lang));

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        Guild g = getAuthorMessage().getGuild();
        List<Role> roles = g.getRoles();

        EntitySelectMenu.DefaultValue moderator = roles
                .stream()
                .filter(r -> r.getId().equals(holder.moderator))
                .findAny()
                .map(role -> EntitySelectMenu.DefaultValue.role(role.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue member = roles
                .stream()
                .filter(r -> r.getId().equals(holder.member))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        EntitySelectMenu.DefaultValue booster = roles
                .stream()
                .filter(r -> r.getId().equals(holder.booster))
                .findAny()
                .map(r -> EntitySelectMenu.DefaultValue.role(r.getId()))
                .orElse(null);

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("moderator", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(moderator == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { moderator })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                .setPlaceholder(LangID.getStringByID("serverConfig.general.role.selectNone.everyone", lang))
                                .setDefaultValues(member == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { member })
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                .setDefaultValues(booster == null ? new EntitySelectMenu.DefaultValue[0] : new EntitySelectMenu.DefaultValue[] { booster })
                                .setPlaceholder(LangID.getStringByID("serverConfig.general.role.selectNone.none", lang))
                                .setRequiredRange(0, 1)
                                .build()
                )
        );

        result.add(
                ActionRow.of(
                        Button.secondary("custom", LangID.getStringByID("serverConfig.general.role.custom", lang)).withEmoji(Emoji.fromUnicode("⚙️"))
                )
        );

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
        } else if (holder.ID.containsValue(id)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.custom.already.custom", lang))
                    .setAllowedMentions(new ArrayList<>())
                    .setEphemeral(true)
                    .queue();

            return true;
        }

        return false;
    }
}

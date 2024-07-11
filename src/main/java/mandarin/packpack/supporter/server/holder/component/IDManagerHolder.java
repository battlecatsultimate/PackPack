package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Conflictable;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IDManagerHolder extends ComponentHolder implements Conflictable {
    private static final int PAGE_SIZE = 8;

    private final IDHolder holder;
    private final Guild g;

    private int page;

    public IDManagerHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message msg, @NotNull IDHolder holder, @NotNull Guild g, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        this.holder = holder;
        this.g = g;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        message.editMessage(LangID.getStringByID("idset_expire", holder.config.lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        switch (event.getComponentId()) {
            case "mod" -> {
                if (event instanceof EntitySelectInteractionEvent e) {
                    List<IMentionable> mentionables = e.getValues();

                    if(!mentionables.isEmpty() && mentionables.getFirst() instanceof Role r) {
                        if(alreadyBeingUsed(r.getId())) {
                            event.deferReply(true)
                                    .setContent(LangID.getStringByID("idset_rolealr", lang))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        } else {
                            holder.moderator = r.getId();

                            event.deferReply(true)
                                    .setContent(String.format(LangID.getStringByID("idset_modset", lang), r.getAsMention(), r.getId()))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        }

                        message.editMessage(generateIDData())
                                .setComponents(registerComponent())
                                .mentionRepliedUser(false)
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            }
            case "member" -> {
                if (event instanceof EntitySelectInteractionEvent e) {
                    List<IMentionable> mentionables = e.getValues();

                    if(!mentionables.isEmpty() && mentionables.getFirst() instanceof Role r) {
                        if(alreadyBeingUsed(r.getId())) {
                            event.deferReply(true)
                                    .setContent(LangID.getStringByID("idset_rolealr", lang))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        } else {
                            holder.member = r.getId();

                            event.deferReply(true)
                                    .setContent(String.format(LangID.getStringByID("idset_memset", lang), r.getAsMention(), r.getId()))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        }
                    } else {
                        holder.member = null;

                        event.deferReply(true)
                                .setContent(LangID.getStringByID("idset_memrem", lang))
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .queue();
                    }

                    message.editMessage(generateIDData())
                            .setComponents(registerComponent())
                            .mentionRepliedUser(false)
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
            case "booster" -> {
                if (event instanceof EntitySelectInteractionEvent e) {
                    List<IMentionable> mentionables = e.getValues();

                    if(!mentionables.isEmpty() && mentionables.getFirst() instanceof Role r) {
                        if(alreadyBeingUsed(r.getId())) {
                            event.deferReply(true)
                                    .setContent(LangID.getStringByID("idset_rolealr", lang))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        } else {
                            holder.booster = r.getId();

                            event.deferReply(true)
                                    .setContent(String.format(LangID.getStringByID("idset_booset", lang), r.getAsMention(), r.getId()))
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        }
                    } else {
                        holder.booster = null;

                        event.deferReply(true)
                                .setContent(LangID.getStringByID("idset_boorem", lang))
                                .setAllowedMentions(new ArrayList<>())
                                .mentionRepliedUser(false)
                                .queue();
                    }

                    message.editMessage(generateIDData())
                            .setComponents(registerComponent())
                            .mentionRepliedUser(false)
                            .setAllowedMentions(new ArrayList<>())
                            .queue();
                }
            }
            case "customManage" -> {
                event.deferEdit()
                        .setContent(getManagerText())
                        .setComponents(getManagerComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();

                expired = true;

                StaticStore.putHolder(userID, new CustomIDManagerHolder(getAuthorMessage(), channelID, message, holder, g, this.lang));
            }
            case "announce" -> {
                if (event instanceof EntitySelectInteractionEvent e) {
                    if (!e.getValues().isEmpty()) {
                        IMentionable mentionable = e.getValues().getFirst();

                        if (mentionable instanceof GuildMessageChannel m) {
                            if (!m.canTalk()) {
                                event.deferReply(true)
                                        .setContent(LangID.getStringByID("idset_chantalk", lang))
                                        .queue();
                            } else {
                                holder.announceChannel = m.getId();

                                event.deferReply(true)
                                        .setContent(String.format(LangID.getStringByID("idset_annset", lang), m.getId(), m.getAsMention()))
                                        .queue();

                                message.editMessage(generateIDData())
                                        .setComponents(registerComponent())
                                        .mentionRepliedUser(false)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue();
                            }
                        }
                    } else {
                        holder.announceChannel = null;

                        event.deferReply(true)
                                .setContent(LangID.getStringByID("idset_annrem", lang))
                                .queue();

                        message.editMessage(generateIDData())
                                .setComponents(registerComponent())
                                .mentionRepliedUser(false)
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            }
            case "announcePost" -> {
                holder.publish = !holder.publish;

                performResult(event);
            }
            case "logDM" -> {
                if (event instanceof EntitySelectInteractionEvent e) {
                    if (!e.getValues().isEmpty()) {
                        IMentionable mentionable = e.getValues().getFirst();

                        if (mentionable instanceof GuildMessageChannel m) {
                            if (!m.canTalk()) {
                                event.deferReply(true)
                                        .setContent(LangID.getStringByID("idset_chantalk", lang))
                                        .queue();
                            } else {
                                holder.logDM = m.getId();

                                event.deferReply(true)
                                        .setContent(String.format(LangID.getStringByID("idset_logset", lang), m.getId(), m.getAsMention()))
                                        .queue();

                                message.editMessage(generateIDData())
                                        .setComponents(registerComponent())
                                        .mentionRepliedUser(false)
                                        .setAllowedMentions(new ArrayList<>())
                                        .queue();
                            }
                        }
                    } else {
                        holder.logDM = null;

                        event.deferReply(true)
                                .setContent(LangID.getStringByID("idset_logrem", lang))
                                .queue();

                        message.editMessage(generateIDData())
                                .setComponents(registerComponent())
                                .mentionRepliedUser(false)
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }
                }
            }
            case "next" -> {
                page++;
                performResult(event);
            }
            case "prev" -> {
                page--;
                performResult(event);
            }
            case "confirm" -> {
                expired = true;

                StaticStore.removeHolder(userID, this);

                event.deferEdit()
                        .setContent(LangID.getStringByID("idset_apply", lang))
                        .setComponents()
                        .queue();
            }
        }
    }

    private void performResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(generateIDData())
                .setComponents(registerComponent())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private String generateIDData() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        StringBuilder result = new StringBuilder();

        String[] data = { "moderator", "member", "booster" };
        String[] ids = { holder.moderator, holder.member, holder.booster};

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0, 1, 2 -> {
                    result.append("**")
                            .append(LangID.getStringByID("idset_" + data[i], lang))
                            .append("** : ");

                    if(ids[i] == null) {
                        result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
                    } else {
                        Role r = getRoleSafelyWithID(ids[i]);

                        if (r == null) {
                            result.append(LangID.getStringByID(i == 1 ? "data_everyone" : "data_none", lang));
                        } else {
                            result.append(r.getId())
                                    .append(" [")
                                    .append(r.getAsMention())
                                    .append("]");
                        }
                    }
                }
                case 3 -> {
                    result.append("**")
                            .append(LangID.getStringByID("idset_custom", lang))
                            .append("**");

                    if (holder.ID.isEmpty()) {
                        result.append(" : ")
                                .append(LangID.getStringByID("data_none", lang));
                    } else {
                        result.append("\n\n");

                        int j = 0;

                        for(String name : holder.ID.keySet()) {
                            if (j == SearchHolder.PAGE_CHUNK)
                                break;

                            String id = holder.ID.get(name);

                            if (id == null)
                                continue;

                            result.append(name)
                                    .append(" : ");

                            Role r = getRoleSafelyWithID(id);

                            if (r == null) {
                                result.append(String.format(LangID.getStringByID("idset_unknown", lang), id));
                            } else {
                                result.append(r.getId())
                                        .append(" [")
                                        .append(r.getAsMention())
                                        .append("]");
                            }

                            result.append("\n");

                            j++;
                        }
                    }
                }
                case 6 -> {
                    result.append("**")
                            .append(LangID.getStringByID("idset_announcement", lang))
                            .append("** : ");

                    if (holder.announceChannel == null)
                        result.append(LangID.getStringByID("data_none", lang));
                    else {
                        GuildChannel ch = getChannelSafelyWithID(holder.announceChannel);

                        if (ch == null) {
                            result.append(String.format(LangID.getStringByID("data_unknown", lang), holder.announceChannel));
                        } else {
                            result.append(ch.getId())
                                    .append(" [")
                                    .append(ch.getAsMention())
                                    .append("]");
                        }
                    }

                    result.append("\n\n");

                    if(holder.announceChannel == null)
                        result.append(LangID.getStringByID("idset_annfalse", lang));
                    else
                        result.append(LangID.getStringByID("idset_anntrue", lang));

                    if(holder.publish) {
                        result.append("\n\n")
                                .append(LangID.getStringByID("idset_post", lang));
                    }
                }
                case 8 -> {
                    result.append("**")
                            .append(LangID.getStringByID("idset_log", lang))
                            .append("** : ");

                    if (holder.logDM == null)
                        result.append(LangID.getStringByID("data_none", lang));
                    else {
                        GuildChannel ch = getChannelSafelyWithID(holder.logDM);

                        if (ch == null) {
                            result.append(String.format(LangID.getStringByID("data_unknown", lang), holder.logDM));
                        } else {
                            result.append(ch.getId())
                                    .append(" [")
                                    .append(ch.getAsMention())
                                    .append("]");
                        }
                    }

                    result.append("\n\n");

                    if(holder.logDM == null)
                        result.append(LangID.getStringByID("idset_logfalse", lang));
                    else
                        result.append(LangID.getStringByID("idset_logtrue", lang));
                }
            }

            if (i < (page + 1) * 3 - 1)
                result.append("\n\n");
        }

        return result.toString().replace("\n\n\n\n", "\n\n");
    }

    private String getManagerText() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        StringBuilder builder = new StringBuilder(LangID.getStringByID("idset_managetitle", lang)).append("\n\n");

        int i = 0;

        for (String key : holder.ID.keySet()) {
            if (i == SearchHolder.PAGE_CHUNK)
                break;

            builder.append(i + 1).append(". ").append(key).append(" : ");

            String id = holder.ID.get(key);

            if (id == null) {
                builder.append("UNKNOWN");
            } else {
                builder.append("<@&").append(id).append("> [").append(id).append("]");
            }

            builder.append("\n");

            i++;
        }

        if(holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = holder.ID.size() / SearchHolder.PAGE_CHUNK;

            if(holder.ID.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage));
        }

        return builder.toString();
    }

    private List<LayoutComponent> registerComponent() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        List<LayoutComponent> components = new ArrayList<>();

        for (int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0 -> components.add(
                        ActionRow.of(
                                EntitySelectMenu.create("mod", EntitySelectMenu.SelectTarget.ROLE)
                                        .setRequiredRange(1, 1)
                                        .setPlaceholder(LangID.getStringByID("idset_modselect", lang))
                                        .build()
                        )
                );
                case 1 -> components.add(
                        ActionRow.of(
                                EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.ROLE)
                                        .setRequiredRange(0, 1)
                                        .setPlaceholder(LangID.getStringByID("idset_memberselect", lang))
                                        .build()
                        )
                );
                case 2 -> components.add(
                        ActionRow.of(
                                EntitySelectMenu.create("booster", EntitySelectMenu.SelectTarget.ROLE)
                                        .setRequiredRange(0, 1)
                                        .setPlaceholder(LangID.getStringByID("idset_boosterselect", lang))
                                        .build()
                        )
                );
                case 3 -> {
                    Button b = Button.secondary("customManage", LangID.getStringByID("idset_custommanage", lang)).withEmoji(Emoji.fromUnicode("\uD83D\uDCDD"));

                    components.add(ActionRow.of(b));
                }
                case 6 -> components.add(
                        ActionRow.of(
                                EntitySelectMenu.create("announce", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
                                        .setRequiredRange(0, 1)
                                        .build()
                        )
                );
                case 7 -> {
                    if(holder.publish) {
                        components.add(ActionRow.of(Button.secondary("announcePost", String.format(LangID.getStringByID("idset_announcepost", lang), LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        components.add(ActionRow.of(Button.secondary("announcePost", String.format(LangID.getStringByID("idset_announcepost", lang), LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 8 -> components.add(
                        ActionRow.of(
                                EntitySelectMenu.create("logDM", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
                                        .setRequiredRange(0, 1)
                                        .build()
                        )
                );
            }
        }

        List<ActionComponent> pages = new ArrayList<>();

        if(page == 0) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));
        } else if((page + 1) * 3 >= PAGE_SIZE) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT).asDisabled());
        } else {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));
        }

        components.add(ActionRow.of(pages));

        components.add(ActionRow.of(Button.primary("confirm", LangID.getStringByID("button_confirm", lang))));

        return components;
    }

    private List<LayoutComponent> getManagerComponents() {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        Button b = Button.secondary("customAdd", LangID.getStringByID("idset_customadd", lang)).withEmoji(Emoji.fromUnicode("➕"));

        result.add(ActionRow.of(b));

        if (!holder.ID.isEmpty()) {
            List<SelectOption> options = new ArrayList<>();

            for(String name : holder.ID.keySet()) {
                if (options.size() == SearchHolder.PAGE_CHUNK)
                    break;

                String id = holder.ID.get(name);

                if(id == null) {
                    options.add(SelectOption.of(name, name).withDescription(LangID.getStringByID("idset_none", lang)));
                } else {
                    Role r = getRoleSafelyWithID(id);

                    if(r == null) {
                        options.add(SelectOption.of(name, name).withDescription(String.format(LangID.getStringByID("idset_unknown", lang), id)));
                    } else {
                        options.add(SelectOption.of(name, name).withDescription(id));
                    }
                }
            }

            result.add(ActionRow.of(StringSelectMenu.create("customRemove").addOptions(options).setRequiredRange(0, 20).setPlaceholder(LangID.getStringByID("idset_customremove", lang)).build()));
        }

        if (holder.ID.size() > SearchHolder.PAGE_CHUNK) {
            List<ActionComponent> pages = new ArrayList<>();

            if (holder.ID.size() > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

            if (holder.ID.size() > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).asDisabled());
            }

            result.add(ActionRow.of(pages));
        }

        result.add(ActionRow.of(Button.primary("back", LangID.getStringByID("button_back", lang))));

        return result;
    }

    private boolean alreadyBeingUsed(String id) {
        if(id.equals("none"))
            return false;

        boolean res = id.equals(holder.moderator) || id.equals(holder.member) || id.equals(holder.booster);

        if(res)
            return true;

        for(String i : holder.ID.values()) {
            if (id.equals(i))
                return true;
        }

        return false;
    }

    private Role getRoleSafelyWithID(String id) {
        if (id == null)
            return null;

        try {
            return g.getRoleById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private GuildChannel getChannelSafelyWithID(String id) {
        if (id == null)
            return null;

        try {
            return g.getGuildChannelById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean isConflicted(Holder holder) {
        if (!(holder instanceof IDManagerHolder i))
            return false;

        return i.g.getId().equals(g.getId());
    }

    @Override
    public void onConflict() {
        expire(userID);
    }
}

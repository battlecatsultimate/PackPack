package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.modal.LevelModalHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigButtonHolder extends ComponentHolder {
    private static final int TOTAL_CONFIG = 7;

    private final Message msg;
    private final ConfigHolder config;
    private final ConfigHolder backup;
    private final IDHolder holder;

    private int page = 0;

    public ConfigButtonHolder(Message author, Message msg, ConfigHolder config, IDHolder holder, String channelID) {
        super(author, channelID, msg);

        this.msg = msg;
        this.config = config;
        this.backup = config.clone();
        this.holder = holder;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), ConfigButtonHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.lang = StaticStore.safeParseInt(es.getValues().getFirst());
                
                performResult(event);
            }
            case "defLevels" -> {
                TextInput input = TextInput.create("level", LangID.getStringByID("config_levelsubject", config.lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("config_levelplace", config.lang))
                        .setRequiredRange(1, 2)
                        .setRequired(true)
                        .setValue(String.valueOf(config.defLevel))
                        .build();

                Modal modal = Modal.create("level", LangID.getStringByID("config_leveltitle", config.lang))
                        .addActionRow(input)
                        .build();
                
                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new LevelModalHolder(getAuthorMessage(), msg, channelID, config, e -> e.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(parseComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue()));
            }
            case "extra" -> {
                config.extra = !config.extra;
                
                performResult(event);
            }
            case "unit" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.useFrame = es.getValues().getFirst().equals("frame");
                
                performResult(event);
            }
            case "compact" -> {
                config.compact = !config.compact;
                
                performResult(event);
            }
            case "force" -> {
                holder.forceCompact = !holder.forceCompact;

                performResult(event);
            }
            case "trueForm" -> {
                config.trueForm = !config.trueForm;
                
                performResult(event);
            }
            case "treasure" -> {
                config.treasure = !config.treasure;

                performResult(event);
            }
            case "forceTreasure" -> {
                holder.forceFullTreasure = !holder.forceFullTreasure;

                performResult(event);
            }
            case "boosterPin" -> {
                holder.boosterPin = !holder.boosterPin;

                performResult(event);
            }
            case "boosterPinChannel" -> {
                if(event instanceof EntitySelectInteractionEvent e) {
                    List<IMentionable> mentionables = e.getValues();

                    holder.boosterPinChannel.clear();

                    StringBuilder result = new StringBuilder();

                    int succeed = 0;

                    for(int i = 0; i < mentionables.size(); i++) {
                        if(mentionables.get(i) instanceof GuildMessageChannel t) {
                            if(t.canTalk() && t.getGuild().getSelfMember().hasPermission(t, Permission.MESSAGE_MANAGE)) {
                                holder.boosterPinChannel.add(t.getId());
                                succeed++;
                            } else {
                                result.append(String.format(LangID.getStringByID("config_bootalk", config.lang), t.getAsMention()))
                                        .append("\n\n");
                            }
                        } else if(mentionables.get(i) instanceof ForumChannel f) {
                            if(f.getGuild().getSelfMember().hasPermission(f, Permission.MESSAGE_MANAGE, Permission.MESSAGE_SEND, Permission.MESSAGE_SEND_IN_THREADS)) {
                                holder.boosterPinChannel.add(f.getId());
                                succeed++;
                            } else {
                                result.append(String.format(LangID.getStringByID("config_bootalk", config.lang), f.getAsMention()))
                                        .append("\n\n");
                            }
                        }
                    }

                    if(succeed != mentionables.size()) {
                        if(succeed != 0) {
                            result.append(LangID.getStringByID("config_boootherset", config.lang));
                        }

                        event.deferReply(true)
                                .setContent(result.toString())
                                .mentionRepliedUser(false)
                                .queue();
                    } else {
                        event.deferReply(true)
                                .setContent(LangID.getStringByID("config_boosuccess", config.lang))
                                .mentionRepliedUser(false)
                                .queue();
                    }
                }

                msg.editMessage(parseMessage())
                        .setComponents(parseComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue();
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

                int lang = config.lang;

                if (lang == -1)
                    lang = holder == null ? LangID.EN : holder.config.lang;

                event.deferEdit()
                        .setContent(LangID.getStringByID("config_apply", lang))
                        .setComponents()
                        .queue();

                if (!StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, config);
                }
            }
            case "cancel" -> {
                expired = true;

                StaticStore.removeHolder(userID, this);

                if(StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, backup);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("config_cancel", backup.lang))
                        .setComponents()
                        .queue();
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        if(StaticStore.config.containsKey(id)) {
            StaticStore.config.put(id, backup);
        }

        msg.editMessage(LangID.getStringByID("config_expire", config.lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }

    private void performResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(parseMessage())
                .setComponents(parseComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private List<ActionRow> parseComponents() {
        int lang = config.lang;

        if(lang == -1)
            lang = holder == null ? LangID.EN : holder.config.lang;

        List<ActionRow> m = new ArrayList<>();

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0 -> m.add(ActionRow.of(Button.secondary("defLevels", String.format(LangID.getStringByID("config_setlevel", lang), config.defLevel)).withEmoji(Emoji.fromUnicode("⚙"))));
                case 1 -> {
                    if(config.extra) {
                        m.add(ActionRow.of(Button.secondary("extra", LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("extra", LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 2 -> {
                    List<SelectOption> languages = new ArrayList<>();

                    languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(config.lang == -1));

                    for (int j = 0; j < StaticStore.langIndex.length; j++) {
                        String l = LangID.getStringByID("lang_" + StaticStore.langCode[j], lang);

                        languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), String.valueOf(StaticStore.langIndex[j])).withDefault(config.lang == StaticStore.langIndex[j]));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()));
                }
                case 3 -> {
                    if(config.compact) {
                        m.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 4 -> {
                    if(config.trueForm) {
                        m.add(ActionRow.of(Button.secondary("trueForm", String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("trueForm", String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 5 -> {
                    List<SelectOption> units = new ArrayList<>();

                    if (config.useFrame) {
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame").withDefault(true));
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
                    } else {
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second").withDefault(true));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("unit").addOptions(units).build()));
                }
                case 6 -> {
                    if(config.treasure) {
                        m.add(ActionRow.of(Button.secondary("treasure", String.format(LangID.getStringByID("config_treasure", lang), LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("treasure", String.format(LangID.getStringByID("config_treasure", lang), LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
            }
        }

        List<ActionComponent> pages = new ArrayList<>();

        if(page == 0) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));
        } else if((page + 1) * 3 >= TOTAL_CONFIG) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT).asDisabled());
        } else {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));
        }

        m.add(ActionRow.of(pages));

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        m.add(ActionRow.of(components));

        return m;
    }

    private String parseMessage() {
        int lang = config.lang;

        if(lang == -1)
            lang = holder == null ? LangID.EN : holder.config.lang;

        StringBuilder message = new StringBuilder();

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0 -> message.append("**")
                        .append(LangID.getStringByID("config_default", lang).replace("_", String.valueOf(config.defLevel)))
                        .append("**\n\n")
                        .append(LangID.getStringByID("config_deflvdesc", lang).replace("_", String.valueOf(config.defLevel)));
                case 1 -> {
                    String ex = LangID.getStringByID(config.extra ? "config_extrue" : "config_exfalse", lang);
                    String bool = LangID.getStringByID(config.extra ? "data_true" : "data_false", lang);

                    message.append("**")
                            .append(LangID.getStringByID("config_extra", lang).replace("_", bool))
                            .append("**\n\n")
                            .append(ex);
                }
                case 2 -> {
                    String locale = switch (config.lang) {
                        case LangID.EN -> LangID.getStringByID("lang_en", lang);
                        case LangID.JP -> LangID.getStringByID("lang_jp", lang);
                        case LangID.KR -> LangID.getStringByID("lang_kr", lang);
                        case LangID.ZH -> LangID.getStringByID("lang_zh", lang);
                        case LangID.FR -> LangID.getStringByID("lang_fr", lang);
                        case LangID.IT -> LangID.getStringByID("lang_it", lang);
                        case LangID.ES -> LangID.getStringByID("lang_es", lang);
                        case LangID.DE -> LangID.getStringByID("lang_de", lang);
                        case LangID.TH -> LangID.getStringByID("lang_th", lang);
                        case LangID.RU -> LangID.getStringByID("lang_ru", lang);
                        default -> LangID.getStringByID("config_auto", lang);
                    };

                    message.append("**")
                            .append(LangID.getStringByID("config_locale", lang).replace("_", locale))
                            .append("**");
                }
                case 3 -> {
                    String compact = LangID.getStringByID(config.compact ? "data_true" : "data_false", lang);
                    String comp = LangID.getStringByID(config.compact ? "config_comtrue" : "config_comfalse", lang);

                    message.append("**")
                            .append(LangID.getStringByID("config_compact", lang).replace("_", compact))
                            .append("**\n\n")
                            .append(comp);
                }
                case 4 -> {
                    String trueForm = LangID.getStringByID(config.trueForm ? "data_true" : "data_false", lang);
                    String tr = LangID.getStringByID(config.trueForm ? "config_truetrue" : "config_truefalse", lang);

                    message.append("**")
                            .append(String.format(LangID.getStringByID("config_trueform", lang), trueForm))
                            .append("**\n\n")
                            .append(tr);
                }
                case 5 -> {
                    String unit = LangID.getStringByID(config.useFrame ? "config_frame" : "config_second", lang);

                    message.append("**")
                            .append(LangID.getStringByID("config_unit", lang).replace("_", unit))
                            .append("**\n\n")
                            .append(LangID.getStringByID("config_unitdesc", lang));
                }
                case 6 -> {
                    String treasure = LangID.getStringByID(config.treasure ? "data_true" : "data_false", lang);
                    String trea = LangID.getStringByID(config.treasure ? "config_treasuretrue" : "config_treasurefalse", lang);

                    message.append("**")
                            .append(String.format(LangID.getStringByID("config_treasure", lang), treasure))
                            .append("**\n\n")
                            .append(trea);
                }
            }

            if(i < (page + 1) * 3 - 1) {
                message.append("\n\n");
            }
        }

        return message.toString();
    }
}

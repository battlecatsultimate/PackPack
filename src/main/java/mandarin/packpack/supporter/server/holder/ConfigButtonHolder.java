package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.segment.InteractionHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ConfigButtonHolder extends InteractionHolder {
    private static final int TOTAL_CONFIG = 6;

    private final Message msg;
    private final ConfigHolder config;
    private final ConfigHolder backup;
    private final IDHolder holder;
    private final boolean forServer;

    private int page = 0;

    public ConfigButtonHolder(Message author, Message msg, ConfigHolder config, IDHolder holder, String channelID, boolean forServer) {
        super(author, channelID, msg.getId());

        this.msg = msg;
        this.config = config;
        this.backup = config.clone();
        this.holder = holder;
        
        this.forServer = forServer;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), ConfigButtonHolder.this);

                expire(userID);
            }
        }, FIVE_MIN);
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.lang = StaticStore.safeParseInt(es.getValues().get(0));
                
                performResult(event);
            }
            case "defLevels" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.defLevel = StaticStore.safeParseInt(es.getValues().get(0));
                
                performResult(event);
            }
            case "extra" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.extra = es.getValues().get(0).equals("true");
                
                performResult(event);
            }
            case "unit" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.useFrame = es.getValues().get(0).equals("frame");
                
                performResult(event);
            }
            case "compact" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.compact = es.getValues().get(0).equals("true");
                
                performResult(event);
            }
            case "force" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (!forServer) {
                    StaticStore.logger.uploadLog("W/ConfigButtonHolder::performInteraction - Force compact mode is visible for personal config");

                    return;
                }
                
                holder.forceCompact = es.getValues().get(0).equals("true");
                
                performResult(event);
            }
            case "trueform" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.trueForm = es.getValues().get(0).equals("true");
                
                performResult(event);
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

                if (!forServer && !StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, config);
                }
            }
            case "cancel" -> {
                expired = true;

                StaticStore.removeHolder(userID, this);

                if (forServer) {
                    holder.config = backup;
                } else {
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
                case 0 -> {
                    List<SelectOption> languages = new ArrayList<>();

                    if (!forServer) {
                        if (config.lang == -1)
                            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(true));
                        else
                            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1"));
                    }

                    for (int j = 0; j < StaticStore.langIndex.length; j++) {
                        String l = LangID.getStringByID("lang_" + StaticStore.langCode[j], lang);

                        if (config.lang == StaticStore.langIndex[j]) {
                            languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), String.valueOf(StaticStore.langIndex[j])).withDefault(true));
                        } else {
                            languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), String.valueOf(StaticStore.langIndex[j])));
                        }
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()));
                }
                case 1 -> {
                    List<SelectOption> levels = new ArrayList<>();

                    for (int j = 0; j <= 50; j += 5) {
                        final String level = j == 0 ? "1" : String.valueOf(j);

                        if (config.defLevel == j) {
                            levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", level), level).withDefault(true));
                        } else {
                            levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", level), level));
                        }
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("defLevels").addOptions(levels).build()));
                }
                case 2 -> {
                    List<SelectOption> extras = new ArrayList<>();

                    if (config.extra) {
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
                    } else {
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("extra").addOptions(extras).build()));
                }
                case 3 -> {
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
                case 4 -> {
                    List<SelectOption> compacts = new ArrayList<>();

                    if (config.compact) {
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
                    } else {
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("compact").addOptions(compacts).build()));
                }
                case 5 -> {
                    List<SelectOption> trueForms = new ArrayList<>();

                    if (config.trueForm) {
                        trueForms.add(SelectOption.of(String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                        trueForms.add(SelectOption.of(String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_false", lang)), "false"));
                    } else {
                        trueForms.add(SelectOption.of(String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_true", lang)), "true"));
                        trueForms.add(SelectOption.of(String.format(LangID.getStringByID("config_trueform", lang), LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("trueform").addOptions(trueForms).build()));
                }
                case 6 -> {
                    if (forServer && holder != null) {
                        List<SelectOption> forces = new ArrayList<>();

                        if (holder.forceCompact) {
                            forces.add(SelectOption.of(LangID.getStringByID("config_force", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                            forces.add(SelectOption.of(LangID.getStringByID("config_force", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
                        } else {
                            forces.add(SelectOption.of(LangID.getStringByID("config_force", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                            forces.add(SelectOption.of(LangID.getStringByID("config_force", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                        }

                        m.add(ActionRow.of(StringSelectMenu.create("force").addOptions(forces).build()));
                    }
                }
            }
        }

        List<ActionComponent> pages = new ArrayList<>();

        if(page == 0) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)));
        } else if((page + 1) * 3 >= (holder != null && forServer ? TOTAL_CONFIG + 1 : TOTAL_CONFIG)) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)).asDisabled());
        } else {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)));
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
            default -> LangID.getStringByID("config_auto", lang);
        };

        String ex;
        String bool;

        if(config.extra) {
            ex = LangID.getStringByID("config_extrue", lang);
            bool = LangID.getStringByID("data_true", lang);
        } else {
            ex = LangID.getStringByID("config_exfalse", lang);
            bool = LangID.getStringByID("data_false", lang);
        }

        String unit;

        if(config.useFrame)
            unit = LangID.getStringByID("config_frame", lang);
        else
            unit = LangID.getStringByID("config_second", lang);

        String compact;
        String comp;

        if(config.compact) {
            compact = LangID.getStringByID("data_true", lang);
            comp = LangID.getStringByID("config_comtrue", lang);
        } else {
            compact = LangID.getStringByID("data_false", lang);
            comp = LangID.getStringByID("config_comfalse", lang);
        }

        String trueForm;
        String tr;

        if(config.trueForm) {
            trueForm = LangID.getStringByID("data_true", lang);
            tr = LangID.getStringByID("config_truetrue", lang);
        } else {
            trueForm = LangID.getStringByID("data_false", lang);
            tr = LangID.getStringByID("config_truefalse", lang);
        }

        String message = "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                "**" + LangID.getStringByID("config_default", lang).replace("_", String.valueOf(config.defLevel)) + "**\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", String.valueOf(config.defLevel)) + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                LangID.getStringByID("config_unitdesc", lang) + "\n\n" +
                "**" + LangID.getStringByID("config_compact", lang).replace("_", compact) + "**\n" +
                comp + "\n\n" +
                "**" + String.format(LangID.getStringByID("config_trueform", lang), trueForm) + "**\n" +
                tr;

        if(forServer) {
            String force = LangID.getStringByID((holder != null && holder.forceCompact) ? "data_true" : "data_false", lang);
            String forc = LangID.getStringByID((holder != null && holder.forceCompact) ? "config_fortrue" : "config_forfalse", lang);

            message += "\n\n" +
                    "**" + LangID.getStringByID("config_force", lang).replace("_", force) + "**\n" +
                    forc;
        }

        return message;
    }
}

package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ConfigButtonHolder extends InteractionHolder<GenericComponentInteractionCreateEvent> {
    private final Message msg;
    private final ConfigHolder config;
    private final ConfigHolder backup;
    private final IDHolder holder;

    private final String channelID;
    private final String memberID;
    private final boolean forServer;

    private int page = 0;

    public ConfigButtonHolder(Message msg, Message author, ConfigHolder config, IDHolder holder, String channelID, String memberID, boolean forServer) {
        super(GenericComponentInteractionCreateEvent.class, author);

        this.msg = msg;
        this.config = config;
        this.backup = config.clone();
        this.holder = holder;

        this.channelID = channelID;
        this.memberID = memberID;
        this.forServer = forServer;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), ConfigButtonHolder.this);

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();

        if (!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember();

        if(!mem.getId().equals(memberID))
            return RESULT_STILL;

        Message m = event.getMessage();

        if(!m.getId().equals(msg.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language":
                SelectMenuInteractionEvent es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                config.lang = StaticStore.safeParseInt(es.getValues().get(0));

                performResult(event);

                break;
            case "defLevels":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                config.defLevel = StaticStore.safeParseInt(es.getValues().get(0));

                performResult(event);

                break;
            case "extra":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                config.extra = es.getValues().get(0).equals("true");

                performResult(event);

                break;
            case "unit":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                config.useFrame = es.getValues().get(0).equals("frame");

                performResult(event);

                break;
            case "compact":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                config.compact = es.getValues().get(0).equals("true");

                performResult(event);

                break;
            case "next":
                page++;

                performResult(event);

                break;
            case "prev":
                page--;

                performResult(event);

                break;
            case "confirm":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                StaticStore.config.put(memberID, config);

                int lang = config.lang;

                if(lang == -1)
                    lang = holder.config.lang;

                event.deferEdit()
                        .setContent(LangID.getStringByID("config_apply", lang))
                        .setActionRows()
                        .queue();

                break;
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                if(forServer) {
                    holder.config = backup;
                } else {
                    StaticStore.config.put(memberID, backup);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("config_cancel", backup.lang))
                        .setActionRows()
                        .queue();

                break;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("config_expire", config.lang))
                .setActionRows()
                .queue();
    }

    private void performResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(parseMessage())
                .setActionRows(parseComponents())
                .queue();
    }

    private List<ActionRow> parseComponents() {
        int lang = config.lang;

        if(lang == -1)
            lang = holder.config.lang;

        List<ActionRow> m = new ArrayList<>();

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0:
                    List<SelectOption> languages = new ArrayList<>();

                    if(!forServer) {
                        if(config.lang == -1)
                            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(true));
                        else
                            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1"));
                    }

                    for(int j = 0; j < StaticStore.langIndex.length; j++) {
                        String l = LangID.getStringByID("lang_"+StaticStore.langCode[j], lang);

                        if(config.lang == StaticStore.langIndex[j]) {
                            languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[j]).withDefault(true));
                        } else {
                            languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[j]));
                        }
                    }

                    m.add(ActionRow.of(SelectMenu.create("language").addOptions(languages).build()));

                    break;
                case 1:
                    List<SelectOption> levels = new ArrayList<>();

                    for(int j = 0; j <= 50; j += 5) {
                        if(config.defLevel == j) {
                            levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", j == 0 ? "1" : ""+j), j == 0 ? "1" : ""+j).withDefault(true));
                        } else {
                            levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", j == 0 ? "1" : ""+j), j == 0 ? "1" : ""+j));
                        }
                    }

                    m.add(ActionRow.of(SelectMenu.create("defLevels").addOptions(levels).build()));

                    break;
                case 2:
                    List<SelectOption> extras = new ArrayList<>();

                    if(config.extra) {
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
                    } else {
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                        extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                    }

                    m.add(ActionRow.of(SelectMenu.create("extra").addOptions(extras).build()));

                    break;
                case 3:
                    List<SelectOption> units = new ArrayList<>();

                    if(config.useFrame) {
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame").withDefault(true));
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
                    } else {
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
                        units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second").withDefault(true));
                    }

                    m.add(ActionRow.of(SelectMenu.create("unit").addOptions(units).build()));

                    break;
                case 4:
                    List<SelectOption> compacts = new ArrayList<>();

                    if(config.compact) {
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
                    } else {
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                        compacts.add(SelectOption.of(LangID.getStringByID("config_compact", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
                    }

                    m.add(ActionRow.of(SelectMenu.create("compact").addOptions(compacts).build()));
                    break;
            }
        }

        List<ActionComponent> pages = new ArrayList<>();

        if(page == 0) {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)));
        } else {
            pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)));
            pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)).asDisabled());
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
            lang = holder.config.lang;

        String locale;

        switch (config.lang) {
            case LangID.EN:
                locale = LangID.getStringByID("lang_en", lang);
                break;
            case LangID.JP:
                locale = LangID.getStringByID("lang_jp", lang);
                break;
            case LangID.KR:
                locale = LangID.getStringByID("lang_kr", lang);
                break;
            case LangID.ZH:
                locale = LangID.getStringByID("lang_zh", lang);
                break;
            case LangID.FR:
                locale = LangID.getStringByID("lang_fr", lang);
                break;
            case LangID.IT:
                locale = LangID.getStringByID("lang_it", lang);
                break;
            case LangID.ES:
                locale = LangID.getStringByID("lang_es", lang);
                break;
            case LangID.DE:
                locale = LangID.getStringByID("lang_de", lang);
                break;
            default:
                locale = LangID.getStringByID("config_auto", lang);
                break;
        }

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

        return "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                "**" + LangID.getStringByID("config_default", lang).replace("_", ""+ config.defLevel) + "**\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", config.defLevel+"") + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                LangID.getStringByID("config_unitdesc", lang) + "\n\n" +
                "**" + LangID.getStringByID("config_compact", lang).replace("_", compact) + "**\n" +
                comp;
    }
}

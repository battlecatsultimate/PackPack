package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.bc.FindStage;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class FindStageMessageHolder extends SearchHolder {
    private final List<Stage> stage;
    private final List<Stage> actualStage = new ArrayList<>();
    private final List<FindStage.MONTHLY> monthly;

    private final TreasureHolder treasure;
    private final StageInfo.StageInfoConfig configData;

    private FindStage.MONTHLY selected = FindStage.MONTHLY.ALL;

    public FindStageMessageHolder(List<Stage> stage, List<FindStage.MONTHLY> monthly, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, TreasureHolder treasure, StageInfo.StageInfoConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.stage = stage;
        this.monthly = monthly;

        this.treasure = treasure;
        this.configData = configData;

        actualStage.addAll(stage);
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1); i++) {
            if(i >= stage.size())
                break;

            Stage st = stage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            String fullName = "";

            if (mc != null) {
                String mcName = StaticStore.safeMultiLangGet(mc, lang);

                if (mcName == null || mcName.isBlank()) {
                    mcName = DataToString.getMapCode(mc);
                }

                fullName += mcName + " - ";
            } else {
                fullName += "Unknown - ";
            }

            String stmName = StaticStore.safeMultiLangGet(stm, lang);

            if (stmName == null || stmName.isBlank()) {
                stmName = Data.trio(stm.id.id);
            }

            fullName += stmName + " - ";

            String stName = StaticStore.safeMultiLangGet(st, lang);

            if (stName == null || stName.isBlank()) {
                stName = Data.trio(st.id.id);
            }

            fullName += stName;

            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        name = fullName;
                    } else {
                        name = "`" + DataToString.getStageCode(st) + "` " + fullName;
                    }
                }
                case LIST_DESCRIPTION -> name = DataToString.getStageCode(st);
                case LIST_LABEL -> name = fullName;
            }

            data.add(name);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        String mid = getAuthorMessage().getId();

        if(StaticStore.timeLimit.containsKey(mid)) {
            StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(mid, memberLimit);
        }

        try {
            EntityHandler.generateStageEmbed(actualStage.get(index), event, getAuthorMessage(), "", treasure, configData, true, false, lang, msg ->
                StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), new StageInfoButtonHolder(actualStage.get(index), getAuthorMessage(), userID, channelID, msg, treasure, configData, false, lang))
            );
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FindStageMessageHolder::onSelected - Failed to upload stage embed");
        }
    }

    @Override
    public int getDataSize() {
        return actualStage.size();
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        if(event.getComponentId().equals("category")) {
            String name = ((StringSelectInteractionEvent) event).getValues().getFirst();

            selected = FindStage.MONTHLY.valueOf(name.toUpperCase(Locale.ENGLISH));

            filterStageData(selected);

            apply(event);
        } else {
            super.onEvent(event);
        }
    }

    public void filterStageData(FindStage.MONTHLY data) {
        actualStage.clear();

        if(data == FindStage.MONTHLY.ALL) {
            actualStage.addAll(stage);
        } else {
            for(int i = 0; i < stage.size(); i++) {
                StageMap map = stage.get(i).getCont();

                if(map == null)
                    continue;

                MapColc mc = map.getCont();

                if(mc == null)
                    continue;

                switch (data) {
                    case EOC -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 9)
                            continue;
                    }
                    case ITF1 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 3)
                            continue;
                    }
                    case ITF2 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 4)
                            continue;
                    }
                    case ITF3 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 5)
                            continue;
                    }
                    case COTC1 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 6)
                            continue;
                    }
                    case COTC2 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 7)
                            continue;
                    }
                    case COTC3 -> {
                        if (!mc.getSID().equals("000003") || map.id.id != 8)
                            continue;
                    }
                    case SOL -> {
                        if (!mc.getSID().equals("000000"))
                            continue;
                    }
                    case CYCLONE -> {
                        if (!mc.getSID().equals("000001") && !mc.getSID().equals("000014"))
                            continue;
                    }
                }

                actualStage.add(stage.get(i));
            }
        }
    }

    public List<ActionRow> getComponent() {
        int totalPage = getTotalPage(getDataSize(), chunk);

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > chunk) {
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

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        List<String> data = accumulateTextData(TextType.LIST_LABEL);

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(page * chunk + i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(page * chunk + i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(page * chunk + i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));

        if(monthly != null) {
            List<SelectOption> categories = new ArrayList<>();

            categories.add(SelectOption.of(LangID.getStringByID("data.all", lang), "all"));

            for(int i = 0; i < monthly.size(); i++) {
                String name = monthly.get(i).name().toLowerCase(Locale.ENGLISH);

                categories.add(SelectOption.of(LangID.getStringByID(monthly.get(i).id, lang), name).withDefault(monthly.get(i) == selected));
            }

            rows.add(ActionRow.of(StringSelectMenu.create("category").addOptions(categories).setPlaceholder(LangID.getStringByID("findStage.monthly.category", lang)).build()));
        }

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return rows;
    }
}

package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.bc.FindStage;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

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

    public FindStageMessageHolder(List<Stage> stage, List<FindStage.MONTHLY> monthly, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, TreasureHolder treasure, StageInfo.StageInfoConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.stage = stage;
        this.monthly = monthly;

        this.treasure = treasure;
        this.configData = configData;

        actualStage.addAll(stage);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if(i >= actualStage.size())
                break;

            Stage st = actualStage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
                if(mc != null) {
                    String mcn = MultiLangCont.get(mc, lang);

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            String stmn = MultiLangCont.get(stm, lang);

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            String stn = MultiLangCont.get(st, lang);

            if(st.id != null) {
                if(stn == null || stn.isBlank())
                    stn = Data.trio(st.id.id);
            } else {
                if(stn == null || stn.isBlank())
                    stn = "Unknown";
            }

            name += stn;

            data.add(name);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        String mid = getAuthorMessage().getId();

        if(StaticStore.timeLimit.containsKey(mid)) {
            StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(mid, memberLimit);
        }

        try {
            EntityHandler.showStageEmb(actualStage.get(id), event, getAuthorMessage(), "", treasure, configData, true, lang, msg ->
                StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), new StageInfoButtonHolder(actualStage.get(id), getAuthorMessage(), userID, channelID, msg, configData.isCompact, lang))
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

    @Override
    public List<ActionRow> getComponents() {
        int totalPage = getDataSize() / SearchHolder.PAGE_CHUNK;

        if(getDataSize() % SearchHolder.PAGE_CHUNK != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > PAGE_CHUNK) {
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

        List<String> data = accumulateListData(false);

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(page * PAGE_CHUNK + i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
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

package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.FindStage;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StageEnemyMessageHolder extends SearchHolder {
    private final List<List<Enemy>> enemySequences;
    private final List<Enemy> filterEnemy;
    private final StringBuilder enemyList;

    private List<Enemy> enemy;

    private final boolean orOperate;
    private final boolean hasBoss;
    private final boolean monthly;

    private final TreasureHolder treasure;
    private final StageInfo.StageInfoConfig configData;

    private final int background;
    private final int castle;
    private final int music;

    public StageEnemyMessageHolder(List<List<Enemy>> enemySequences, List<Enemy> filterEnemy, StringBuilder enemyList, @Nullable Message author, String keyword, ConfigHolder.SearchLayout layout, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, boolean orOperate, boolean hasBoss, boolean monthly, TreasureHolder treasure, StageInfo.StageInfoConfig configData, int background, int castle, int music, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, msg, keyword, layout, lang);

        this.enemySequences = enemySequences;
        this.filterEnemy = filterEnemy;
        this.enemyList = enemyList;

        this.orOperate = orOperate;
        this.hasBoss = hasBoss;
        this.monthly = monthly;

        this.treasure = treasure;
        this.configData = configData;

        this.background = background;
        this.castle = castle;
        this.music = music;

        enemy = enemySequences.removeFirst();
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(e.id.id);

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(e.id.id) + "`";

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(e.id.id);
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(e, lang);

                    if (text == null) {
                        text = Data.trio(e.id.id);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(e.id.id);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        MessageChannel ch = event.getChannel();
        Message author = getAuthorMessage();

        try {
            ArrayList<Stage> stages = EntityFilter.findStage(filterEnemy, music, background, castle, hasBoss, orOperate, monthly);

            if(stages.isEmpty()) {
                event.deferEdit()
                        .setContent(LangID.getStringByID("findStage.failed.noResult", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();
            } else if(stages.size() == 1) {
                EntityHandler.generateStageEmbed(stages.getFirst(), event, getAuthorMessage(), "", treasure, configData, true, false, lang, result -> {
                    if(StaticStore.timeLimit.containsKey(author.getAuthor().getId())) {
                        StaticStore.timeLimit.get(author.getAuthor().getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(author.getAuthor().getId(), memberLimit);
                    }

                    StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stages.getFirst(), author, userID, channelID, result, treasure, configData, false, lang));
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("findStage.several", lang)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > chunk) {
                    int totalPage = stages.size() / chunk;

                    if(stages.size() % chunk != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                createMonthlyMessage(ch, sb.toString(), accumulateStage(stages, false), stages, stages.size(), monthly).queue(res ->
                    StaticStore.putHolder(author.getAuthor().getId(), new FindStageMessageHolder(stages, monthly ? accumulateCategory(stages) : null, getAuthorMessage(), userID, ch.getId(), res, keyword, layout, treasure, configData, lang))
                );

                message.delete().queue();
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/StageEnemyMessageHolder::onSelected - Failed to upload stage selecting message");
        }
    }

    @Override
    public void finish(GenericComponentInteractionCreateEvent event, int index) {
        Enemy e = enemy.get(index);

        filterEnemy.add(e);

        String n = StaticStore.safeMultiLangGet(e, lang);

        if(n == null || n.isBlank())
            n = Data.trio(e.id.id);

        enemyList.append(n).append(", ");

        if(enemySequences.isEmpty()) {
            super.finish(event, index);
        } else {
            enemy = enemySequences.removeFirst();
            page = 0;

            apply(event);
        }
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }

    private List<String> accumulateStage(List<Stage> stages, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < chunk; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
                if (mc != null)
                    name = mc.getSID() + "/";
                else
                    name = "Unknown/";

                if (stm.id != null)
                    name += Data.trio(stm.id.id) + "/";
                else
                    name += "Unknown/";

                if (st.id != null)
                    name += Data.trio(st.id.id) + " | ";
                else
                    name += "Unknown | ";

                if (mc != null) {
                    String mcn = MultiLangCont.get(mc, lang);

                    if (mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn + " - ";
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

    private RestAction<Message> createMonthlyMessage(MessageChannel ch, String content, List<String> data, List<Stage> stages, int size, boolean monthly) {
        int totalPage = size / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

        if(size % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:\\S+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("ui.search.selectList", lang)).build()));

        if(monthly) {
            List<SelectOption> categories = new ArrayList<>();

            List<FindStage.MONTHLY> category = accumulateCategory(stages);

            categories.add(SelectOption.of(LangID.getStringByID("data.all", lang), "all"));

            for(int i = 0; i < category.size(); i++) {
                String name = category.get(i).name().toLowerCase(Locale.ENGLISH);

                categories.add(SelectOption.of(LangID.getStringByID(category.get(i).id, lang), name));
            }

            rows.add(ActionRow.of(StringSelectMenu.create("category").addOptions(categories).setPlaceholder(LangID.getStringByID("findStage.monthly.category", lang)).build()));
        }

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return ch.sendMessage(content).setAllowedMentions(new ArrayList<>()).setComponents(rows);
    }

    private List<FindStage.MONTHLY> accumulateCategory(List<Stage> stages) {
        List<FindStage.MONTHLY> category = new ArrayList<>();

        for(int i = 0; i < stages.size(); i++) {
            StageMap map = stages.get(i).getCont();

            if(map == null || map.id == null)
                continue;

            MapColc mc = map.getCont();

            if(mc == null)
                continue;

            switch (mc.getSID()) {
                case "000003" -> {
                    switch (map.id.id) {
                        case 3 -> addIfNone(category, FindStage.MONTHLY.ITF1);
                        case 4 -> addIfNone(category, FindStage.MONTHLY.ITF2);
                        case 5 -> addIfNone(category, FindStage.MONTHLY.ITF3);
                        case 6 -> addIfNone(category, FindStage.MONTHLY.COTC1);
                        case 7 -> addIfNone(category, FindStage.MONTHLY.COTC2);
                        case 8 -> addIfNone(category, FindStage.MONTHLY.COTC3);
                        case 9 -> addIfNone(category, FindStage.MONTHLY.EOC);
                    }
                }
                case "000001" -> addIfNone(category, FindStage.MONTHLY.CYCLONE);
                case "000000" -> addIfNone(category, FindStage.MONTHLY.SOL);
            }
        }

        return category;
    }

    private <T> void addIfNone(List<T> data, T element) {
        if(!data.contains(element)) {
            data.add(element);
        }
    }
}

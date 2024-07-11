package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.FindStage;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.*;

public class StageEnemyMessageHolder extends SearchHolder {
    private final List<List<Enemy>> enemySequences;
    private final List<Enemy> filterEnemy;
    private final StringBuilder enemyList;

    private List<Enemy> enemy;

    private final boolean isFrame;
    private final boolean isExtra;
    private final boolean isCompact;
    private final boolean orOperate;
    private final boolean hasBoss;
    private final boolean monthly;

    private final int star;
    private final TreasureHolder treasure;

    private final int background;
    private final int castle;
    private final int music;

    public StageEnemyMessageHolder(List<List<Enemy>> enemySequences, List<Enemy> filterEnemy, StringBuilder enemyList, Message author, Message msg, String channelID, boolean isFrame, boolean isExtra, boolean isCompact, boolean orOperate, boolean hasBoss, boolean monthly, int star, TreasureHolder treasure, int background, int castle, int music, CommonStatic.Lang.Locale lang) {
        super(author, msg, channelID, lang);

        this.enemySequences = enemySequences;
        this.filterEnemy = filterEnemy;
        this.enemyList = enemyList;

        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.isCompact = isCompact;
        this.orOperate = orOperate;
        this.hasBoss = hasBoss;
        this.monthly = monthly;

        this.star = star;
        this.treasure = treasure;

        this.background = background;
        this.castle = castle;
        this.music = music;

        enemy = enemySequences.removeFirst();

        registerAutoFinish(this, msg, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

            String ename = Data.trio(e.id.id) + " - ";

            if(MultiLangCont.get(e, lang) != null)
                ename += MultiLangCont.get(e, lang);

            data.add(ename);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Message author = getAuthorMessage();

        try {
            ArrayList<Stage> stages = EntityFilter.findStage(filterEnemy, music, background, castle, hasBoss, orOperate, monthly);

            if(stages.isEmpty()) {
                message.delete().queue();

                ch.sendMessage(LangID.getStringByID("fstage_nost", lang)).queue();
            } else if(stages.size() == 1) {
                message.delete().queue();

                EntityHandler.showStageEmb(stages.getFirst(), ch, getAuthorMessage(), isFrame, isExtra, isCompact, star, treasure, lang, result -> {
                    if(StaticStore.timeLimit.containsKey(author.getAuthor().getId())) {
                        StaticStore.timeLimit.get(author.getAuthor().getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(author.getAuthor().getId(), memberLimit);
                    }

                    StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stages.getFirst(), author, result, channelID, isCompact, lang));
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("fstage_several", lang)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > PAGE_CHUNK) {
                    int totalPage = stages.size() / PAGE_CHUNK;

                    if(stages.size() % PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                createMonthlyMessage(ch, sb.toString(), accumulateStage(stages, false), stages, stages.size(), monthly).queue(res ->
                    StaticStore.putHolder(author.getAuthor().getId(), new FindStageMessageHolder(stages, monthly ? accumulateCategory(stages) : null, getAuthorMessage(), res, ch.getId(), star, treasure, isFrame, isExtra, isCompact, lang))
                );

                message.delete().queue();
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/StageEnemyMessageHolder::onSelected - Failed to upload stage selecting message");
        }
    }

    @Override
    public void finish(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        Enemy e = enemy.get(id);

        filterEnemy.add(e);

        String n = StaticStore.safeMultiLangGet(e, lang);

        if(n == null || n.isBlank())
            n = Data.trio(e.id.id);

        enemyList.append(n).append(", ");

        if(enemySequences.isEmpty()) {
            super.finish(event);
        } else {
            enemy = enemySequences.removeFirst();
            page = 0;

            apply(event);
        }
    }

    @Override
    protected String getPage() {
        StringBuilder sb = new StringBuilder();

        if(!enemyList.isEmpty()) {
            sb.append(LangID.getStringByID("fstage_selected", lang).replace("_", enemyList.toString().replaceAll(", $", "")));
        }

        sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

        List<String> data = accumulateListData(false);

        for(int i = 0; i < data.size(); i++) {
            sb.append(page * PAGE_CHUNK + i + 1).append(". ").append(data.get(i)).append("\n");
        }

        if(enemy.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = enemy.size() / SearchHolder.PAGE_CHUNK;

            if(enemy.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            sb.append(LangID.getStringByID("formst_page", lang).formatted(page + 1, totalPage)).append("\n");
        }

        sb.append("```");

        return sb.toString();
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }

    private List<String> accumulateStage(List<Stage> stages, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < PAGE_CHUNK; i++) {
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
        int totPage = size / SearchHolder.PAGE_CHUNK;

        if(size % SearchHolder.PAGE_CHUNK != 0)
            totPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(size > SearchHolder.PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT));

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT));
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            String element = data.get(i);

            String[] elements = element.split("\\\\\\\\");

            if(elements.length == 2) {
                if(elements[0].matches("<:[^\\s]+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("search_list", lang)).build()));

        if(monthly) {
            List<SelectOption> categories = new ArrayList<>();

            List<FindStage.MONTHLY> category = accumulateCategory(stages);

            categories.add(SelectOption.of(LangID.getStringByID("data_all", lang), "all"));

            for(int i = 0; i < category.size(); i++) {
                String name = category.get(i).name().toLowerCase(Locale.ENGLISH);

                categories.add(SelectOption.of(LangID.getStringByID("data_" + name, lang), name));
            }

            rows.add(ActionRow.of(StringSelectMenu.create("category").addOptions(categories).setPlaceholder(LangID.getStringByID("fstage_category", lang)).build()));
        }

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

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

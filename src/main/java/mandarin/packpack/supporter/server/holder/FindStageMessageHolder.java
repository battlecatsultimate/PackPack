package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.bc.FindStage;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.*;

public class FindStageMessageHolder extends SearchHolder {
    private final List<Stage> stage;
    private final List<Stage> actualStage = new ArrayList<>();
    private final List<FindStage.MONTHLY> monthly;

    private final boolean isFrame;
    private final boolean isExtra;
    private final boolean isCompact;
    private final int star;

    private FindStage.MONTHLY selected = FindStage.MONTHLY.ALL;

    public FindStageMessageHolder(List<Stage> stage, List<FindStage.MONTHLY> monthly, Message author, Message msg, String channelID, int star, boolean isFrame, boolean isExtra, boolean isCompact, int lang) {
        super(msg, author, channelID, lang);

        this.stage = stage;
        this.monthly = monthly;

        this.star = star;
        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.isCompact = isCompact;

        actualStage.addAll(stage);

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
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
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String mcn = MultiLangCont.get(mc);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String stmn = MultiLangCont.get(stm);

            CommonStatic.getConfig().lang = oldConfig;

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            CommonStatic.getConfig().lang = lang;

            String stn = MultiLangCont.get(st);

            CommonStatic.getConfig().lang = oldConfig;

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
        MessageChannel ch = event.getChannel();
        Guild g = event.getGuild();

        if(g == null)
            return;

        int id = parseDataToInt(event);

        msg.delete().queue();

        String mid = getAuthorMessage().getId();

        if(StaticStore.timeLimit.containsKey(mid)) {
            StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(mid, memberLimit);
        }

        try {
            Message msg = EntityHandler.showStageEmb(actualStage.get(id), ch, getAuthorMessage(), isFrame, isExtra, isCompact, star, lang);

            if(msg != null && StaticStore.idHolder.containsKey(g.getId())) {
                StaticStore.putHolder(getAuthorMessage().getAuthor().getId(), new StageInfoButtonHolder(actualStage.get(id), getAuthorMessage(), msg, channelID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return actualStage.size();
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        if(event.getComponentId().equals("category")) {
            String name = ((StringSelectInteractionEvent) event).getValues().get(0);

            selected = FindStage.MONTHLY.valueOf(name.toUpperCase(Locale.ENGLISH));

            filterStageData(selected);

            apply(event);
        } else {
            super.performInteraction(event);
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
                    case EOC:
                        if(!mc.getSID().equals("000003") || map.id.id != 9)
                            continue;

                        break;
                    case ITF1:
                        if(!mc.getSID().equals("000003") || map.id.id != 3)
                            continue;

                        break;
                    case ITF2:
                        if(!mc.getSID().equals("000003") || map.id.id != 4)
                            continue;

                        break;
                    case ITF3:
                        if(!mc.getSID().equals("000003") || map.id.id != 5)
                            continue;

                        break;
                    case COTC1:
                        if(!mc.getSID().equals("000003") || map.id.id != 6)
                            continue;

                        break;
                    case COTC2:
                        if(!mc.getSID().equals("000003") || map.id.id != 7)
                            continue;

                        break;
                    case COTC3:
                        if(!mc.getSID().equals("000003") || map.id.id != 8)
                            continue;

                        break;
                    case SOL:
                        if(!mc.getSID().equals("000000"))
                            continue;

                        break;
                    case CYCLONE:
                        if(!mc.getSID().equals("000001") && !mc.getSID().equals("000014"))
                            continue;

                        break;
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
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)));
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)));
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)));
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
                if(elements[0].matches("<:[^\\s]+?:\\d+>")) {
                    options.add(SelectOption.of(elements[1], String.valueOf(page * PAGE_CHUNK + i)).withEmoji(Emoji.fromFormatted(elements[0])));
                } else {
                    options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
                }
            } else {
                options.add(SelectOption.of(element, String.valueOf(page * PAGE_CHUNK + i)));
            }
        }

        rows.add(ActionRow.of(StringSelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("search_list", lang)).build()));

        if(monthly != null) {
            List<SelectOption> categories = new ArrayList<>();

            categories.add(SelectOption.of(LangID.getStringByID("data_all", lang), "all"));

            for(int i = 0; i < monthly.size(); i++) {
                String name = monthly.get(i).name().toLowerCase(Locale.ENGLISH);

                if(monthly.get(i) == selected) {
                    categories.add(SelectOption.of(LangID.getStringByID("data_" + name, lang), name).withDefault(true));
                } else {
                    categories.add(SelectOption.of(LangID.getStringByID("data_" + name, lang), name));
                }
            }

            rows.add(ActionRow.of(StringSelectMenu.create("category").addOptions(categories).setPlaceholder(LangID.getStringByID("fstage_category", lang)).build()));
        }

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

        return rows;
    }
}

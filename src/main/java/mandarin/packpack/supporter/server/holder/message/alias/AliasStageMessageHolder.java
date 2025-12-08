package mandarin.packpack.supporter.server.holder.message.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AliasStageMessageHolder extends SearchHolder {
    private final ArrayList<Stage> stages;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    private final String summary;

    public AliasStageMessageHolder(ArrayList<Stage> stages, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, AliasHolder.MODE mode, CommonStatic.Lang.Locale lang, @Nonnull String keyword, @Nullable String aliasName, @Nonnull String summary) {
        super(author, userID, channelID, msg, keyword, ConfigHolder.SearchLayout.FANCY_LIST, lang);

        this.stages = stages;
        this.mode = mode;
        this.aliasName = aliasName;

        this.summary = summary;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onExpire() {
        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("ui.search.expired", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1); i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
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
    public String getSearchSummary() {
        return summary;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        String stName = StaticStore.safeMultiLangGet(stages.get(index), lang);

        if(stName == null || stName.isBlank())
            stName = stages.get(index).name;

        if(stName == null || stName.isBlank())
            stName = stages.get(index).getCont().getSID() + "-" + Data.trio(stages.get(index).getCont().id.id) + "-" + Data.trio(stages.get(index).id.id);

        ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.STAGE, lang, stages.get(index));

        switch (mode) {
            case GET -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(stName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                } else {
                    StringBuilder result = new StringBuilder(LangID.getStringByID("alias.aliases.stage", lang).formatted(stName, alias.size()));

                    result.append("\n\n");

                    for (int i = 0; i < alias.size(); i++) {
                        String temp = "- " + alias.get(i);

                        if (result.length() + temp.length() > 1900) {
                            result.append("\n")
                                    .append(LangID.getStringByID("alias.etc", lang));
                            break;
                        }

                        result.append(temp);

                        if (i < alias.size() - 1) {
                            result.append("\n");
                        }
                    }

                    event.deferEdit()
                            .setComponents(TextDisplay.of(result.toString()))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                }
            }
            case ADD -> {
                if (alias == null)
                    alias = new ArrayList<>();

                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.contain", lang).formatted(stName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.add(aliasName);
                AliasHolder.SALIAS.put(lang, stages.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.added", lang).formatted(stName, alias)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias added\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + event.getUser().getAsMention());
            }
            case REMOVE -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(stName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }
                if (!alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.removeFail", lang).formatted(stName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.remove(aliasName);
                AliasHolder.SALIAS.put(lang, stages.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.removed", lang).formatted(stName, aliasName)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias removed\n\nStage : " + stName + "\nAlias : " + aliasName + "\nBy : " + event.getUser().getAsMention());
            }
        }
    }

    @Override
    public int getDataSize() {
        return stages.size();
    }
}

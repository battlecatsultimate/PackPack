package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.NumericalResult;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SolutionHolder extends SearchHolder {
    private final String summary;
    private final List<BigDecimal[]> targetRanges;
    private final List<NumericalResult> solutions;

    public SolutionHolder(@NotNull Message msg, @NotNull Message author, @NotNull String channelID, String summary, List<BigDecimal[]> targetRanges, List<NumericalResult> solutions, CommonStatic.Lang.Locale lang) {
        super(author, msg, channelID, lang);

        this.summary = summary;
        this.targetRanges = targetRanges;
        this.solutions = solutions;

        registerAutoFinish(this, msg, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> result = new ArrayList<>();

        for(int i = 5 * page; i < 5 * (page + 1); i++) {
            if(i >= solutions.size())
                break;

            NumericalResult solution = solutions.get(i);

            result.add(String.format(
                    LangID.getStringByID("solve.root", lang),
                    i + 1,
                    Equation.formatNumber(solution.value),
                    Equation.formatNumber(targetRanges.get(i)[0]),
                    Equation.formatNumber(targetRanges.get(i)[1]),
                    Equation.formatNumber(solution.error)
            ));
        }

        return result;
    }

    @Override
    protected String getPage() {
        StringBuilder sb = new StringBuilder(summary).append("```ansi\n");

        List<String> data = accumulateListData(true);

        for(int i = 0; i < data.size(); i++) {
            sb.append(data.get(i));

            if(i < data.size() - 1)
                sb.append("\n\n");
            else
                sb.append("\n");
        }

        if(getDataSize() > 5) {
            int totalPage = getDataSize() / 5;

            if(getDataSize() % 5 != 0)
                totalPage++;

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage)).append("\n");
        }

        sb.append("```");

        return sb.toString();
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
    }

    @Override
    public int getDataSize() {
        return solutions.size();
    }

    @Override
    public List<ActionRow> getComponents() {
        int totalPage = getDataSize() / 5;

        if(getDataSize() % 5 != 0)
            totalPage++;

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > 5) {
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

        return rows;
    }

    @Override
    public void onExpire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        message.editMessageComponents(new ArrayList<>())
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }
}

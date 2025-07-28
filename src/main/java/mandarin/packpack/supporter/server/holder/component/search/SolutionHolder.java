package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.calculation.NumericalResult;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SolutionHolder extends SearchHolder {
    private final String summary;
    private final List<BigDecimal[]> targetRanges;
    private final List<NumericalResult> solutions;

    public SolutionHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, ConfigHolder.SearchLayout layout, String summary, List<BigDecimal[]> targetRanges, List<NumericalResult> solutions, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, msg, "", layout, lang);

        this.summary = summary;
        this.targetRanges = targetRanges;
        this.solutions = solutions;

        chunk = 5;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> result = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1); i++) {
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
    public void onSelected(GenericComponentInteractionCreateEvent event) {
    }

    @Override
    public int getDataSize() {
        return solutions.size();
    }

    @Override
    public void onExpire() {
        message.editMessageComponents(new ArrayList<>())
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    @Override
    public Container getComponents() {
        int totalPage = getTotalPage(getDataSize(), chunk);

        List<ContainerChildComponent> children = new ArrayList<>();
        List<String> data = accumulateTextData(TextType.TEXT);

        children.add(TextDisplay.of(summary));
        children.add(Separator.create(true, Separator.Spacing.LARGE));

        switch (layout) {
            case FANCY_BUTTON, FANCY_LIST -> {
                for (int i = 0; i < data.size(); i++) {
                    children.add(TextDisplay.of("```ansi\n" + data.get(i) + "\n```"));

                    if (i < data.size() - 1) {
                        children.add(Separator.create(false, Separator.Spacing.SMALL));
                    }
                }
            }
            case COMPACTED -> {
                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < data.size(); i++) {
                    builder.append(i + 1).append(". ").append(data.get(i));

                    if (i < data.size() - 1) {
                        builder.append("\n");
                    }
                }

                children.add(TextDisplay.of("```ansi\n" + builder + "\n```"));
            }
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(TextDisplay.of(LangID.getStringByID("ui.search.page", lang).formatted(page + 1, totalPage)));

        if(getDataSize() > chunk) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            children.add(ActionRow.of(buttons));
        }

        children.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang))));

        return Container.of(children);
    }
}

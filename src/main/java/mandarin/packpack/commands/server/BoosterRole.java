package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.booster.BoosterRoleListHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nonnull;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoosterRole extends ConstraintCommand {
    public static final String SEGMENT_NAME = "ROLE_NAME";
    public static final String SEGMENT_COLOR1 = "ROLE_COLOR_PRIMARY";
    public static final String SEGMENT_COLOR2 = "ROLE_COLOR_GRADIENT";

    public BoosterRole(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if(holder == null)
            return;

        File temp = new File("./temp");

        if(!temp.exists() && temp.mkdirs()) {
            System.out.println("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        if(holder.booster == -1L) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), LangID.getStringByID("boosterRole.failed.noRole", lang));

            return;
        }

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), msg -> StaticStore.putHolder(loader.getUser().getIdLong(), new BoosterRoleListHolder(loader.getMessage(), loader.getUser().getIdLong(), loader.getChannel().getIdLong(), loader.getGuild(), msg, holder, lang)), getComponents(loader.getGuild()));
    }

    private net.dv8tion.jda.api.components.container.Container getComponents(Guild g) {
        BoosterHolder boosterHolder = StaticStore.boosterData.computeIfAbsent(g.getIdLong(), _ -> new BoosterHolder());

        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of(
                "## " + LangID.getStringByID("boosterRole.title", lang) + "\n" +
                        LangID.getStringByID("boosterRole.explanation", lang) + "\n\n" +
                        LangID.getStringByID("boosterRole.select", lang)
        ));

        List<Map.Entry<Long, BoosterData>> boosterData = boosterHolder.serverBooster.entrySet().stream().filter(e -> e.getValue().getRole() != -1L).toList();

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        int size = Math.min(boosterData.size(), ConfigHolder.SearchLayout.FANCY_LIST.chunkSize);

        if (boosterData.isEmpty()) {
            children.add(TextDisplay.of("### " + LangID.getStringByID("boosterRole.noMember", lang)));
        } else {
            for (int i = 0; i < size; i++) {
                Map.Entry<Long, BoosterData> entry = boosterData.get(i);
                Role role = g.getRoleById(entry.getValue().getRole());

                if (role == null)
                    continue;

                children.add(TextDisplay.of(
                        LangID.getStringByID("boosterRole.list.member.text", lang).formatted(i, entry.getKey(), entry.getKey()) + "\n" +
                                "  " + LangID.getStringByID("boosterRole.list.role", lang).formatted(role.getName(), role.getIdLong(), role.getIdLong())
                ));

                if (i < size - 1) {
                    children.add(Separator.create(false, Separator.Spacing.SMALL));
                }
            }
        }

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        List<SelectOption> options = new ArrayList<>();

        if (size == 0) {
            options.add(SelectOption.of("A", "A"));
        } else {
            for (int i = 0; i < size; i++) {
                Map.Entry<Long, BoosterData> entry = boosterData.get(i);
                Role role = g.getRoleById(entry.getValue().getRole());

                if (role == null)
                    continue;

                User u = g.getJDA().getUserById(entry.getKey());

                if (u == null)
                    continue;

                options.add(SelectOption.of(LangID.getStringByID("boosterRole.list.member.selectMenu", lang).formatted(i + 1, u.getGlobalName(), entry.getKey()), String.valueOf(i)));
            }
        }

        children.add(ActionRow.of(StringSelectMenu.create("role").addOptions(options).setPlaceholder(LangID.getStringByID("boosterRole.list.edit", lang)).setRequiredRange(1, 1).setDisabled(boosterData.isEmpty()).build()));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        if (boosterData.size() > ConfigHolder.SearchLayout.FANCY_LIST.chunkSize) {
            int totalPage = SearchHolder.getTotalPage(boosterData.size(), ConfigHolder.SearchLayout.FANCY_LIST.chunkSize);

            children.add(TextDisplay.of(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)));

            List<net.dv8tion.jda.api.components.buttons.Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                buttons.add(net.dv8tion.jda.api.components.buttons.Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", lang), EmojiStore.TWO_PREVIOUS).asDisabled());
            }

            buttons.add(net.dv8tion.jda.api.components.buttons.Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", lang), EmojiStore.PREVIOUS).asDisabled());
            buttons.add(net.dv8tion.jda.api.components.buttons.Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", lang), EmojiStore.NEXT));

            if(totalPage > 10) {
                buttons.add(net.dv8tion.jda.api.components.buttons.Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", lang), EmojiStore.TWO_NEXT));
            }

            children.add(ActionRow.of(buttons));

            children.add(Separator.create(false, Separator.Spacing.SMALL));
        }

        EntitySelectMenu memberMenu = EntitySelectMenu.create("create", EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, 1)
                .setPlaceholder(LangID.getStringByID("boosterRole.list.add", lang))
                .build();

        children.add(ActionRow.of(memberMenu));

        children.add(Separator.create(false, Separator.Spacing.SMALL));

        children.add(ActionRow.of(Button.danger("close", LangID.getStringByID("ui.button.close", lang)).withEmoji(EmojiStore.CROSS)));

        return Container.of(children);
    }
}

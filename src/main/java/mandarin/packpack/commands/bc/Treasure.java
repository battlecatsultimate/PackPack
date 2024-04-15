package mandarin.packpack.commands.bc;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.TreasureButtonHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Treasure extends ConstraintCommand {
    public Treasure(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        TreasureHolder treasure = StaticStore.treasure.getOrDefault(u.getId(), new TreasureHolder());

        replyToMessageSafely(ch, generateText(treasure), loader.getMessage(), this::attachUIComponents, result ->
                StaticStore.putHolder(u.getId(), new TreasureButtonHolder(loader.getMessage(), result, ch.getId(), treasure, lang))
        );
    }

    private String generateText(TreasureHolder treasure) {
        StringBuilder generator = new StringBuilder();

        generator.append("**")
                .append(LangID.getStringByID("data_tbasic", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.basicText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.basicText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_level", lang), treasure.basic[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_teoc", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.eocText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.eocText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.eoc[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_titf", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.itfText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.itfText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.itf[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_tcotc", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.cotcText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.cotcText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.cotc[i]))
                    .append("\n");
        }

        return generator.toString();
    }

    private MessageCreateAction attachUIComponents(MessageCreateAction a) {
        return a.setComponents(
                ActionRow.of(Button.secondary("basic", LangID.getStringByID("treasure_basic", lang)).withEmoji(EmojiStore.ORB)),
                ActionRow.of(Button.secondary("eoc", LangID.getStringByID("treasure_eoc", lang)).withEmoji(EmojiStore.DOGE)),
                ActionRow.of(Button.secondary("itf", LangID.getStringByID("treasure_itf", lang)).withEmoji(EmojiStore.SHIBALIEN)),
                ActionRow.of(Button.secondary("cotc", LangID.getStringByID("treasure_cotc", lang)).withEmoji(EmojiStore.SHIBALIENELITE)),
                ActionRow.of(Button.success("confirm", LangID.getStringByID("button_confirm", lang)), Button.danger("cancel", LangID.getStringByID("button_cancel", lang)))
        );
    }
}

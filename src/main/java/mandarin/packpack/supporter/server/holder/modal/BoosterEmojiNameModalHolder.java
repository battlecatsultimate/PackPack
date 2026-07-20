package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BoosterEmojiNameModalHolder extends ModalHolder {
    private final RichCustomEmoji targetEmoji;

    public BoosterEmojiNameModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetEmoji = null;
    }

    public BoosterEmojiNameModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull RichCustomEmoji targetEmoji, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetEmoji = targetEmoji;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) throws Exception {
        if (!event.getModalId().equals("name"))
            return;

        String name = getValueFromMap(event.getValues(), "name").replace(" ", "_");

        if (!name.matches("[A-z0-9_]+")) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.invalidName", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (targetEmoji != null && name.equals(targetEmoji.getName())) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterEmoji.failed.sameName", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        goBack(event, name);
    }

    @Override
    public void clean() {

    }
}

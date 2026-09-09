package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.commands.server.BoosterRole;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BoosterRoleNameModalHolder extends ModalHolder {
    private final Role targetRole;

    public BoosterRoleNameModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetRole = null;
    }

    public BoosterRoleNameModalHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, @NotNull Role targetRole, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.targetRole = targetRole;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) throws Exception {
        if (!event.getModalId().equals("name"))
            return;

        String name = getValueFromMap(event.getValues(), "name").trim();

        if (name.isBlank()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.invalidName", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (targetRole != null && name.equals(targetRole.getName())) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.sameName", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        goBack(event, BoosterRole.SEGMENT_NAME + "\n" + name);
    }

    @Override
    public void clean() {

    }
}

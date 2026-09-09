package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.commands.server.BoosterRole;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;

public class BoosterRoleColorHolder extends ModalHolder {
    private final boolean primaryColor;

    public BoosterRoleColorHolder(@Nullable Message author, long userID, long channelID, @NotNull Message message, boolean primaryColor, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.primaryColor = primaryColor;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) throws Exception {
        if (!event.getModalId().equals("color"))
            return;

        String colorCode = getValueFromMap(event.getValues(), "hexCode").replace("#", "").toUpperCase(Locale.ENGLISH);

        String red = getValueFromMap(event.getValues(), "red");
        String green = getValueFromMap(event.getValues(), "green");
        String blue = getValueFromMap(event.getValues(), "blue");

        if (colorCode.isBlank() && red.isBlank() && green.isBlank() && blue.isBlank()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.noColor", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        if (colorCode.matches("[0-9A-F]+") && colorCode.length() == 6) {
            if (primaryColor) {
                goBack(event, BoosterRole.SEGMENT_COLOR1 + "\n" + Integer.parseInt(colorCode, 16));
            } else {
                goBack(event, BoosterRole.SEGMENT_COLOR2 + "\n" + Integer.parseInt(colorCode, 16));
            }

            return;
        } else if (red.isBlank() && green.isBlank() && blue.isBlank()) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.wrongHexCode", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        if (!StaticStore.isNumeric(red) || !StaticStore.isNumeric(green) || !StaticStore.isNumeric(blue)) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.rgbNotNumber", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        int r = StaticStore.safeParseInt(red);
        int g = StaticStore.safeParseInt(green);
        int b = StaticStore.safeParseInt(blue);

        if (notRGB(r) || notRGB(g) || notRGB(b)) {
            event.deferReply()
                    .setComponents(TextDisplay.of(LangID.getStringByID("boosterRole.failed.rgbInvalidRange", lang)))
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();

            goBack();

            return;
        }

        Color c = new Color(r, g, b);

        if (primaryColor) {
            goBack(event, BoosterRole.SEGMENT_COLOR1 + "\n" + (c.getRGB() & 0xFFFFFF));
        } else {
            goBack(event, BoosterRole.SEGMENT_COLOR2 + "\n" + (c.getRGB() & 0xFFFFFF));
        }
    }

    @Override
    public void clean() {

    }

    private boolean notRGB(int value) {
        return value < 0 || value > 255;
    }
}

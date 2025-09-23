package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class FormTalentHolder extends ComponentHolder {
    private final Form f;
    private final FormStat.FormStatConfig configData;

    public FormTalentHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, Form f, FormStat.FormStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.f = f;
        this.configData = configData;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (event.getComponentId().equals("back")) {
            goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessageComponents(expireButton(message, configData.compact))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        EntityHandler.generateTalentEmbed(event, hasAuthorMessage() ? getAuthorMessage() : null, f, configData.isFrame, true, true, lang);
    }
}

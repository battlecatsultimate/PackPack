package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class FormDPSButtonHolder extends ComponentHolder {
    private final Form f;
    private final TreasureHolder t;
    private final ConfigHolder config;
    private final FormStat.FormStatConfig configData;

    public FormDPSButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, Form f, TreasureHolder t, ConfigHolder config, FormStat.FormStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.f = f;
        this.t = t;
        this.config = config;
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
        EntityHandler.showFormDPS(event, hasAuthorMessage() ? getAuthorMessage() : null, f, t, configData.lv, config, configData.talent, configData.treasure, true, lang);
    }
}

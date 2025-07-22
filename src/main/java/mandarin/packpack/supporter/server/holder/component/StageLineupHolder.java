package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.stage.Stage;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponentUnion;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StageLineupHolder extends ComponentHolder {
    private final Stage st;
    private final StageInfo.StageInfoConfig configData;

    public StageLineupHolder(Stage st, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, StageInfo.StageInfoConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.st = st;
        this.configData = configData;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        if(configData.isCompact) {
            message.editMessageComponents()
                    .mentionRepliedUser(false)
                    .queue();
        } else {
            ArrayList<MessageTopLevelComponent> components = new ArrayList<>();

            for (MessageTopLevelComponent layout : message.getComponents()) {
                if (!(layout instanceof ActionRow row))
                    continue;

                List<ActionRowChildComponentUnion> itemComponents = new ArrayList<>();

                for (ActionRowChildComponentUnion i : row.getComponents()) {
                    if (i instanceof Button button) {
                        itemComponents.add((ActionRowChildComponentUnion) button.asDisabled());
                    } else if (i instanceof SelectMenu selectMenu) {
                        itemComponents.add((ActionRowChildComponentUnion) selectMenu.asDisabled());
                    }
                }

                components.add(ActionRow.of(itemComponents));
            }

            message.editMessageComponents(components)
                    .mentionRepliedUser(false)
                    .queue();
        }
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        EntityHandler.showFixedLineupData(st, st.preset, event, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (event.getComponentId().equals("back")) {
            goBack(event);
        }
    }
}

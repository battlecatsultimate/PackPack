package mandarin.packpack.supporter.server.holder.modal;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomRoleAssignHolder extends ModalHolder {
    private final Runnable editor;
    private final IDHolder holder;
    private final Guild g;

    public CustomRoleAssignHolder(@NotNull Message author, @NotNull String channelID, @NotNull String messageID, @NotNull Runnable editor, @NotNull IDHolder holder, @NotNull Guild g) {
        super(author, channelID, messageID);

        this.editor = editor;
        this.holder = holder;
        this.g = g;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }

    @Override
    public void onEvent(ModalInteractionEvent event) {
        int lang = holder.config.lang;
        List<ModalMapping> values = event.getValues();

        String name = getValueFromMap(values, "name");

        if (holder.ID.containsKey(name)) {
            event.reply(LangID.getStringByID("idset_conflict", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String role = getValueFromMap(values, "role");

        if (!StaticStore.isNumeric(role)) {
            event.reply(LangID.getStringByID("idset_rolenum", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        Role r = getRoleSafelyWithID(role);

        if (r == null) {
            event.reply(LangID.getStringByID("idset_roleno", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        holder.ID.put(name, r.getId());

        event.reply(LangID.getStringByID("idset_customadded", lang))
                .setEphemeral(true)
                .queue();

        editor.run();
    }

    private Role getRoleSafelyWithID(String id) {
        if (id == null)
            return null;

        try {
            return g.getRoleById(id);
        } catch (Exception ignored) {
            return null;
        }
    }
}
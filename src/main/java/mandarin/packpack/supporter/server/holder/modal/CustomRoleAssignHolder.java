package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CustomRoleAssignHolder extends ModalHolder {
    private final Runnable editor;
    private final IDHolder holder;
    private final Guild g;

    public CustomRoleAssignHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull Runnable editor, @NotNull IDHolder holder, @NotNull Guild g, @NotNull CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, lang);

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
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        CommonStatic.Lang.Locale lang = holder.config.lang;

        List<ModalMapping> values = event.getValues();

        String name = getValueFromMap(values, "name");

        if (holder.ID.containsKey(name)) {
            event.reply(LangID.getStringByID("idset_conflict", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (name.length() > 10 && StaticStore.isNumeric(name)) {
            event.reply(LangID.getStringByID("idset_numformat", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (name.length() > 32) {
            event.reply(LangID.getStringByID("idset_toolong", lang))
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

        if (holder.MOD.equals(r.getId())) {
            event.reply(LangID.getStringByID("idset_modalready", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (holder.member != null && holder.member.equals(r.getId())) {
            event.reply(LangID.getStringByID("idset_memalready", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (holder.booster != null && holder.booster.equals(r.getId())) {
            event.reply(LangID.getStringByID("idset_booalready", lang))
                    .setEphemeral(true)
                    .setAllowedMentions(new ArrayList<>())
                    .queue();

            return;
        }

        for (String key : holder.ID.keySet()) {
            String id = holder.ID.get(key);

            if (id != null && id.equals(r.getId())) {
                event.reply(String.format(LangID.getStringByID("idset_idalready", lang), r.getId(), r.getId(), key))
                        .setEphemeral(true)
                        .queue();

                return;
            }
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

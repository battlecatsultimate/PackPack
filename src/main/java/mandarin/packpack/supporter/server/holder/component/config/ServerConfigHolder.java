package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Conflictable;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public abstract class ServerConfigHolder extends ComponentHolder implements Conflictable {
    protected final Message message;

    protected final IDHolder holder;
    protected final IDHolder backup;

    protected final int lang;

    public ServerConfigHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, int lang) {
        super(author, channelID, message);

        this.message = message;

        this.holder = holder;
        backup = this.holder.clone();

        this.lang = lang;
    }

    public ServerConfigHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message);

        this.message = message;

        this.holder = holder;
        this.backup = backup;

        this.lang = lang;
    }

    @Override
    public final void onExpire(String id) {
        message.editMessage(LangID.getStringByID("sercon_expire", lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();

        holder.inject(backup);
    }

    @Override
    public boolean isConflicted(Holder holder) {
        if (holder instanceof ConfirmPopUpHolder ch) {
            Holder parent = ch.parent;

            if (parent == null)
                return false;

            return parent instanceof ServerConfigHolder sh && sh.holder == this.holder;
        } else if (holder instanceof ServerConfigHolder sh) {
            if (getAuthorMessage() == holder.getAuthorMessage())
                return false;

            return sh.holder == this.holder;
        }

        return false;
    }

    @Override
    public void onConflict() {
        expired = true;

        expire();
    }
}

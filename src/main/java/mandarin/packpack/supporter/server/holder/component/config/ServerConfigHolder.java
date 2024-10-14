package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Conflictable;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public abstract class ServerConfigHolder extends ComponentHolder implements Conflictable {
    protected final Message message;

    protected final IDHolder holder;
    protected final IDHolder backup;

    public ServerConfigHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.message = message;

        this.holder = holder;
        backup = this.holder.clone();

        registerAutoExpiration(FIVE_MIN);
    }

    public ServerConfigHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.message = message;

        this.holder = holder;
        this.backup = backup;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public final void onExpire() {
        message.editMessage(LangID.getStringByID("serverConfig.expired", lang))
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
        expire();
    }
}

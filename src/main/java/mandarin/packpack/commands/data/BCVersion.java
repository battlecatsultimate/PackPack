package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class BCVersion extends ConstraintCommand {
    public BCVersion(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String versions = "BCEN : " +
                convertVersion(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.EN, false)) +
                "\n" +
                "BCTW : " +
                convertVersion(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.ZH, false)) +
                "\n" +
                "BCKR : " +
                convertVersion(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.KR, false)) +
                "\n" +
                "BCJP : " +
                convertVersion(StaticStore.event.getVersionCode(CommonStatic.Lang.Locale.JP, false));

        ch.sendMessage(versions).queue();
    }

    private String convertVersion(long version) {
        long main = version / 100000;

        version -= main * 100000;

        long major = version / 1000;

        version -= major * 1000;

        long minor = version % 1000;

        return main + "." + major + "." + minor;
    }
}

package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.BannerHolder;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1), false);
    }

    @Override
    protected void doThing(CommandLoader loader) {
        BannerHolder.BannerData data = StaticStore.bannerHolder.pickBanner();

        if (data == null) {
            return;
        }

        Calendar c = Calendar.getInstance();

        replyToMessageSafely(loader.getChannel(), "Banner Data : " + data + "\n\nCurrent Month : " + c.get(Calendar.MONTH), loader.getMessage(), a -> a);
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}
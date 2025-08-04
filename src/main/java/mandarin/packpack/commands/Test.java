package mandarin.packpack.commands;

import common.CommonStatic;
import kotlin.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1), false);
    }

    @Override
    protected void doThing(CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ch.sendMessage("Test Message")
                .setMessageReference((Message) null)
                .queue();

        StaticStore.renderManager.createRenderer(128, 128, new File("./temp"), c -> {
            for (int i = 0; i < 255; i++) {
                int finalI = i;

                c.queue(g -> {
                    g.setColor(255, 255, 255, finalI);

                    g.fillRect(0f, 0f, 128f, 128f);

                    return Unit.INSTANCE;
                });
            }

            return Unit.INSTANCE;
        }, frame -> Objects.requireNonNull(StaticStore.generateTempFile(new File("./temp"), "frame" + frame, "png", false)), () -> Unit.INSTANCE);
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}
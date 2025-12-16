package mandarin.packpack.commands.bot;

import com.sun.management.HotSpotDiagnosticMXBean;
import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;

public class DumpHeap extends ConstraintCommand {
    public DumpHeap(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch, loader.getMessage(), "Dumping Heap...");

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        File folder = new File("./temp");

        if (!folder.exists() && !folder.exists()) {
            replyToMessageSafely(ch, loader.getMessage(), "Failed to generate folder...");

            return;
        }

        File dumpFile = new File(folder, "heap.hprof");

        StaticStore.deleteFile(dumpFile, true);

        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

        hotSpotDiagnosticMXBean.dumpHeap(dumpFile.getAbsolutePath(), true);

        if (dumpFile.exists()) {
            replyToMessageSafely(ch, loader.getMessage(), "Dumping success, check file manually");
        } else {
            replyToMessageSafely(ch, loader.getMessage(), "Failed to dump heap...");
        }
    }
}

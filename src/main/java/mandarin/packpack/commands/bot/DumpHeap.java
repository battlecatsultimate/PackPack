package mandarin.packpack.commands.bot;

import com.sun.management.HotSpotDiagnosticMXBean;
import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;

public class DumpHeap extends ConstraintCommand {
    public DumpHeap(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        replyToMessageSafely(ch,  "Dumping Heap...", loader.getMessage(), a -> a);

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        File folder = new File("./temp");

        if (!folder.exists() && !folder.exists()) {
            replyToMessageSafely(ch, "Failed to generate folder...", loader.getMessage(), a -> a);

            return;
        }

        File dumpFile = new File(folder, "heap.hprof");

        StaticStore.deleteFile(dumpFile, true);

        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

        hotSpotDiagnosticMXBean.dumpHeap(dumpFile.getAbsolutePath(), true);

        if (dumpFile.exists()) {
            replyToMessageSafely(ch, "Dumping success, check file manually", loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(ch, "Failed to dump heap...", loader.getMessage(), a -> a);
        }
    }
}

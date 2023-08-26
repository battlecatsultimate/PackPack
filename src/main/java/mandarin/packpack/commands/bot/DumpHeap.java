package mandarin.packpack.commands.bot;

import com.sun.management.HotSpotDiagnosticMXBean;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;

public class DumpHeap extends ConstraintCommand {
    public DumpHeap(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if (ch == null)
            return;

        replyToMessageSafely(ch,  "Dumping Heap...", getMessage(event), a -> a);

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        File folder = new File("./temp");

        if (!folder.exists() && !folder.exists()) {
            replyToMessageSafely(ch, "Failed to generate folder...", getMessage(event), a -> a);

            return;
        }

        File dumpFile = new File(folder, "heap.hprof");

        StaticStore.deleteFile(dumpFile, true);

        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

        hotSpotDiagnosticMXBean.dumpHeap(dumpFile.getAbsolutePath(), true);

        if (dumpFile.exists()) {
            replyToMessageSafely(ch, "Dumping success, check file manually", getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, "Failed to dump heap...", getMessage(event), a -> a);
        }
    }
}

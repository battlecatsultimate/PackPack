package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnalyzeServer extends ConstraintCommand {
    public AnalyzeServer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        StringBuilder builder = new StringBuilder("----- SERVER ANALYSIS -----\n\n");

        List<Guild> guilds = new ArrayList<>();

        int i = 1;

        for(String id : StaticStore.idHolder.keySet()) {
            try {
                Guild g = client.getGuildById(id);

                if (g != null) {
                    guilds.add(g);
                }
            } catch (Exception ignored) {}
        }

        guilds.sort((g1, g2) -> {
            if (g1 == null && g2 == null) {
                return 0;
            }

            if (g1 == null) {
                return 1;
            }

            if (g2 == null) {
                return -1;
            }

            return -Integer.compare(g1.getMemberCount(), g2.getMemberCount());
        });

        for (Guild g : guilds) {
            String id = g.getId();
            IDHolder idHolder = StaticStore.idHolder.get(id);

            if (idHolder == null) {
                continue;
            }

            String size;
            int s = g.getMemberCount();

            if(s > 1000)
                size = "Large";
            else if(s > 100)
                size = "Medium";
            else
                size = "Small";

            builder.append("Server No. ")
                    .append(i)
                    .append("\n")
                    .append("Name : ")
                    .append(g.getName())
                    .append(" (")
                    .append(id)
                    .append(")\n")
                    .append("Number of users : ")
                    .append(g.getMemberCount())
                    .append(" (")
                    .append(size)
                    .append(")\n")
                    .append("Owner : ");

            AtomicBoolean running = new AtomicBoolean(true);

            g.retrieveOwner().queue(owner -> {
                if(owner == null) {
                    builder.append("Unknown\n\n");
                } else {
                    User user = owner.getUser();

                    builder.append(user.getEffectiveName())
                            .append(" (")
                            .append(user.getId())
                            .append(")")
                            .append("\n");
                }

                running.set(false);
            }, e -> running.set(false));

            while(true) {
                if (!running.get())
                    break;
            }

            Role role;

            if (idHolder.moderator == null) {
                role = null;
            } else {
                role = g.getRoleById(idHolder.moderator);
            }

            if(role == null) {
                builder.append("\nisProperlySet? : No moderator role\n\n");
            } else {
                builder.append("isProperlySet? : ")
                        .append(!role.getName().equals("PackPackMod"))
                        .append("\nisFully Set? :")
                        .append(!role.getName().equals("PackPackMod") && idHolder.member != null)
                        .append("\n\n");
            }

            i++;
        }

        File f = new File("./temp");

        if(!f.exists() && !f.mkdirs()) {
            StaticStore.logger.uploadLog("Couldn't create folder : "+f.getAbsolutePath());
            return;
        }

        File analysis = StaticStore.generateTempFile(f, "analysis", "", true);

        if(analysis == null) {
            return;
        }

        File text = new File(analysis, "analysis.txt");

        if(!text.createNewFile()) {
            StaticStore.logger.uploadLog("Couldn't create file : "+text.getAbsolutePath());
            return;
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(text));

        writer.write(builder.toString());

        writer.close();

        sendMessageWithFile(ch, "Analyzed " + StaticStore.idHolder.size() + " servers", text, "Analysis.txt");
    }
}

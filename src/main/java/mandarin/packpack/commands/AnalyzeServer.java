package mandarin.packpack.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.UserData;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.*;

public class AnalyzeServer extends ConstraintCommand {
    private final GatewayDiscordClient client;

    public AnalyzeServer(ROLE role, int lang, IDHolder id, GatewayDiscordClient client) {
        super(role, lang, id);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        StringBuilder builder = new StringBuilder("----- SERVER ANALYSIS -----\n\n");

        int i = 1;

        for(String id : StaticStore.idHolder.keySet()) {
            try {
                IDHolder idHolder = StaticStore.idHolder.get(id);
                Guild g = client.getGuildById(Snowflake.of(id)).block();

                if(g != null) {
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

                    Member owner = g.getOwner().block();

                    if(owner == null) {
                        builder.append("Unknown\n\n");

                        i++;

                        continue;
                    }

                    UserData user = owner.getUserData();

                    builder.append(user.username())
                            .append("#")
                            .append(user.discriminator())
                            .append(" (")
                            .append(user.id().asString())
                            .append(")")
                            .append("\n");

                    Role role = g.getRoleById(Snowflake.of(idHolder.MOD)).block();

                    if(role == null) {
                        builder.append("\nisProperlySet? : Unknown\n\n");

                        i++;

                        continue;
                    }

                    builder.append("isProperlySet? : ")
                            .append(!role.getName().equals("PackPackMod"))
                            .append("\nisFully Set? :")
                            .append(!role.getName().equals("PackPackMod") && idHolder.MEMBER != null)
                            .append("\n\n");


                    i++;
                }
            } catch (Exception ignored) {}
        }

        File f = new File("./temp");

        if(!f.exists() && !f.mkdirs()) {
            StaticStore.logger.uploadLog("Couldn't create folder : "+f.getAbsolutePath());
            return;
        }

        File analysis = new File(f, StaticStore.findFileName(f, "analysis", ""));

        if(!analysis.mkdirs()) {
            StaticStore.logger.uploadLog("Couldn't create folder : "+analysis.getAbsolutePath());
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

        FileInputStream fis = new FileInputStream(text);

        createMessage(ch, m -> {
            m.content("Analyzed " + StaticStore.idHolder.size() + " servers");
            m.addFile(MessageCreateFields.File.of("Analysis.txt", fis));
        }, () -> {
            try {
                fis.close();

                StaticStore.deleteFile(analysis, true);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }
}

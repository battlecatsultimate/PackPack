package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ServerJson extends ConstraintCommand {
    public ServerJson(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        StaticStore.saveServerInfo();

        File f = new File("./data/serverinfo.json");

        if(f.exists()) {
            FileInputStream fis = new FileInputStream(f);

            Message msg = getMessage(event);

            if(msg != null) {
                msg.getAuthor().ifPresent(a -> a.getPrivateChannel().subscribe(channel -> channel.createMessage(m -> {
                    m.setContent("Sent serverinfo.json via DM");
                    m.addFile("serverinfo.json", fis);
                }).subscribe(null, null, () -> {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })));
            }
        }
    }
}

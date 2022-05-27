package mandarin.packpack.commands;

import common.util.Data;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.cell.AbilityData;
import mandarin.packpack.supporter.bc.cell.CellData;
import mandarin.packpack.supporter.bc.cell.FlagCellData;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.StatAnalyzerMessageHolder;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test extends GlobalTimedConstraintCommand {
    private final GatewayDiscordClient gate;

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID, GatewayDiscordClient gate) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));

        this.gate = gate;
    }

    @Override
    protected void doThing(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 3)
            createMessage(ch, m -> m.content("p!test -s/c/m [ID]"));

        switch (contents[1]) {
            case "-m":
                gate.getUserById(Snowflake.of(contents[2])).subscribe(u -> createMessage(ch, m -> m.content("User Name : "+u.getUsername()+u.getDiscriminator())));
                break;
            case "-c":
                gate.getChannelById(Snowflake.of(contents[2])).subscribe(c -> {
                    createMessage(ch, m -> m.content("Channel type : "+c.getType()));

                    if(c instanceof VoiceChannel) {
                        createMessage(ch, m -> m.content("Channel Name : "+((VoiceChannel) c).getName()));
                        createMessage(ch, m -> m.content("Server name : "+((VoiceChannel) c).getGuild().block().getName()));
                    } else if(c instanceof MessageChannel) {
                        createMessage(ch, m -> m.content("Channel Name : "+c.getRestChannel().getData().block().name().get()));
                        createMessage(ch, m -> m.content("Server name : "+(c.getRestChannel().getData().block().guildId().isAbsent() ? "None" : c.getRestChannel().getData().block().guildId().get())));
                    }
                });

                break;
            case "-s":
                gate.getGuildById(Snowflake.of(contents[2])).subscribe(g -> createMessage(ch, m -> m.content("Guild Name : "+g.getName())));

        }
    }

    @Override
    protected void setOptionalID(MessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}
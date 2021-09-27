package mandarin.packpack.commands.bot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

public class Suggest extends TimedConstraintCommand {
    private final GatewayDiscordClient client;

    public Suggest(ConstraintCommand.ROLE role, int lang, IDHolder id, long time, GatewayDiscordClient client) {
        super(role, lang, id, time, StaticStore.COMMAND_SUGGEST_ID);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> canGo = new AtomicReference<>(true);

        getMember(event).ifPresent(m -> {
            canGo.set(!StaticStore.suggestBanned.containsKey(m.getId().asString()));

            if(StaticStore.suggestBanned.containsKey(m.getId().asString())) {
                ch.createMessage(LangID.getStringByID("suggest_banned", lang).replace("_RRR_", StaticStore.suggestBanned.get(m.getId().asString()))).subscribe();
            }
        });

        if(!canGo.get()) {
            disableTimer();
            return;
        }

        String title = getTitle(getContent(event));

        if(title.isBlank()) {
            ch.createMessage(LangID.getStringByID("suggest_notitle", lang)).subscribe();
            disableTimer();
        } else {
            if(title.length() >= 256) {
                title = title.substring(0, 236)+"... (too long)";
            }

            String desc = getDescription(getContent(event));

            Mono<User> me = client.getUserById(Snowflake.of(StaticStore.MANDARIN_SMELL));

            String finalTitle = title;
            me.subscribe(u -> u.getPrivateChannel().subscribe(pc -> {
                EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

                builder.color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

                if(!desc.isBlank()) {
                    if(desc.length() >= 1024) {
                        String newDesc = desc.substring(0, 1004) + "... (too long)";


                        builder.addField("Description", newDesc, false);
                    } else {
                        builder.addField("Description", desc, false);
                    }
                }

                getMember(event).ifPresentOrElse(m -> {
                    builder.addField("Member ID", m.getId().asString(), true);
                    builder.addField("Member Name", m.getUsername(), true);
                    builder.author(finalTitle, null, m.getAvatarUrl());
                }, () -> builder.title(finalTitle));

                builder.addField("Channel ID" , ch.getId().asString(), false);

                Mono<Guild> mono = getGuild(event);

                if(mono != null) {
                    Guild g = mono.block();

                    if(g != null) {
                        builder.footer("From "+g.getName()+" | "+g.getId().asString(), null);
                    }
                }

                pc.createMessage(builder.build()).subscribe();
            }));

            System.out.println(desc);

            if(desc.length() >= 1024) {
                ch.createMessage(LangID.getStringByID("suggest_sentwarn", lang)).subscribe();
            } else {
                ch.createMessage(LangID.getStringByID("suggest_sent", lang)).subscribe();
            }
        }
    }

    public String getTitle(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2) {
            return "";
        } else {
            StringBuilder result = new StringBuilder();

            for(int i = 1; i < contents.length; i++) {
                if(contents[i].equals("-d") || contents[i].equals("-desc"))
                    break;
                else {
                    result.append(contents[i]);

                    if(i < contents.length - 1) {
                        result.append(" ");
                    }
                }
            }

            return result.toString();
        }
    }

    public String getDescription(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2) {
            return "";
        } else {
            StringBuilder result = new StringBuilder();

            boolean descStart = false;

            for(int i = 1; i < contents.length; i++) {
                if((contents[i].equals("-d") || contents[i].equals("-desc")) && !descStart) {
                    descStart = true;
                } else if(descStart) {
                    result.append(contents[i]);

                    if(i < contents.length - 1) {
                        result.append(" ");
                    }
                }
            }

            return result.toString();
        }
    }
}

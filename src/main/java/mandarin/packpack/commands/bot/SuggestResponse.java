package mandarin.packpack.commands.bot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class SuggestResponse extends ConstraintCommand {

    private final GatewayDiscordClient client;

    public SuggestResponse(ROLE role, int lang, IDHolder id, GatewayDiscordClient client) {
        super(role, lang, id);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 6);

        if(contents.length < 6) {
            ch.createMessage("Invalid Format : `p!suggestresponse [Guild ID] [Channel ID] [Member ID] [Suggestion ID] Contents`").subscribe();
            return;
        }

        client.getGuildById(Snowflake.of(contents[1])).subscribe(g -> g.getChannelById(Snowflake.of(contents[2])).subscribe(c -> {
            if(c instanceof TextChannel) {
                client.getUserById(Snowflake.of(StaticStore.MANDARIN_SMELL)).subscribe(me -> me.getPrivateChannel().subscribe(pc -> pc.getMessageById(Snowflake.of(contents[4])).subscribe(emb -> {
                    if(!emb.getEmbeds().isEmpty()) {
                        Embed embed = emb.getEmbeds().get(0);

                        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                                .title(LangID.getStringByID("response_title", lang))
                                .color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

                        embed.getAuthor().ifPresentOrElse(
                                au -> builder.addField(au.getName().orElse("Unknown"), contents[5], false),
                                () -> builder.addField("UNKNOWN", contents[5], false)
                        );

                        client.getUserById(Snowflake.of(contents[3])).subscribe(u -> builder.footer(LangID.getStringByID("response_suggestedby", lang).replace("_UUU_", u.getTag()), u.getAvatarUrl()));

                        ((TextChannel) c).createMessage(builder.build());
                    }
                })));
            }
        }));

        client.getUserById(Snowflake.of(contents[3])).subscribe(u -> u.getPrivateChannel().subscribe(pc -> client.getUserById(Snowflake.of(StaticStore.MANDARIN_SMELL)).subscribe(me -> me.getPrivateChannel().subscribe(mpc -> mpc.getMessageById(Snowflake.of(contents[4])).subscribe(emb -> {
            if(!emb.getEmbeds().isEmpty()) {
                Embed embed = emb.getEmbeds().get(0);

                EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

                builder.title(LangID.getStringByID("response_title", lang))
                        .description(LangID.getStringByID("response_desc", lang))
                        .color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

                embed.getAuthor().ifPresentOrElse(
                        au -> builder.addField(au.getName().orElse("UNKNOWN"), contents[5], false),
                        () -> builder.addField("UNKNOWN", contents[5], false)
                );

                pc.createMessage(builder.build()).subscribe();
            }
        })))));
    }
}

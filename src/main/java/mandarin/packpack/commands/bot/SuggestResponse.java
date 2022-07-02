package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class SuggestResponse extends ConstraintCommand {

    public SuggestResponse(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        JDA client = event.getJDA();

        String[] contents = getContent(event).split(" ", 6);

        if(contents.length < 6) {
            ch.sendMessage("Invalid Format : `p!suggestresponse [Guild ID] [Channel ID] [Member ID] [Suggestion ID] Contents`").queue();
            return;
        }

        Guild g = client.getGuildById(contents[1]);

        if(g == null)
            return;

        GuildChannel c = g.getGuildChannelById(contents[2]);

        if(c == null)
            return;

        if(c instanceof GuildMessageChannel) {
            User me = client.getUserById(StaticStore.MANDARIN_SMELL);

            if(me == null)
                return;

            Message emb = me.openPrivateChannel().complete().getHistory().getMessageById(contents[4]);

            if(emb == null || emb.getEmbeds().isEmpty())
                return;

            MessageEmbed embed = emb.getEmbeds().get(0);

            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(LangID.getStringByID("response_title", lang))
                    .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

            MessageEmbed.AuthorInfo info = embed.getAuthor();

            if(info == null)
                return;

            builder.addField(info.getName(), contents[5], false);

            User user = client.getUserById(contents[3]);

            if(user != null) {
                builder.setFooter(LangID.getStringByID("response_suggestedby", lang).replace("_UUU_", user.getAsTag()), user.getAvatarUrl());

                user.openPrivateChannel()
                        .flatMap(pc -> pc.sendMessageEmbeds(builder.build()))
                        .queue();
            }

            ((GuildMessageChannel) c).sendMessageEmbeds(builder.build()).queue();
        }
    }
}

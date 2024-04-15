package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

public class Suggest extends TimedConstraintCommand {
    public Suggest(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_SUGGEST_ID, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        User u = loader.getUser();

        if(StaticStore.suggestBanned.containsKey(u.getId())) {
            ch.sendMessage(LangID.getStringByID("suggest_banned", lang).replace("_RRR_", StaticStore.suggestBanned.get(u.getId()))).queue();

            disableTimer();

            return;
        }

        String title = getTitle(loader.getContent());

        if(title.isBlank()) {
            ch.sendMessage(LangID.getStringByID("suggest_notitle", lang)).queue();
            disableTimer();
        } else {
            if(title.length() >= 256) {
                title = title.substring(0, 236)+"... (too long)";
            }

            String desc = getDescription(loader.getContent());

            User me = client.getUserById(StaticStore.MANDARIN_SMELL);

            if(me != null) {
                EmbedBuilder builder = new EmbedBuilder();

                builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

                if(!desc.isBlank()) {
                    if(desc.length() >= 1024) {
                        String newDesc = desc.substring(0, 1004) + "... (too long)";


                        builder.addField("Description", newDesc, false);
                    } else {
                        builder.addField("Description", desc, false);
                    }
                }

                builder.addField("Member ID", u.getId(), true);
                builder.addField("Member Name", u.getName(), true);
                builder.setAuthor(title, null, u.getAvatarUrl());

                builder.addField("Channel ID" , ch.getId(), false);

                Guild g = loader.getGuild();

                builder.setFooter("From " + g.getName() + " | " + g.getId(), null);

                me.openPrivateChannel().flatMap(pc -> pc.sendMessageEmbeds(builder.build())).queue();
            }

            if(desc.length() >= 1024) {
                ch.sendMessage(LangID.getStringByID("suggest_sentwarn", lang)).queue();
            } else {
                ch.sendMessage(LangID.getStringByID("suggest_sent", lang)).queue();
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

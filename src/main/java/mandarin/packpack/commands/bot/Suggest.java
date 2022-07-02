package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Suggest extends TimedConstraintCommand {
    public Suggest(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_SUGGEST_ID);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        JDA client = event.getJDA();

        Member m = getMember(event);

        if(m == null)
            return;

        if(StaticStore.suggestBanned.containsKey(m.getId())) {
            ch.sendMessage(LangID.getStringByID("suggest_banned", lang).replace("_RRR_", StaticStore.suggestBanned.get(m.getId()))).queue();

            disableTimer();

            return;
        }

        String title = getTitle(getContent(event));

        if(title.isBlank()) {
            ch.sendMessage(LangID.getStringByID("suggest_notitle", lang)).queue();
            disableTimer();
        } else {
            if(title.length() >= 256) {
                title = title.substring(0, 236)+"... (too long)";
            }

            String desc = getDescription(getContent(event));

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

                builder.addField("Member ID", m.getId(), true);
                builder.addField("Member Name", m.getNickname(), true);
                builder.setAuthor(title, null, m.getAvatarUrl());

                builder.addField("Channel ID" , ch.getId(), false);

                Guild g = getGuild(event);

                if(g != null) {
                    builder.setFooter("From "+g.getName()+" | "+g.getId(), null);
                }

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

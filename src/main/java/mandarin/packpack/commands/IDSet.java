package mandarin.packpack.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class IDSet extends ConstraintCommand {
    private final IDHolder holder;

    public IDSet(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);

        holder = id;
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] msg = getMessage(event).split(" ");

        if(msg.length == 1) {
            ch.createMessage(LangID.getStringByID("idset_argu", lang));
        } else {
            StringBuilder result = new StringBuilder(LangID.getStringByID("idset_result", lang));

            Guild g = event.getGuild().block();

            if(g == null)
                result.append(LangID.getStringByID("idset_noguild", lang));
            else {
                boolean dev = false;
                boolean mod = false;
                boolean mem = false;
                boolean pre = false;
                boolean pc = false;
                boolean and = false;
                boolean mut = false;
                boolean get = false;

                for(int i = 0; i < msg.length; i++) {
                    switch (msg[i]) {
                        case "-d":
                        case "-dev":
                            if(!dev && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.DEV = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Developer").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    dev = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.DEV = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Developer").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_",id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Developer")).append("\n");
                            }
                            break;
                        case "-m":
                        case "-mod":
                            if(!mod && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.MOD = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Moderator").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    mod = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.MOD = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Moderator").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Moderator")).append("\n");
                            }
                            break;
                        case "-me":
                        case "-member":
                            if(!mem && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.MEMBER = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Member").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    mem = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.MEMBER = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Member").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Member")).append("\n");
                            }
                            break;
                        case "-p":
                        case "-premember":
                            if(!pre && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.PRE_MEMBER = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Pre-Member").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    pre = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.PRE_MEMBER = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Pre-Member").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Pre-Member")).append("\n");
                            }
                            break;
                        case "-mu":
                        case "-muted":
                            if(!mut && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.MUTED = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Muted").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    mut = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.MUTED = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Muted").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Muted")).append("\n");
                            }
                            break;
                        case "-pc":
                        case "-bcupc":
                            if(!pc && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.BCU_PC_USER = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-PC User").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    pc = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.BCU_PC_USER = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-PC User").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "BCU-PC User")).append("\n");
                            }
                            break;
                        case "-a":
                        case "-bcuandroid":
                            if(!and && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidID(g, id)) {
                                    holder.BCU_ANDROID = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-Android User").replace("=", getRoleIDWithName(id, event))).append("\n");

                                    and = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.BCU_ANDROID = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-Android User").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "BCU-Android User")).append("\n");
                            }
                            break;
                        case "-g":
                        case "-getaccess":
                            if(!get && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(isValidChannel(g, id)) {
                                    holder.GET_ACCESS = id;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_chanchange", lang).replace("_", getChannelIDWithName(id, g))).append("\n");

                                    get = true;
                                } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                    holder.GET_ACCESS = null;

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_chanchange", lang).replace("_", LangID.getStringByID("idset_none", lang))).append("\n");

                                    dev = true;
                                } else if(StaticStore.isNumeric(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_chaninvalid", lang).replace("_", id)).append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                                }

                                i++;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_chanignore", lang)).append("\n");
                            }
                            break;
                    }
                }

                result.append("\n").append(LangID.getStringByID("idset_result", lang));

                ch.createMessage(result.toString()).subscribe();
            }
        }
    }

    private boolean isValidID(Guild g, String id) {
        Set<Snowflake> ids = g.getRoleIds();

        for(Snowflake snow : ids) {
            if(snow.asString().equals(id))
                return true;
        }

        return false;
    }

    private boolean isValidChannel(Guild g, String id) {
        AtomicReference<Boolean> valid = new AtomicReference<>(false);

        g.getChannels().collectList().subscribe(l -> {
            for(GuildChannel gc : l) {
                if(gc.getType() == Channel.Type.GUILD_TEXT && id.equals(gc.getId().asString())) {
                    valid.set(true);
                    return;
                }
            }
        });

        return valid.get();
    }

    private String getRoleIDWithName(String id, MessageCreateEvent event) {
        return "`"+id+"`" + " [**"+StaticStore.roleNameFromID(event, id)+"**]";
    }

    private String getChannelIDWithName(String id, Guild g) {
        GuildChannel gc = g.getChannelById(Snowflake.of(id)).block();

        if(gc == null) {
            return id;
        }

        return "`"+id+"`" + "[**"+gc.getName()+"**]";
    }
}

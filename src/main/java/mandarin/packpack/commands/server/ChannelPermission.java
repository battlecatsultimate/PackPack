package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ChannelPermission extends ConstraintCommand {
    private static final int PARAM_ADD = 2;
    private static final int PARAM_REMOVE = 4;

    public ChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String pureMessage = getMessage(event).replaceAll("[ ]+,[ ]+|,[ ]+|[ ]+,", ",");

        String[] msg = pureMessage.split(" ");

        if(msg.length == 1) {
            Guild g = event.getGuild().block();

            if(g == null)
                return;

            String result = "Member : " + printChannels(holder.MEMBER) + "\n" +
                    "Pre-Member : " + printChannels(holder.PRE_MEMBER) + "\n" +
                    "Muted : " + printChannels(holder.MUTED) + "\n" +
                    "BCU-PC User : " + printChannels(holder.BCU_PC_USER) + "\n" +
                    "BCU-Android User : " + printChannels(holder.BCU_ANDROID);

            ch.createMessage(result).subscribe();
        } else {
            StringBuilder result = new StringBuilder(LangID.getStringByID("idset_result", lang));

            int param = checkParameter(pureMessage);

            Guild g = event.getGuild().block();

            if(g == null)
                result.append(LangID.getStringByID("idset_noguild", lang));
            else {
                boolean mem = false;
                boolean pre = false;
                boolean pc = false;
                boolean and = false;
                boolean mut = false;

                for(int i = 0; i < msg.length; i++) {
                    switch (msg[i]) {
                        case "-me":
                        case "-member":
                            if(!mem && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(holder.MEMBER != null) {
                                    ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                    if(channels == null) {
                                        holder.channel.put(holder.MEMBER, null);
                                    } else if(channels.isEmpty() && id.contains("all")) {
                                        holder.channel.put(holder.MEMBER, channels);
                                    } else {
                                        ArrayList<String> oldChannels = holder.channel.get(holder.MEMBER);

                                        if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                            holder.channel.put(holder.MEMBER, channels);
                                        } else if(oldChannels != null) {
                                            if((param & PARAM_ADD) > 0) {
                                                oldChannels.addAll(channels);
                                            } else {
                                                oldChannels.removeAll(channels);
                                            }

                                            holder.channel.put(holder.MEMBER, oldChannels);
                                        }
                                    }

                                    String allNone;

                                    if(channels == null)
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    else if(channels.isEmpty() && id.contains("all")) {
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    } else {
                                        allNone = printChannels(channels);
                                    }

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");

                                    if((param & PARAM_ADD) > 0) {
                                        result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "Member").replace("=", allNone).replace("::", printChannels(holder.MEMBER)));
                                    } else {
                                        result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "Member").replace("=", allNone).replace("::", printChannels(holder.MEMBER)));
                                    }

                                    result.append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noid", lang).replace("_", "Member")).append("\n");
                                }

                                mem = true;

                                i++;
                            } else if(i <msg.length - 1) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "Member")).append("\n");
                            }
                            break;
                        case "-p":
                        case "-premember":
                            if(!pre && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(holder.PRE_MEMBER != null) {
                                    ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                    if(channels == null) {
                                        holder.channel.put(holder.PRE_MEMBER, null);
                                    } else if(channels.isEmpty() && id.contains("all")) {
                                        holder.channel.put(holder.PRE_MEMBER, channels);
                                    } else {
                                        ArrayList<String> oldChannels = holder.channel.get(holder.PRE_MEMBER);

                                        if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                            holder.channel.put(holder.PRE_MEMBER, channels);
                                        } else if(oldChannels != null) {
                                            if((param & PARAM_ADD) > 0) {
                                                oldChannels.addAll(channels);
                                            } else {
                                                oldChannels.removeAll(channels);
                                            }

                                            holder.channel.put(holder.PRE_MEMBER, oldChannels);
                                        }
                                    }

                                    String allNone;

                                    if(channels == null)
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    else if(channels.isEmpty() && id.contains("all")) {
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    } else {
                                        allNone = printChannels(channels);
                                    }

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");

                                    if((param & PARAM_ADD) > 0) {
                                        result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "Pre-Member").replace("=", allNone).replace("::", printChannels(holder.PRE_MEMBER)));
                                    } else {
                                        result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "Pre-Member").replace("=", allNone).replace("::", printChannels(holder.PRE_MEMBER)));
                                    }

                                    result.append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noid", lang).replace("_", "Pre-Member")).append("\n");
                                }

                                pre = true;

                                i++;
                            } else if(i <msg.length - 1) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "Pre-Member")).append("\n");
                            }
                            break;
                        case "-mu":
                        case "-muted":
                            if(!mut && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(holder.MUTED != null) {
                                    ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                    if(channels == null) {
                                        holder.channel.put(holder.MUTED, null);
                                    } else if(channels.isEmpty() && id.contains("all")) {
                                        holder.channel.put(holder.MUTED, channels);
                                    } else {
                                        ArrayList<String> oldChannels = holder.channel.get(holder.MUTED);

                                        if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                            holder.channel.put(holder.MUTED, channels);
                                        } else if(oldChannels != null) {
                                            if((param & PARAM_ADD) > 0) {
                                                oldChannels.addAll(channels);
                                            } else {
                                                oldChannels.removeAll(channels);
                                            }

                                            holder.channel.put(holder.MUTED, oldChannels);
                                        }
                                    }

                                    String allNone;

                                    if(channels == null)
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    else if(channels.isEmpty() && id.contains("all")) {
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    } else {
                                        allNone = printChannels(channels);
                                    }

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");

                                    if((param & PARAM_ADD) > 0) {
                                        result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "Muted").replace("=", allNone).replace("::", printChannels(holder.MUTED)));
                                    } else {
                                        result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "Muted").replace("=", allNone).replace("::", printChannels(holder.MUTED)));
                                    }

                                    result.append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noid", lang).replace("_", "Muted")).append("\n");
                                }

                                mut = true;

                                i++;
                            } else if(i <msg.length - 1) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "Muted")).append("\n");
                            }
                            break;
                        case "-pc":
                        case "-bcupc":
                            if(!pc && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(holder.BCU_PC_USER != null) {
                                    ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                    if(channels == null) {
                                        holder.channel.put(holder.BCU_PC_USER, null);
                                    } else if(channels.isEmpty() && id.contains("all")) {
                                        holder.channel.put(holder.BCU_PC_USER, channels);
                                    } else {
                                        ArrayList<String> oldChannels = holder.channel.get(holder.BCU_PC_USER);

                                        if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                            holder.channel.put(holder.BCU_PC_USER, channels);
                                        } else if(oldChannels != null) {
                                            if((param & PARAM_ADD) > 0) {
                                                oldChannels.addAll(channels);
                                            } else {
                                                oldChannels.removeAll(channels);
                                            }

                                            holder.channel.put(holder.BCU_PC_USER, oldChannels);
                                        }
                                    }

                                    String allNone;

                                    if(channels == null)
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    else if(channels.isEmpty() && id.contains("all")) {
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    } else {
                                        allNone = printChannels(channels);
                                    }

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");

                                    if((param & PARAM_ADD) > 0) {
                                        result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "BCU-PC User").replace("=", allNone).replace("::", printChannels(holder.BCU_PC_USER)));
                                    } else {
                                        result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "BCU-PC User").replace("=", allNone).replace("::", printChannels(holder.BCU_PC_USER)));
                                    }

                                    result.append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noid", lang).replace("_", "BCU-PC User")).append("\n");
                                }

                                pc = true;

                                i++;
                            } else if(i <msg.length - 1) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "BCU-PC User")).append("\n");
                            }
                            break;
                        case "-an":
                        case "-bcuandroid":
                            if(!and && i < msg.length - 1) {
                                String id = msg[i+1];

                                if(holder.BCU_ANDROID != null) {
                                    ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                    if(channels == null) {
                                        holder.channel.put(holder.BCU_ANDROID, null);
                                    } else if(channels.isEmpty() && id.contains("all")) {
                                        holder.channel.put(holder.BCU_ANDROID, channels);
                                    } else {
                                        ArrayList<String> oldChannels = holder.channel.get(holder.BCU_ANDROID);

                                        if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                            holder.channel.put(holder.BCU_ANDROID, channels);
                                        } else if(oldChannels != null) {
                                            if((param & PARAM_ADD) > 0) {
                                                oldChannels.addAll(channels);
                                            } else {
                                                oldChannels.removeAll(channels);
                                            }

                                            holder.channel.put(holder.BCU_ANDROID, oldChannels);
                                        }
                                    }

                                    String allNone;

                                    if(channels == null)
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    else if(channels.isEmpty() && id.contains("all")) {
                                        allNone = LangID.getStringByID("channelpermission_all", lang);
                                    } else {
                                        allNone = printChannels(channels);
                                    }

                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");

                                    if((param & PARAM_ADD) > 0) {
                                        result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "BCU-Android User").replace("=", allNone).replace("::", printChannels(holder.BCU_ANDROID)));
                                    } else {
                                        result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "BCU-Android User").replace("=", allNone).replace("::", printChannels(holder.BCU_ANDROID)));
                                    }

                                    result.append("\n");
                                } else {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noid", lang).replace("_", "BCU-Android User")).append("\n");
                                }

                                and = true;

                                i++;
                            } else if(i <msg.length - 1) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "BCU-Android User")).append("\n");
                            }
                            break;
                    }
                }

                result.append("\n").append(LangID.getStringByID("idset_result", lang));

                StaticStore.saveServerInfo();

                ch.createMessage(result.toString()).subscribe();
            }
        }
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

    private String printChannels(String id) {
        if(id == null)
            return LangID.getStringByID("channelpermission_all", lang);

        ArrayList<String> channels = holder.channel.get(id);

        if(channels == null) {
            return LangID.getStringByID("channelpermission_all", lang);
        } else if(channels.isEmpty()) {
            return LangID.getStringByID("channelpermission_none", lang);
        } else {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < channels.size(); i++) {
                result.append("<#").append(channels.get(i)).append(">");

                if(i < channels.size() - 1) {
                    result.append(", ");
                }
            }

            return result.toString();
        }
    }

    private String printChannels(ArrayList<String> channels) {
        if(channels == null) {
            return LangID.getStringByID("channelpermission_all", lang);
        } else if(channels.isEmpty()) {
            return LangID.getStringByID("channelpermission_none", lang);
        } else {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < channels.size(); i++) {
                result.append("<#").append(channels.get(i)).append(">");

                if(i < channels.size() - 1) {
                    result.append(", ");
                }
            }

            return result.toString();
        }
    }

    private ArrayList<String> getChannelList(Guild g, String message, boolean add) {
        String[] contents = message.split(",");

        ArrayList<String> result = new ArrayList<>();

        for (String content : contents) {
            if (content.equals("all")) {
                if (add) {
                    return null;
                } else {
                    return new ArrayList<>();
                }
            } else if (content.startsWith("<")) {
                String id = getIDFromMention(content);

                if (isValidChannel(g, id)) {
                    result.add(id);
                }
            } else if (isValidChannel(g, content)) {
                result.add(content);
            }
        }

        return result;
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        for (String content : contents) {
            if (content.equals("-a")) {
                res |= PARAM_ADD;
                break;
            } else if (content.equals("-r")) {
                res |= PARAM_REMOVE;
                break;
            }
        }

        if(res == 1)
            return 2;

        return res;
    }

    private String getIDFromMention(String mention) {
        return mention.replace("<#", "").replace(">", "");
    }
}

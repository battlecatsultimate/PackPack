package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChannelPermission extends ConstraintCommand {
    private static final int PARAM_ADD = 2;
    private static final int PARAM_REMOVE = 4;

    public ChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String pureMessage = getContent(event).replaceAll("[ ]+,[ ]+|,[ ]+|[ ]+,", ",");

        String[] msg = pureMessage.split(" ");

        if(msg.length == 1) {
            Guild g = getGuild(event).block();

            if(g == null)
                return;

            StringBuilder result = new StringBuilder("Member : " + printChannels(holder.MEMBER) + "\n");

            for(String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if(id == null)
                    continue;

                result.append(limitName(name)).append(" : ").append(printChannels(id)).append("\n");
            }

            if(result.length() < 2000) {
                ch.createMessage(result.toString()).subscribe();
            } else {
                ch.createMessage(LangID.getStringByID("idset_toobig", lang)).subscribe();
            }
        } else {
            StringBuilder result = new StringBuilder(LangID.getStringByID("idset_result", lang));

            int param = checkParameter(pureMessage);

            Guild g = getGuild(event).block();

            if(g == null)
                result.append(LangID.getStringByID("idset_noguild", lang));
            else {
                boolean mem = false;

                List<String> customName = new ArrayList<>();

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
                        case "-c":
                        case "-custom":
                            if(i < msg.length - 1 && msg[i + 1].startsWith("\"")) {
                                Object[] set = getName(msg, i + 1);

                                if(set == null) {
                                    result.append(msg[i]).append(" : ");
                                    result.append(LangID.getStringByID("idset_opened", lang));
                                    continue;
                                }

                                String name = (String) set[0];

                                if(!holder.ID.containsKey(name)) {
                                    result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_noname", lang)).append("\n");
                                    continue;
                                }

                                int index = (int) set[1];

                                if(index >= msg.length) {
                                    result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_nochannel", lang)).append("\n");
                                    continue;
                                }

                                if(customName.contains(name)) {
                                    result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                    result.append(LangID.getStringByID("channelpermission_ignorecu", lang)).append("\n");
                                    i = index;
                                    continue;
                                }

                                String id = msg[index];

                                String chID = holder.ID.get(name);

                                ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                                if(channels == null) {
                                    holder.channel.put(chID, null);
                                } else if(channels.isEmpty() && id.contains("all")) {
                                    holder.channel.put(chID, channels);
                                } else {
                                    ArrayList<String> oldChannels = holder.channel.get(chID);

                                    if(oldChannels == null && (param & PARAM_ADD) > 0) {
                                        holder.channel.put(chID, channels);
                                    } else if(oldChannels != null) {
                                        if((param & PARAM_ADD) > 0) {
                                            oldChannels.addAll(channels);
                                        } else {
                                            oldChannels.removeAll(channels);
                                        }

                                        holder.channel.put(chID, oldChannels);
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

                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");

                                if((param & PARAM_ADD) > 0) {
                                    result.append(LangID.getStringByID("channelpermission_added", lang).replace("=", allNone).replace("::", printChannels(chID)).replace("_", limitName(name)));
                                } else {
                                    result.append(LangID.getStringByID("channelpermission_removed", lang).replace("=", allNone).replace("::", printChannels(holder.MEMBER)).replace("_", limitName(name)));
                                }

                                result.append("\n");

                                i = index;

                                customName.add(name);
                            } else if(i < msg.length - 1 && !msg[i + 1].startsWith("\"")) {
                                result.append(msg[i]).append(" ").append(" : ");
                                result.append(LangID.getStringByID("channelpermission_format", lang)).append("\n");
                            }
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

    private String limitName(String name) {
        if(name.length() > 20) {
            return name.substring(0, 17) + "...";
        }

        return name;
    }

    private Object[] getName(String[] contents, int start) {
        StringBuilder res = new StringBuilder();

        res.append(contents[start]);

        if(contents[start].endsWith("\""))
            return new Object[] {res.substring(1, res.length() - 1), start + 1};
        else
            res.append(" ");

        int last;
        boolean ended = false;

        for(last = start + 1; last < contents.length; last++) {
            res.append(contents[last]);

            if(contents[last].endsWith("\"")) {
                ended = true;
                break;
            } else {
                res.append(" ");
            }
        }


        if(!ended)
            return null;

        return new Object[] {res.substring(1, res.length() - 1), last + 1};
    }
}

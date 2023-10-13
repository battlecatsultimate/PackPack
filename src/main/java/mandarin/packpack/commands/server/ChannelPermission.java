package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChannelPermission extends ConstraintCommand {
    private static final int PARAM_ADD = 2;
    private static final int PARAM_REMOVE = 4;

    public ChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String pureMessage = loader.getContent().replaceAll(" +, +|, +| +,", ",");

        String[] msg = pureMessage.split(" ");

        if(msg.length == 1) {

            StringBuilder result = new StringBuilder("Member : " + printChannels(holder.MEMBER == null ? "Member" : holder.MEMBER) + "\n");

            for(String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if(id == null)
                    continue;

                result.append(limitName(name)).append(" : ").append(printChannels(id)).append("\n");
            }

            if(result.length() < 2000) {
                ch.sendMessage(result.toString()).queue();
            } else {
                ch.sendMessage(LangID.getStringByID("idset_toobig", lang)).queue();
            }
        } else {
            StringBuilder result = new StringBuilder(LangID.getStringByID("idset_result", lang));

            int param = checkParameter(pureMessage);

            Guild g = loader.getGuild();

            boolean mem = false;

            List<String> customName = new ArrayList<>();

            for(int i = 0; i < msg.length; i++) {
                switch (msg[i]) {
                    case "-me", "-member" -> {
                        if (!mem && i < msg.length - 1) {
                            String id = msg[i + 1];

                            ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                            String memberID = holder.MEMBER;

                            if (memberID == null)
                                memberID = "Member";

                            if (channels == null) {
                                holder.channel.put(memberID, null);
                            } else if (channels.isEmpty() && id.contains("all")) {
                                holder.channel.put(memberID, channels);
                            } else {
                                List<String> oldChannels = holder.channel.get(memberID);

                                if (oldChannels == null && (param & PARAM_ADD) > 0) {
                                    holder.channel.put(memberID, channels);
                                } else if (oldChannels != null) {
                                    if ((param & PARAM_ADD) > 0) {
                                        oldChannels.addAll(channels);
                                    } else {
                                        oldChannels.removeAll(channels);
                                    }

                                    holder.channel.put(memberID, oldChannels);
                                }
                            }

                            String allNone;

                            if (channels == null)
                                allNone = LangID.getStringByID("channelpermission_all", lang);
                            else if (channels.isEmpty() && id.contains("all")) {
                                allNone = LangID.getStringByID("channelpermission_all", lang);
                            } else {
                                allNone = printChannels(channels);
                            }

                            result.append(msg[i]).append(" ").append(msg[i + 1]).append(" : ");

                            if ((param & PARAM_ADD) > 0) {
                                result.append(LangID.getStringByID("channelpermission_added", lang).replace("_", "Member").replace("=", allNone).replace("::", printChannels(memberID)));
                            } else {
                                result.append(LangID.getStringByID("channelpermission_removed", lang).replace("_", "Member").replace("=", allNone).replace("::", printChannels(memberID)));
                            }

                            result.append("\n");

                            mem = true;

                            i++;
                        } else if (i < msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i + 1]).append(" : ");
                            result.append(LangID.getStringByID("channelpermission_ignore", lang).replace("_", "Member")).append("\n");
                        }
                    }
                    case "-c", "-custom" -> {
                        if (i < msg.length - 1 && msg[i + 1].startsWith("\"")) {
                            Object[] set = getName(msg, i + 1);

                            if (set == null) {
                                result.append(msg[i]).append(" : ");
                                result.append(LangID.getStringByID("idset_opened", lang));
                                continue;
                            }

                            String name = (String) set[0];

                            if (!holder.ID.containsKey(name)) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_noname", lang)).append("\n");
                                continue;
                            }

                            int index = (int) set[1];

                            if (index >= msg.length) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_nochannel", lang)).append("\n");
                                continue;
                            }

                            if (customName.contains(name)) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("channelpermission_ignorecu", lang)).append("\n");
                                i = index;
                                continue;
                            }

                            String id = msg[index];

                            String chID = holder.ID.get(name);

                            ArrayList<String> channels = getChannelList(g, id, (param & PARAM_ADD) > 0);

                            if (channels == null) {
                                holder.channel.put(chID, null);
                            } else if (channels.isEmpty() && id.contains("all")) {
                                holder.channel.put(chID, channels);
                            } else {
                                List<String> oldChannels = holder.channel.get(chID);

                                if (oldChannels == null && (param & PARAM_ADD) > 0) {
                                    holder.channel.put(chID, channels);
                                } else if (oldChannels != null) {
                                    if ((param & PARAM_ADD) > 0) {
                                        oldChannels.addAll(channels);
                                    } else {
                                        oldChannels.removeAll(channels);
                                    }

                                    holder.channel.put(chID, oldChannels);
                                }
                            }

                            String allNone;

                            if (channels == null)
                                allNone = LangID.getStringByID("channelpermission_all", lang);
                            else if (channels.isEmpty() && id.contains("all")) {
                                allNone = LangID.getStringByID("channelpermission_all", lang);
                            } else {
                                allNone = printChannels(channels);
                            }

                            result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");

                            if ((param & PARAM_ADD) > 0) {
                                result.append(LangID.getStringByID("channelpermission_added", lang).replace("=", allNone).replace("::", printChannels(chID)).replace("_", limitName(name)));
                            } else {
                                result.append(LangID.getStringByID("channelpermission_removed", lang).replace("=", allNone).replace("::", printChannels(chID)).replace("_", limitName(name)));
                            }

                            result.append("\n");

                            i = index;

                            customName.add(name);
                        } else if (i < msg.length - 1 && !msg[i + 1].startsWith("\"")) {
                            result.append(msg[i]).append(" ").append(" : ");
                            result.append(LangID.getStringByID("channelpermission_format", lang)).append("\n");
                        }
                    }
                }
            }

            result.append("\n").append(LangID.getStringByID("idset_result", lang));

            StaticStore.saveServerInfo();

            ch.sendMessage(result.toString()).queue();
        }
    }

    private boolean isValidChannel(Guild g, String id) {
        List<GuildChannel> channels = g.getChannels();

        for(GuildChannel gc : channels) {
            if(gc.getType() == ChannelType.TEXT && id.equals(gc.getId()))
                return true;
        }

        return false;
    }

    private String printChannels(String id) {
        if(id == null)
            return LangID.getStringByID("channelpermission_all", lang);

        List<String> channels = Objects.requireNonNull(holder).channel.get(id);

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

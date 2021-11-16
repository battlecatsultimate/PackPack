package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
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
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Guild g = getGuild(event).block();

        StringBuilder result = new StringBuilder();

        if(g == null) {
            result.append(LangID.getStringByID("idset_noguild", lang));
            ch.createMessage(result.toString()).subscribe();
            return;
        }

        String[] msg = getContent(event).split(" ");

        if(msg.length == 1) {
            StringBuilder res = new StringBuilder("Moderator : " + getRoleIDWithName(holder.MOD) + "\n" +
                    "Member : " + (holder.MEMBER == null ? "None" : getRoleIDWithName(holder.MEMBER)) + "\n" +
                    "Booster User : " + (holder.BOOSTER == null ? "None" : getRoleIDWithName(holder.BOOSTER)) + "\n" +
                    "Get-Access : " + (holder.GET_ACCESS == null ? "None" : getChannelIDWithName(holder.GET_ACCESS, g)) + "\n" +
                    "Announcement : " + (holder.ANNOUNCE == null ? "None" : getChannelIDWithName(holder.ANNOUNCE, g)) + "\n" +
                    "Publish : "+ holder.publish);

            if(!holder.ID.isEmpty()) {
                res.append("\n\nCustom roles\n\n");
            }

            for(String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                res.append(name).append(" : ").append(id == null ? "None" : getRoleIDWithName(id)).append("\n");
            }

            createMessageWithNoPings(ch, res.toString());
        } else {
            result.append(LangID.getStringByID("idset_result", lang));

            List<String> customName = new ArrayList<>();

            boolean mod = false;
            boolean mem = false;
            boolean get = false;
            boolean ann = false;
            boolean pub = false;
            boolean boo = false;

            for(int i = 0; i < msg.length; i++) {
                switch (msg[i]) {
                    case "-m":
                    case "-mod":
                        if(!mod && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                if(alreadyBeingUsed(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_used", lang).replace("_", id)).append("\n");
                                    continue;
                                }

                                holder.MOD = id;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Moderator").replace("=", getRoleIDWithName(id))).append("\n");

                                mod = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Moderator")).append("\n");
                        }
                        break;
                    case "-me":
                    case "-member":
                        if(!mem && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                if(alreadyBeingUsed(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_used", lang).replace("_", id)).append("\n");
                                    continue;
                                }

                                String oldID = holder.MEMBER;

                                holder.MEMBER = id;

                                if(oldID != null && holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                    holder.channel.remove(oldID);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Member").replace("=", getRoleIDWithName(id))).append("\n");

                                mem = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.MEMBER);

                                holder.MEMBER = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Member").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                mem = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Member")).append("\n");
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

                                get = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_chaninvalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                            result.append(LangID.getStringByID("idset_chanignore", lang)).append("\n");
                        }
                        break;
                    case "-ann":
                    case "-announcement":
                        if(!ann && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidChannel(g, id)) {
                                holder.ANNOUNCE = id;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_annchange", lang).replace("_", getChannelIDWithName(id, g))).append("\n");

                                ann = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.ANNOUNCE = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_annchange", lang).replace("_", LangID.getStringByID("idset_none", lang))).append("\n");

                                ann = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_chaninvalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                            result.append(LangID.getStringByID("idset_annignore", lang)).append("\n");
                        }
                        break;
                    case "-pub":
                    case "-publish":
                        if(!pub && i < msg.length - 1) {
                            String value = msg[i+1];

                            if(value.toLowerCase(Locale.ENGLISH).equals("true")) {
                                holder.publish = true;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_pubchangetrue", lang)).append("\n");

                                pub = true;
                            } else if(value.toLowerCase(Locale.ENGLISH).equals("false")) {
                                holder.publish = false;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_pubchangefalse", lang)).append("\n");

                                pub = true;
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_pubinvalid", lang)).append("\n");
                            }
                        } else if(i < msg.length - 1) {
                            result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                            result.append(LangID.getStringByID("idset_pubignore", lang)).append("\n");
                        }
                        break;
                    case "-b":
                    case "-booster":
                        if(!boo && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                if(alreadyBeingUsed(id)) {
                                    result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                    result.append(LangID.getStringByID("idset_used", lang).replace("_", id)).append("\n");
                                    continue;
                                }

                                String oldID = holder.BOOSTER;

                                holder.BOOSTER = id;

                                if(oldID != null && holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                    holder.channel.remove(oldID);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Booster User").replace("=", getRoleIDWithName(id))).append("\n");

                                boo = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.BOOSTER);

                                holder.BOOSTER = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Booster User").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                boo = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Booster User")).append("\n");
                        }
                        break;
                    case "-c":
                    case "-custom":
                        if(i < msg.length - 1 && msg[i + 1].startsWith("\"")) {
                            Object[] set = getName(msg, i + 1);

                            if(set == null) {
                                result.append(msg[i]).append(" : ");
                                result.append(LangID.getStringByID("idset_opened", lang)).append("\n");
                                continue;
                            }

                            String name = (String) set[0];
                            int index = (int) set[1];

                            if(index >= msg.length) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_noroleid", lang)).append("\n");
                                continue;
                            }

                            if(!StaticStore.isNumeric(msg[index]) && !msg[index].toLowerCase(Locale.ENGLISH).equals("none")) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", msg[index])).append("\n");
                                continue;
                            }

                            String id = msg[index].toLowerCase(Locale.ENGLISH);

                            if(!id.equals("none") && !isValidID(g, id)) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                                continue;
                            }

                            if(alreadyBeingUsed(id)) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_used", lang).replace("_", id)).append("\n");
                                continue;
                            }

                            if(customName.contains(name)) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_already", lang)).append("\n");
                                i = index;
                                continue;
                            }

                            if(!holder.ID.containsKey(name) && id.equals("none")) {
                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_noid", lang)).append("\n");
                                i = index;
                                continue;
                            }

                            if(holder.ID.containsKey(name)) {
                                String oldID = holder.ID.get(name);

                                if(id.equals("none")) {
                                    holder.ID.remove(name);
                                } else {
                                    holder.ID.put(name, id);

                                    ArrayList<String> channel = holder.channel.get(oldID);

                                    holder.channel.put(id, channel);
                                }

                                holder.channel.remove(oldID);

                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_change", lang).replace("_OOO_", getRoleIDWithName(oldID)).replace("_III_", id.equals("none") ? "None" : getRoleIDWithName(id)).replace("_NNN_", name)).append("\n");
                            } else {
                                holder.ID.put(name, id);

                                result.append(msg[i]).append(" ").append(limitName(name)).append(" : ");
                                result.append(LangID.getStringByID("idset_custom", lang).replace("_III_", getRoleIDWithName(id)).replace("_NNN_", name)).append("\n");
                            }

                            customName.add(name);

                            i = index;
                        } else if(i < msg.length - 1 && !msg[i + 1].startsWith("\"")) {
                            result.append(msg[i]).append(" ").append(" : ");
                            result.append(LangID.getStringByID("idset_format", lang)).append("\n");
                        }
                }
            }

            result.append("\n").append(LangID.getStringByID("idset_result", lang));

            if(result.length() > 2000) {
                createMessageWithNoPings(ch, LangID.getStringByID("idset_toobig", lang));
            } else {
                createMessageWithNoPings(ch, result.toString());
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
                if((gc.getType() == Channel.Type.GUILD_TEXT || gc.getType() == Channel.Type.GUILD_NEWS) && id.equals(gc.getId().asString())) {
                    valid.set(true);
                    return;
                }
            }
        });

        return valid.get();
    }

    private String getRoleIDWithName(String id) {
        return "`"+id+"`" + " [<@&"+id+">]";
    }

    private String getChannelIDWithName(String id, Guild g) {
        GuildChannel gc = g.getChannelById(Snowflake.of(id)).block();

        if(gc == null) {
            return id;
        }

        return "`"+id+"`" + "[**<#"+id+">**]";
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

    private boolean alreadyBeingUsed(String id) {
        if(id.equals("none"))
            return false;

        boolean res = id.equals(holder.MOD) || id.equals(holder.MEMBER) || id.equals(holder.BOOSTER);

        if(res)
            return true;

        for(String i : holder.ID.values()) {
            if (id.equals(i))
                return true;
        }

        return false;
    }

    private String limitName(String name) {
        if(name.length() > 20) {
            return name.substring(0, 17) + "...";
        }

        return name;
    }
}

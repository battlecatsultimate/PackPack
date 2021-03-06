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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class IDSet extends ConstraintCommand {
    private final IDHolder holder;

    public IDSet(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);

        holder = id;
    }

    @SuppressWarnings("ConstantConditions")
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
            String res = "Moderator : " + getRoleIDWithName(holder.MOD, g) + "\n" +
                    "Member : " + (holder.MEMBER == null ? "None" : getRoleIDWithName(holder.MEMBER, g)) + "\n" +
                    "Pre-Member : " + (holder.PRE_MEMBER == null ? "None" : getRoleIDWithName(holder.PRE_MEMBER, g)) + "\n" +
                    "Muted : " + (holder.MUTED == null ? "None" : getRoleIDWithName(holder.MUTED, g)) + "\n" +
                    "BCU-PC User : " + (holder.BCU_PC_USER == null ? "None" : getRoleIDWithName(holder.BCU_PC_USER, g)) + "\n" +
                    "BCU-Android User : " + (holder.BCU_ANDROID == null ? "None" : getRoleIDWithName(holder.BCU_ANDROID, g)) + "\n" +
                    "Get-Access : " + (holder.GET_ACCESS == null ? "None" : getChannelIDWithName(holder.GET_ACCESS, g)) + "\n" +
                    "Announcement : " + (holder.ANNOUNCE == null ? "None" : getChannelIDWithName(holder.ANNOUNCE, g)) + "\n" +
                    "Publish : "+ holder.publish;

            ch.createMessage(res).subscribe();
        } else {
            result.append(LangID.getStringByID("idset_result", lang));

            boolean mod = false;
            boolean mem = false;
            boolean pre = false;
            boolean pc = false;
            boolean and = false;
            boolean mut = false;
            boolean get = false;
            boolean ann = false;
            boolean pub = false;

            for(int i = 0; i < msg.length; i++) {
                switch (msg[i]) {
                    case "-m":
                    case "-mod":
                        if(!mod && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                holder.MOD = id;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Moderator").replace("=", getRoleIDWithName(id, g))).append("\n");

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
                                String oldID = holder.MEMBER;

                                holder.MEMBER = id;

                                if(holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Member").replace("=", getRoleIDWithName(id, g))).append("\n");

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
                    case "-p":
                    case "-premember":
                        if(!pre && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                String oldID = holder.PRE_MEMBER;

                                holder.PRE_MEMBER = id;

                                if(holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Pre-Member").replace("=", getRoleIDWithName(id, g))).append("\n");

                                pre = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.PRE_MEMBER);

                                holder.PRE_MEMBER = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Pre-Member").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                pre = true;
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
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Pre-Member")).append("\n");
                        }
                        break;
                    case "-mu":
                    case "-muted":
                        if(!mut && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                String oldID = holder.MUTED;

                                holder.MUTED = id;

                                if(holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Muted").replace("=", getRoleIDWithName(id, g))).append("\n");

                                mut = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.MUTED);

                                holder.MUTED = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "Muted").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                mut = true;
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
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "Muted")).append("\n");
                        }
                        break;
                    case "-pc":
                    case "-bcupc":
                        if(!pc && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                String oldID = holder.BCU_PC_USER;

                                holder.BCU_PC_USER = id;

                                if(holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-PC User").replace("=", getRoleIDWithName(id, g))).append("\n");

                                pc = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.BCU_PC_USER);

                                holder.BCU_PC_USER = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-PC User").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                pc = true;
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
                            result.append(LangID.getStringByID("idset_ignore", lang).replace("_", "BCU-PC User")).append("\n");
                        }
                        break;
                    case "-an":
                    case "-bcuandroid":
                        if(!and && i < msg.length - 1) {
                            String id = msg[i+1];

                            if(isValidID(g, id)) {
                                String oldID = holder.BCU_ANDROID;

                                holder.BCU_ANDROID = id;

                                if(holder.channel.containsKey(oldID)) {
                                    ArrayList<String> arr = holder.channel.get(oldID);

                                    holder.channel.put(id, arr);
                                }

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-Android User").replace("=", getRoleIDWithName(id, g))).append("\n");

                                and = true;
                            } else if(id.toLowerCase(Locale.ENGLISH).equals("none")) {
                                holder.channel.remove(holder.BCU_ANDROID);

                                holder.BCU_ANDROID = null;

                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_idchange", lang).replace("_", "BCU-Android User").replace("=", LangID.getStringByID("idset_none", lang))).append("\n");

                                and = true;
                            } else if(StaticStore.isNumeric(id)) {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_invalid", lang).replace("_", id)).append("\n");
                            } else {
                                result.append(msg[i]).append(" ").append(msg[i+1]).append(" : ");
                                result.append(LangID.getStringByID("idset_numeric", lang).replace("_", id)).append("\n");
                            }

                            i++;
                        } else if(i <msg.length - 1) {
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
                }
            }

            result.append("\n").append(LangID.getStringByID("idset_result", lang));

            createMessageWithNoPings(ch, result.toString());
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

    private String getRoleIDWithName(String id, Guild g) {
        return "`"+id+"`" + " [**"+StaticStore.roleNameFromID(g, id)+"**]";
    }

    private String getChannelIDWithName(String id, Guild g) {
        GuildChannel gc = g.getChannelById(Snowflake.of(id)).block();

        if(gc == null) {
            return id;
        }

        return "`"+id+"`" + "[**<#"+id+">**]";
    }
}

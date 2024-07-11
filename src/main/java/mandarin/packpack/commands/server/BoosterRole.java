package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterRole extends ConstraintCommand {
    private final static String hexString = "0123456789abcdef";
    private final static Pattern p = Pattern.compile("\".+\"");

    public BoosterRole(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        if(g.getRoles().size() == 250) {
            ch.sendMessage(LangID.getStringByID("boorole_max", lang)).queue();

            return;
        }

        IDHolder holder = StaticStore.idHolder.get(g.getId());

        if(holder.BOOSTER == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_norole", lang));
            return;
        }

        String id = getUserID(loader.getContent());

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_invalidid", lang));
            return;
        }

        String name = getRoleName(loader.getContent());

        if(name == null || name.isBlank()) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_norolename", lang));
            return;
        }

        int c = getColor(loader.getContent(), ch);

        if(c == -1)
            return;

        Member m = g.getMemberById(id);

        if(m != null) {
            if(!StaticStore.rolesToString(m.getRoles()).contains(holder.BOOSTER)) {
                createMessageWithNoPings(ch, LangID.getStringByID("boorole_noboost", lang).replace("_RRR_", holder.BOOSTER));
                return;
            }

            if(StaticStore.boosterData.containsKey(g.getId())) {
                BoosterHolder bHolder = StaticStore.boosterData.get(g.getId());

                if(bHolder.serverBooster.containsKey(m.getId())) {
                    BoosterData data = bHolder.serverBooster.get(m.getId());

                    if(data.getRole() != null) {
                        createMessageWithNoPings(ch, LangID.getStringByID("boorole_already", lang));
                        return;
                    }
                }
            }

            if (g.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                g.createRole()
                        .setHoisted(false)
                        .setColor(c)
                        .setName(name)
                        .setMentionable(false)
                        .setPermissions(Permission.EMPTY_PERMISSIONS)
                        .queue(r -> {
                            g.modifyRolePositions().selectPosition(r).moveTo(getPackPackPosition(g) - 1).queue(null, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/BoosterRole - Failed to move role");

                                ch.sendMessage(LangID.getStringByID("boorole_failmove", lang)).queue();
                            });

                            if(StaticStore.boosterData.containsKey(g.getId())) {
                                BoosterHolder bHolder = StaticStore.boosterData.get(g.getId());

                                if(bHolder.serverBooster.containsKey(m.getId())) {
                                    BoosterData data = bHolder.serverBooster.get(m.getId());

                                    int result = data.setRole(r.getId());

                                    if(result == BoosterData.ERR_ALREADY_ROLE_SET) {
                                        createMessageWithNoPings(ch, LangID.getStringByID("boorole_already", lang));
                                    } else {
                                        g.addRoleToMember(UserSnowflake.fromId(m.getId()), r).queue(null, e -> {
                                            StaticStore.logger.uploadErrorLog(e, "E/BoosterRole - Error happened while trying to assign role to member");

                                            createMessageWithNoPings(ch, "Error happened while trying to assign role to member...");
                                        });

                                        createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId()).replace("_MMM_", m.getId()));
                                    }
                                } else {
                                    BoosterData data = new BoosterData(r.getId(), BoosterData.INITIAL.ROLE);

                                    g.addRoleToMember(UserSnowflake.fromId(m.getId()), r).queue(null, e -> {
                                        StaticStore.logger.uploadErrorLog(e, "E/BoosterRole - Error happened while trying to assign role to member");

                                        createMessageWithNoPings(ch, "Error happened while trying to assign role to member...");
                                    });

                                    bHolder.serverBooster.put(m.getId(), data);

                                    createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId()).replace("_MMM_", m.getId()));
                                }
                            } else {
                                BoosterHolder bHolder = new BoosterHolder();

                                BoosterData data = new BoosterData(r.getId(), BoosterData.INITIAL.ROLE);

                                g.addRoleToMember(UserSnowflake.fromId(m.getId()), r).queue(null, e -> {
                                    StaticStore.logger.uploadErrorLog(e, "E/BoosterRole - Error happened while trying to assign role to member");

                                    createMessageWithNoPings(ch, "Error happened while trying to assign role to member...");
                                });

                                bHolder.serverBooster.put(m.getId(), data);

                                StaticStore.boosterData.put(g.getId(), bHolder);

                                createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId()).replace("_MMM_", m.getId()));
                            }
                        }, e -> StaticStore.logger.uploadErrorLog(e, "E/BoosterRole - Error happened while trying to create role"));
            } else {
                createMessageWithNoPings(ch, LangID.getStringByID("boorole_invmem", lang));
            }
        }


    }

    private String getUserID(String message) {
        String[] content = message.split(" ");

        if(content.length < 2)
            return null;

        String id = content[1].replaceAll("<@!?", "").replace(">", "");

        if(StaticStore.isNumeric(id))
            return id;
        else
            return null;
    }

    private int getColor(String message, MessageChannel ch) {
        String[] content = message.split(" ");

        int[] rgb = null;
        String hex = null;

        boolean he = false;
        boolean re = false;
        boolean gr = false;
        boolean bl = false;

        for(int i = 0; i < content.length; i++) {
            if(!he && (content[i].equals("-h") || content[i].equals("-hex")) && i < content.length - 1) {
                String h = content[i+1];

                if(h.startsWith("#"))
                    h = h.substring(1);

                if(isHex(h) && h.length() == 6) {
                    hex = h;
                    he = true;
                } else {
                    createMessageWithNoPings(ch, LangID.getStringByID("boorole_invhex", lang).replace("_", h));

                    return -1;
                }
            } else if(!re && (content[i].equals("-r") || content[i].equals("-red")) && i < content.length - 1 && StaticStore.isNumeric(content[i+1])) {
                if(rgb == null)
                    rgb = new int[3];

                rgb[0] = Math.max(0, Math.min(255, StaticStore.safeParseInt(content[i+1])));

                re = true;
            } else if(!gr && (content[i].equals("-g") || content[i].equals("-green")) && i < content.length - 1 && StaticStore.isNumeric(content[i + 1])) {
                if(rgb == null)
                    rgb = new int[3];

                rgb[1] = Math.max(0, Math.min(255, StaticStore.safeParseInt(content[i + 1])));

                gr = true;
            } else if(!bl && (content[i].equals("-b") || content[i].equals("-blue")) && i < content.length - 1 && StaticStore.isNumeric(content[i + 1])) {
                if(rgb == null)
                    rgb = new int[3];

                rgb[2] = Math.max(0, Math.min(255, StaticStore.safeParseInt(content[i + 1])));

                bl = true;
            }
        }

        if(hex == null && rgb == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_setcolor", lang));

            return -1;
        }

        if(hex != null) {
            return Integer.parseInt(hex, 16);
        }

        if(re && gr && bl) {
            return new Color(rgb[0], rgb[1], rgb[2]).getRGB();
        } else {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_fullrgb", lang));

            return -1;
        }
    }

    private String getRoleName(String message) {
        if(StringUtils.countMatches(message, '"') >= 2) {
            Matcher m = p.matcher(message);

            boolean f = m.find();

            if(!f)
                return null;
            else {
                String g = m.group();

                return g.substring(1, g.length() - 1);
            }
        }

        return null;
    }

    private boolean isHex(String hex) {
        hex = hex.toLowerCase(Locale.ROOT);

        for(int i = 0; i < hex.length(); i++) {
            if (!hexString.contains(Character.toString(hex.charAt(i)))) {
                return false;
            }
        }

        return true;
    }

    private int getPackPackPosition(Guild g) {
        int role = 0;

        List<Role> roles = g.getSelfMember().getRoles();

        for(Role r : roles) {
            int pos = r.getPosition();

            role = Math.max(role, pos);
        }

        return role;
    }
}

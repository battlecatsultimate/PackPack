package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.PermissionSet;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterRole extends ConstraintCommand {
    private final static String hexString = "0123456789abcdef";
    private final static Pattern p = Pattern.compile("\".+\"");

    public BoosterRole(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null)
            return;

        IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

        if(holder.BOOSTER == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_norole", lang));
            return;
        }

        String id = getUserID(getContent(event));

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_invalidid", lang));
            return;
        }

        String name = getRoleName(getContent(event));

        if(name == null || name.isBlank()) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_norolename", lang));
            return;
        }

        Color c = getColor(getContent(event), ch);

        if(c == null) {
            return;
        }

        g.getMemberById(Snowflake.of(id)).subscribe(m -> {
            if(!m.getRoleIds().contains(Snowflake.of(holder.BOOSTER))) {
                createMessageWithNoPings(ch, LangID.getStringByID("boorole_noboost", lang).replace("_RRR_", holder.BOOSTER));
                return;
            }

            if(StaticStore.boosterData.containsKey(g.getId().asString())) {
                BoosterHolder bHolder = StaticStore.boosterData.get(g.getId().asString());

                if(bHolder.serverBooster.containsKey(m.getId().asString())) {
                    BoosterData data = bHolder.serverBooster.get(m.getId().asString());

                    if(data.getRole() != null) {
                        createMessageWithNoPings(ch, LangID.getStringByID("boorole_already", lang));
                        return;
                    }
                }
            }

            g.createRole(r -> {
                r.setHoist(false);
                r.setColor(c);
                r.setName(name);
                r.setMentionable(false);
                r.setPermissions(PermissionSet.none());
            }).subscribe(r -> {
                r.changePosition(getPackPackPosition(g)-1).subscribe(null, e -> createMessageWithNoPings(ch, LangID.getStringByID("boorole_failmove", lang)));

                if(StaticStore.boosterData.containsKey(g.getId().asString())) {
                    BoosterHolder bHolder = StaticStore.boosterData.get(g.getId().asString());

                    if(bHolder.serverBooster.containsKey(m.getId().asString())) {
                        BoosterData data = bHolder.serverBooster.get(m.getId().asString());

                        int result = data.setRole(r.getId().asString());

                        if(result == BoosterData.ERR_ALREADY_ROLE_SET) {
                            createMessageWithNoPings(ch, LangID.getStringByID("boorole_already", lang));
                        } else {
                            m.addRole(r.getId()).subscribe(null, e -> {
                                e.printStackTrace();

                                createMessageWithNoPings(ch, "Something is wong...");
                            });
                            createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId().asString()).replace("_MMM_", m.getId().asString()));
                        }
                    } else {
                        BoosterData data = new BoosterData(r.getId().asString(), BoosterData.INITIAL.ROLE);

                        m.addRole(r.getId()).subscribe(null, e -> {
                            e.printStackTrace();

                            createMessageWithNoPings(ch, "Something is wong...");
                        });

                        bHolder.serverBooster.put(m.getId().asString(), data);

                        createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId().asString()).replace("_MMM_", m.getId().asString()));
                    }
                } else {
                    BoosterHolder bHolder = new BoosterHolder();

                    BoosterData data = new BoosterData(r.getId().asString(), BoosterData.INITIAL.ROLE);

                    m.addRole(r.getId()).subscribe();

                    bHolder.serverBooster.put(m.getId().asString(), data);

                    StaticStore.boosterData.put(g.getId().asString(), bHolder);

                    createMessageWithNoPings(ch, LangID.getStringByID("boorole_success", lang).replace("_RRR_", r.getId().asString()).replace("_MMM_", m.getId().asString()));
                }
            }, e -> {
                e.printStackTrace();

                createMessageWithNoPings(ch, LangID.getStringByID("boorole_invmem", lang));
            });
        });


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

    private Color getColor(String message, MessageChannel ch) {
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
                    return null;
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
            return null;
        }

        if(hex != null) {
            return Color.of(Integer.parseInt(hex, 16));
        }

        if(re && gr && bl) {
            return Color.of(rgb[0], rgb[1], rgb[2]);
        } else {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_fullrgb", lang));
            return null;
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
        Member packpack = StaticStore.getPackPack(g);

        int role = 0;

        List<Role> roles = packpack.getRoles().collectList().block();

        if(roles != null) {
            for(Role r : roles) {
                Integer pos = r.getPosition().block();

                if(pos != null) {
                    role = Math.max(role, pos);
                }
            }
        }

        System.out.println(role);

        return role;
    }
}

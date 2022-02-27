package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.InteractionData;
import discord4j.discordjson.json.MemberData;
import mandarin.packpack.commands.Command;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.slash.SlashBuilder;
import mandarin.packpack.supporter.server.slash.WebhookBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Background extends TimedConstraintCommand {
    public static WebhookBuilder getInteractionWebhook(InteractionData interaction, common.util.pack.Background bg) throws Exception {
        int lang = LangID.EN;

        if(!interaction.guildId().isAbsent()) {
            String gID = interaction.guildId().get();

            if(gID.equals(StaticStore.BCU_KR_SERVER))
                lang = LangID.KR;
        }

        if(!interaction.member().isAbsent()) {
            MemberData m = interaction.member().get();

            if(StaticStore.locales.containsKey(m.user().id().asString())) {
                lang =  StaticStore.locales.get(m.user().id().asString());
            }
        }

        File img = ImageDrawing.drawBGImage(bg, 960, 520, false);

        if(img != null) {
            FileInputStream fis = new FileInputStream(img);

            int finalLang = lang;

            return SlashBuilder.getWebhookRequest(w -> {
                w.setContent(LangID.getStringByID("bg_result", finalLang).replace("_", Data.trio(bg.id.id)).replace("WWW", 960+"").replace("HHH", 520+""));
                w.addFile("bg.png", fis, img);
            });
        }

        return null;
    }

    private common.util.pack.Background bg;

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID);
    }

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time, common.util.pack.Background bg) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID);

        this.bg = bg;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event).block();

        if(ch == null || g == null)
            return;

        if(bg != null) {
            File img = ImageDrawing.drawBGImage(bg, 960, 520, false);

            if(img != null) {
                FileInputStream fis = new FileInputStream(img);

                createMessage(ch, ms -> {
                    ms.content(LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", 960+"").replace("HHH", 520+""));
                    ms.addFile("bg.png", fis);
                }, () -> {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(img.exists()) {
                        boolean res = img.delete();

                        if(!res) {
                            System.out.println("Can't delete file : "+img.getAbsolutePath());
                        }
                    }
                });
            }
        } else {
            String[] msg = getContent(event).split(" ");

            if(msg.length == 1) {
                ch.createMessage(LangID.getStringByID("bg_more", lang)).subscribe();
            } else {
                int id = getID(getContent(event));

                if(id == -1) {
                    ch.createMessage(LangID.getStringByID("bg_more", lang)).subscribe();
                    return;
                }

                common.util.pack.Background bg = UserProfile.getBCData().bgs.getRaw(id);

                if(bg == null) {
                    int[] size = getBGSize();

                    ch.createMessage(LangID.getStringByID("bg_invalid", lang).replace("_", size[0]+"").replace("-", size[1] + "")).subscribe();
                    return;
                }

                Optional<Member> om = getMember(event);

                if(om.isEmpty())
                    return;

                Member m = om.get();

                int w = Math.max(1, getWidth(getContent(event)));
                int h = Math.max(1, getHeight(getContent(event)));
                boolean eff = drawEffect(getContent(event));
                boolean anim = generateAnim(getContent(event));
                boolean isTrusted = StaticStore.contributors.contains(m.getId().asString());

                String cache = StaticStore.imgur.get("BG - "+Data.trio(bg.id.id), false, true);

                if(anim && bg.effect != -1 && cache == null && isTrusted) {
                    if(!EntityHandler.generateBGAnim(ch, g.getPremiumTier().getValue(), bg, lang)) {
                        StaticStore.logger.uploadLog("W/Background | Failed to generate bg effect animation");
                    }
                } else {
                    if(anim && bg.effect != -1) {
                        if(cache != null) {
                            Command.createMessage(ch, ms -> ms.content(LangID.getStringByID("gif_cache", lang).replace("_", cache)));
                            return;
                        } else {
                            createMessage(ch, ms -> ms.content(LangID.getStringByID("bg_ignore", lang)));
                        }
                    }

                    if(eff && bg.effect != -1)
                        w = w * 5 / 6;

                    File img = ImageDrawing.drawBGImage(bg, w, h, eff);

                    if(img != null) {
                        FileInputStream fis = new FileInputStream(img);

                        int finalW = w;

                        createMessage(ch, ms -> {
                            ms.content(LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", finalW +"").replace("HHH", h+""));
                            ms.addFile("bg.png", fis);
                        }, () -> {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if(img.exists()) {
                                boolean res = img.delete();

                                if(!res) {
                                    System.out.println("Can't delete file : "+img.getAbsolutePath());
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private int getWidth(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return 960;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-w") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(1920, StaticStore.safeParseInt(contents[i+1]));
            }
        }

        return 960;
    }

    private int getHeight(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return 520;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-h") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(1080, StaticStore.safeParseInt(contents[i+1]));
            }
        }

        return 520;
    }

    private boolean drawEffect(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return false;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-e") || contents[i].equals("-effect")) {
                return true;
            }
        }

        return false;
    }

    private boolean generateAnim(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return false;

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-a") || contents[i].equals("-anim")) {
                return true;
            }
        }

        return false;
    }

    private int getID(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return -1;

        boolean height = false;
        boolean width = false;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].equals("-w") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                if(!width) {
                    i++;
                    width = true;
                }
            } else if(contents[i].equals("-h") && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                if(!height) {
                    i++;
                    height = true;
                }
            } else if(StaticStore.isNumeric(contents[i])) {
                return StaticStore.safeParseInt(contents[i]);
            }
        }

        return -1;
    }

    private int[] getBGSize() {
        List<common.util.pack.Background> bgs = UserProfile.getBCData().bgs.getList();

        int[] res = {0, 0};

        for(int i = 0; i < bgs.size(); i++) {
            if(bgs.get(i).id.id < 1000)
                res[0] = Math.max(res[0], bgs.get(i).id.id);
            else
                res[1] = Math.max(res[1], bgs.get(i).id.id);
        }

        return res;
    }
}

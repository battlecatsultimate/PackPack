package mandarin.packpack.commands.bc;

import common.pack.UserProfile;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Background extends TimedConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, common.util.pack.Background bg) throws Exception {
        Interaction interaction = event.getInteraction();

        int lang = LangID.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang =  StaticStore.config.get(u.getId()).lang;
        }

        File img = ImageDrawing.drawBGImage(bg, 960, 520, false);

        if(img != null) {
            event.deferReply()
                    .setAllowedMentions(new ArrayList<>())
                    .setContent(LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", 960+"").replace("HHH", 520+""))
                    .addFiles(FileUpload.fromData(img, "bg.png"))
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Background::performButton - Failed to upload bg image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    private common.util.pack.Background bg;

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID, false);
    }

    public Background(ConstraintCommand.ROLE role, int lang, IDHolder id, long time, common.util.pack.Background bg) {
        super(role, lang, id, time, StaticStore.COMMAND_BG_ID, false);

        this.bg = bg;
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        if(bg != null) {
            File img = ImageDrawing.drawBGImage(bg, 960, 520, false);

            if(img != null) {
                sendMessageWithFile(ch, LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", 960+"").replace("HHH", 520+""), img, "bg.png", getMessage(event));
            }
        } else {
            String[] msg = getContent(event).split(" ");

            if(msg.length == 1) {
                replyToMessageSafely(ch, LangID.getStringByID("bg_more", lang), getMessage(event), a -> a);
            } else {
                int id = getID(getContent(event));

                if(id == -1) {
                    replyToMessageSafely(ch, LangID.getStringByID("bg_more", lang), getMessage(event), a -> a);

                    return;
                }

                common.util.pack.Background bg = UserProfile.getBCData().bgs.getRaw(id);

                if(bg == null) {
                    int[] size = getBGSize();

                    replyToMessageSafely(ch, LangID.getStringByID("bg_invalid", lang).replace("_", size[0]+"").replace("-", size[1] + ""), getMessage(event), a -> a);
                    return;
                }

                User u = getUser(event);

                if(u == null)
                    return;

                int w = Math.max(1, getWidth(getContent(event)));
                int h = Math.max(1, getHeight(getContent(event)));
                boolean eff = drawEffect(getContent(event));
                boolean anim = generateAnim(getContent(event));
                boolean isTrusted = StaticStore.contributors.contains(u.getId());

                String cache = StaticStore.imgur.get("BG - "+Data.trio(bg.id.id), false, true);

                if(anim && bg.effect != -1 && cache == null && isTrusted) {
                    if(!EntityHandler.generateBGAnim(ch, getMessage(event), bg, lang)) {
                        StaticStore.logger.uploadLog("W/Background | Failed to generate bg effect animation");
                    }
                } else {
                    if(anim && bg.effect != -1) {
                        if(cache != null) {
                            replyToMessageSafely(ch, LangID.getStringByID("gif_cache", lang).replace("_", cache), getMessage(event), a -> a);

                            return;
                        } else {
                            ch.sendMessage(LangID.getStringByID("bg_ignore", lang)).queue();
                        }
                    }

                    if(eff && bg.effect != -1)
                        w = w * 5 / 6;

                    File img = ImageDrawing.drawBGImage(bg, w, h, eff);

                    if(img != null) {
                        sendMessageWithFile(ch, LangID.getStringByID("bg_result", lang).replace("_", Data.trio(bg.id.id)).replace("WWW", w +"").replace("HHH", h+""), img, "bg.png", getMessage(event));
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

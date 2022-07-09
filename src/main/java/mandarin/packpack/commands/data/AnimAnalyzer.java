package mandarin.packpack.commands.data;

import common.system.files.VFile;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.AnimMessageHolder;
import mandarin.packpack.supporter.server.holder.BCAnimMessageHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnimAnalyzer extends ConstraintCommand {
    private static final int PARAM_DEBUG = 2;
    private static final int PARAM_RAW = 4;
    private static final int PARAM_BC = 8;
    private static final int PARAM_ZOMBIE = 16;
    private static final int PARAM_TRANSPARENT = 32;
    private static final int PARAM_USEAPK = 64;

    public AnimAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        int param = checkParam(getContent(event));

        boolean debug = (PARAM_DEBUG & param) > 0;
        boolean raw = (PARAM_RAW & param) > 0;
        boolean bc = (PARAM_BC & param) > 0;
        boolean zombie = (PARAM_ZOMBIE & param) > 0;
        boolean transparent = (PARAM_TRANSPARENT & param) > 0;
        boolean apk = (PARAM_USEAPK & param) > 0;

        if(apk) {
            String animCode = getAnimCode(getContent(event));

            if(animCode == null) {
                ch.sendMessage("Please specify file code such as `000_f`, `001_m`, etc.").queue();

                return;
            }

            String localeCode;

            switch (getLocale(getContent(event))) {
                case LangID.EN:
                    localeCode = "en";
                    break;
                case LangID.ZH:
                    localeCode = "zh";
                    break;
                case LangID.KR:
                    localeCode = "kr";
                    break;
                default:
                    localeCode = "jp";
            }

            File workspace = new File("./data/bc/"+localeCode+"/workspace");

            if(!workspace.exists()) {
                ch.sendMessage("Bot couldn't find analyzed apk file data for locale "+localeCode+". Please call `p!da [Locale]` first").queue();

                return;
            }

            if(!validateFiles(animCode, workspace, zombie)) {
                ch.sendMessage("Workspace folder contains no animation data with such code, try `p!da [Locale]`").queue();

                return;
            }

            AnimMixer mixer = new AnimMixer(zombie ? 7 : 4);

            VFile imgcut = VFile.getFile(new File(workspace, "ImageDataLocal/"+animCode+".imgcut"));

            if(imgcut == null) {
                ch.sendMessage("Couldn't find imgcut file").queue();

                return;
            }

            mixer.imgCut = ImgCut.newIns(imgcut.getData());

            VFile mamodel = VFile.getFile(new File(workspace, "ImageDataLocal/"+animCode+".mamodel"));

            if(mamodel == null) {
                ch.sendMessage("Couldn't find mamodel file").queue();

                return;
            }

            mixer.model = MaModel.newIns(mamodel.getData());

            List<File> maanims = gatherMaanims(new File(workspace, "ImageDataLocal"), animCode, zombie);

            for(int i = 0; i < maanims.size(); i++) {
                VFile maanim = VFile.getFile(maanims.get(i));

                if(maanim == null) {
                    ch.sendMessage("Couldn't find maanim file : "+maanims.get(i).getName()).queue();

                    return;
                }

                mixer.anim[i] = MaAnim.newIns(maanim.getData());
            }

            mixer.png = ImageIO.read(new File(workspace, "NumberLocal/"+animCode+".png"));

            EntityHandler.generateBCAnim(ch, g.getBoostTier().getKey(), mixer, lang);
        } else {
            int anim = getAnimNumber(getContent(event));

            if(bc)
                anim = zombie ? 7 : 4;

            StringBuilder message = new StringBuilder("PNG : -\nIMGCUT : -\nMAMODEL : -\n");

            if(bc) {
                for(int i = 0; i < anim; i++) {
                    message.append(getMaanimTitle(i)).append("-");

                    if(i < anim - 1)
                        message.append("\n");
                }
            } else {
                for(int i = 0; i < anim; i++) {
                    message.append("MAANIM ").append(i).append(" : -");

                    if(i < anim - 1)
                        message.append("\n");
                }
            }

            Message m = ch.sendMessage(message.toString()).complete();

            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            File container = StaticStore.generateTempFile(temp, "anim", "", true);

            if(container == null) {
                return;
            }

            Message msg = getMessage(event);

            if(msg != null)
                if(bc) {
                    new BCAnimMessageHolder(msg, m, lang, ch.getId(), container, ch, zombie);
                } else {
                    new AnimMessageHolder(msg, m, lang, ch.getId(), container, debug, ch, raw, transparent, anim);
                }
        }
    }

    private int checkParam(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for (String s : pureMessage) {
                switch (s) {
                    case "-r":
                    case "-raw":
                        if ((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
                        } else {
                            break label;
                        }
                        break;
                    case "-d":
                    case "-debug":
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                    case "-bc":
                    case "-b":
                        if((result & PARAM_BC) == 0) {
                            result |= PARAM_BC;
                        } else {
                            break label;
                        }
                        break;
                    case "-zombie":
                    case "-z":
                        if((result & PARAM_ZOMBIE) == 0) {
                            result |= PARAM_ZOMBIE;
                        } else {
                            break label;
                        }
                        break;
                    case "-t":
                        if((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                        break;
                    case "-apk":
                    case "-a":
                        if((result & PARAM_USEAPK) == 0) {
                            result |= PARAM_USEAPK;
                        } else {
                            break label;
                        }
                }
            }
        }

        return result;
    }

    private int getAnimNumber(String message) {
        String[] contents = message.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-anim") || (contents[i].equals("-a"))) && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                return Math.min(5, Math.max(1, StaticStore.safeParseInt(contents[i + 1])));
            }
        }

        return 1;
    }

    private String getMaanimTitle(int index) {
        switch (index) {
            case 0:
                return "MAANIM WALKING : ";
            case 1:
                return "MAANIM IDLE : ";
            case 2:
                return "MAANIM ATTACK : ";
            case 3:
                return "MAANIM HITBACK : ";
            case 4:
                return "MAANIM BURROW DOWN : ";
            case 5:
                return "MAANIM BURROW MOVE : ";
            case 6:
                return "MAANIM BURROW UP : ";
            default:
                return "MAANIM "+index+" : ";
        }
    }

    private String getAnimCode(String content) {
        String[] contents = content.split(" ");

        if(contents.length < 2)
            return null;

        for(int i = 1; i < contents.length; i++) {
            if(contents[i].startsWith("-"))
                continue;

            return contents[i];
        }

        return null;
    }

    private int getLocale(String content) {
        if(content.contains("-tw"))
            return LangID.ZH;
        else if(content.contains("-en"))
            return LangID.EN;
        else if(content.contains("-kr"))
            return LangID.KR;
        else
            return LangID.JP;
    }

    private boolean validateFiles(String code, File workspace, boolean zombie) {
        File numberLocal = new File(workspace, "NumberLocal");
        File imageDataLocal = new File(workspace, "ImageDataLocal");

        if(!numberLocal.exists() || !imageDataLocal.exists()) {
            return false;
        }

        File sprite = new File(numberLocal, code+".png");

        if(!sprite.exists()) {
            return false;
        }

        String[] maanims;

        if(zombie) {
            maanims = new String[] {"00", "01", "02", "03", "_zombie00", "_zombie01", "_zombie02"};
        } else {
            maanims = new String[] {"00", "01", "02", "03"};
        }

        for(int i = 0; i < maanims.length; i++) {
            File f = new File(imageDataLocal, code+maanims[i]+".maanim");

            if(!f.exists()) {
                return false;
            }
        }

        return true;
    }

    private List<File> gatherMaanims(File imageData, String code, boolean zombie) {
        List<File> result = new ArrayList<>();

        String[] maanims;

        if(zombie) {
            maanims = new String[] {"00", "01", "02", "03", "_zombie00", "_zombie01", "_zombie02"};
        } else {
            maanims = new String[] {"00", "01", "02", "03"};
        }

        for(int i = 0; i < maanims.length; i++) {
            result.add(new File(imageData, code+maanims[i]+".maanim"));
        }

        return result;
    }
}

package mandarin.packpack.commands.data;

import common.CommonStatic;
import common.system.files.VFile;
import common.util.anim.ImgCut;
import common.util.anim.MaAnim;
import common.util.anim.MaModel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.message.AnimMessageHolder;
import mandarin.packpack.supporter.server.holder.message.BCAnimMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnimAnalyzer extends ConstraintCommand {
    private static final int PARAM_DEBUG = 2;
    private static final int PARAM_RAW = 4;
    private static final int PARAM_BC = 8;
    private static final int PARAM_ZOMBIE = 16;
    private static final int PARAM_USEAPK = 32;
    private static final int PARAM_PERFORMANCE = 64;

    public AnimAnalyzer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        int param = checkParam(loader.getContent());

        boolean debug = (PARAM_DEBUG & param) > 0;
        boolean raw = (PARAM_RAW & param) > 0;
        boolean bc = (PARAM_BC & param) > 0;
        boolean zombie = (PARAM_ZOMBIE & param) > 0;
        boolean apk = (PARAM_USEAPK & param) > 0;
        boolean performance = (PARAM_PERFORMANCE & param) > 0;

        if(apk) {
            String animCode = getAnimCode(loader.getContent());

            if(animCode == null) {
                ch.sendMessage("Please specify file code such as `000_f`, `001_m`, etc.").queue();

                return;
            }

            String localeCode = switch (getLocale(loader.getContent())) {
                case EN -> "en";
                case ZH -> "zh";
                case KR -> "kr";
                default -> "jp";
            };

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

                mixer.anim[i] = MaAnim.newIns(maanim.getData(), false);
            }

            mixer.buildPng(new File(workspace, "NumberLocal/"+animCode+".png"));

            int boostLevel = 0;

            if (ch instanceof GuildChannel) {
                boostLevel = loader.getGuild().getBoostTier().getKey();
            }

            EntityHandler.generateBCAnim(ch, boostLevel, mixer, performance, lang, () -> { }, () -> { });
        } else {
            int anim = getAnimNumber(loader.getContent());

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

            int finalAnim = anim;

            ch.sendMessage(message.toString()).queue(m -> {
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

                Message msg = loader.getMessage();

                try {
                    if(bc) {
                        new BCAnimMessageHolder(msg, loader.getUser().getId(), ch.getId(), m, performance, lang, container, ch, zombie);
                    } else {
                        new AnimMessageHolder(msg, loader.getUser().getId(), ch.getId(), m, lang, container, performance, debug, ch, raw, finalAnim);
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/AnimAnalyzer::doSomething - Failed to initialize holder");
                }
            });
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
                    case "-r", "-raw" -> {
                        if ((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
                        } else {
                            break label;
                        }
                    }
                    case "-d", "-debug" -> {
                        if ((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                    }
                    case "-bc", "-b" -> {
                        if ((result & PARAM_BC) == 0) {
                            result |= PARAM_BC;
                        } else {
                            break label;
                        }
                    }
                    case "-zombie", "-z" -> {
                        if ((result & PARAM_ZOMBIE) == 0) {
                            result |= PARAM_ZOMBIE;
                        } else {
                            break label;
                        }
                    }
                    case "-apk", "-a" -> {
                        if ((result & PARAM_USEAPK) == 0) {
                            result |= PARAM_USEAPK;
                        } else {
                            break label;
                        }
                    }
                    case "-p", "-pf", "-performance" -> {
                        if ((result & PARAM_PERFORMANCE) == 0) {
                            result |= PARAM_PERFORMANCE;
                        } else {
                            break label;
                        }
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
        return switch (index) {
            case 0 -> "MAANIM WALKING : ";
            case 1 -> "MAANIM IDLE : ";
            case 2 -> "MAANIM ATTACK : ";
            case 3 -> "MAANIM HITBACK : ";
            case 4 -> "MAANIM BURROW DOWN : ";
            case 5 -> "MAANIM BURROW MOVE : ";
            case 6 -> "MAANIM BURROW UP : ";
            default -> "MAANIM " + index + " : ";
        };
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

    private CommonStatic.Lang.Locale getLocale(String content) {
        if(content.contains("-tw"))
            return CommonStatic.Lang.Locale.ZH;
        else if(content.contains("-en"))
            return CommonStatic.Lang.Locale.EN;
        else if(content.contains("-kr"))
            return CommonStatic.Lang.Locale.KR;
        else
            return CommonStatic.Lang.Locale.JP;
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

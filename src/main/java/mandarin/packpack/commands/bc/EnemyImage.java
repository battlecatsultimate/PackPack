package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.EnemyAnimMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EnemyImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public EnemyImage(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_ENEMYIMAGE_ID, false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            String search = filterCommand(loader.getContent());

            if(search.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("enemyImage.fail.noParameter", lang), loader.getMessage(), a -> a);

                return;
            }

            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

            if(enemies.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("enemyStat.fail.noEnemy", lang).replace("_", getSearchKeyword(loader.getContent())), loader.getMessage(), a -> a);

                disableTimer();
            } else if(enemies.size() == 1) {
                int param = checkParameters(loader.getContent());
                int mode = getMode(loader.getContent());
                int frame = getFrame(loader.getContent());

                enemies.getFirst().anim.load();

                if(mode >= enemies.getFirst().anim.anims.length)
                    mode = 0;

                EAnimD<?> anim = enemies.getFirst().getEAnim(ImageDrawing.getAnimType(mode, enemies.getFirst().anim.anims.length));

                File img = ImageDrawing.drawAnimImage(anim, frame, 1f, (param & PARAM_TRANSPARENT) > 0, (param & PARAM_DEBUG) > 0);

                enemies.getFirst().anim.unload();

                if(img != null) {
                    String fName = MultiLangCont.get(enemies.getFirst(), lang);

                    if(fName == null || fName.isBlank())
                        fName = enemies.getFirst().names.toString();

                    if(fName.isBlank())
                        fName = LangID.getStringByID("data.stage.enemy", lang)+" "+ Data.trio(Objects.requireNonNull(enemies.getFirst().id).id);

                    sendMessageWithFile(
                            ch,
                            LangID.getStringByID("formImage.result", lang).replace("_", fName).replace(":::", getModeName(mode, enemies.getFirst().anim.anims.length)).replace("=", String.valueOf(frame)),
                            img,
                            loader.getMessage()
                    );
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(loader.getContent())));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(enemies);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                    if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), enemies.size(), data, lang).queue(res -> {
                    int param = checkParameters(loader.getContent());
                    int mode = getMode(loader.getContent());
                    int frame = getFrame(loader.getContent());

                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new EnemyAnimMessageHolder(enemies, msg, u.getId(), ch.getId(), res, mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang, false, false, false));
                });

                disableTimer();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("enemyImage.fail.noParameter", lang)).queue();
            disableTimer();
        }
    }

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("data.animation.mode.walk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("data.animation.mode.idle", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("data.animation.mode.attack", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("data.animation.mode.kb", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("data.animation.mode.enter", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowDown", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowMove", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("data.animation.mode.burrowUp", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 6;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getFrame(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-f") || msg[i].equals("-fr")) {
                if(i < msg.length - 1 && StaticStore.isNumeric(msg[i+1])) {
                    return StaticStore.safeParseInt(msg[i+1]);
                }
            }
        }

        return 0;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-t" -> {
                        if ((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
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
                    case "-f", "-fr" -> {
                        if (i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i + 1])) {
                            i++;
                        } else {
                            break label;
                        }
                    }
                    case "-m", "-mode" -> {
                        if (i < pureMessage.length - 1) {
                            i++;
                        } else {
                            break label;
                        }
                    }
                }
            }
        }

        return result;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean debug = false;
        boolean trans = false;

        boolean mode = false;
        boolean frame = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-t" -> {
                    if (!trans) {
                        trans = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-d", "-debug" -> {
                    if (!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-m", "-mode" -> {
                    if (!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-f", "-fr" -> {
                    if (!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                default -> {
                    result.append(contents[i]);
                    written = true;
                }
            }

            if(written && i < contents.length - 1) {
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private String getModeName(int mode, int max) {
        switch (mode) {
            case 1 -> {
                return LangID.getStringByID("data.animation.mode.idle", lang);
            }
            case 2 -> {
                return LangID.getStringByID("data.animation.mode.attack", lang);
            }
            case 3 -> {
                return LangID.getStringByID("formImage.mode.kb", lang);
            }
            case 4 -> {
                if (max == 5)
                    return LangID.getStringByID("data.animation.mode.enter", lang);
                else
                    return LangID.getStringByID("formImage.mode.burrowDown", lang);
            }
            case 5 -> {
                return LangID.getStringByID("formImage.mode.burrowMove", lang);
            }
            case 6 -> {
                return LangID.getStringByID("formImage.mode.burrowUp", lang);
            }
            default -> {
                return LangID.getStringByID("data.animation.mode.walk", lang);
            }
        }
    }

    private List<String> accumulateData(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename;

            if(e.id != null) {
                ename = Data.trio(e.id.id)+" ";
            } else {
                ename = " ";
            }

            String name = StaticStore.safeMultiLangGet(e, lang);

            if(name != null)
                ename += name;

            data.add(ename);
        }

        return data;
    }

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if(result == null)
            return "";

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

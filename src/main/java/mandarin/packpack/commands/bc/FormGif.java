package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.GlobalTimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.FormAnimMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
public class FormGif extends GlobalTimedConstraintCommand {
    private static final int PARAM_DEBUG = 2;
    private static final int PARAM_RAW = 4;
    private static final int PARAM_GIF = 8;

    public static List<Integer> forbidden = new ArrayList<>();

    static {
        int[] data = {

        };

        for(int d : data)
            forbidden.add(d);
    }

    public FormGif(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(30), false);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    protected void doThing(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        if(ch == null)
            return;

        User u = loader.getUser();

        if(u == null)
            return;

        boolean isTrusted = StaticStore.contributors.contains(u.getId()) || u.getId().equals(StaticStore.MANDARIN_SMELL);

        String[] list = loader.getContent().split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    disableTimer();
                    return;
                }
            }

            String search = filterCommand(loader.getContent());

            if(search.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("formImage.fail.noParameter", lang), loader.getMessage(), a -> a);
                disableTimer();
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search, false, lang);

            if(forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formStat.fail.noUnit", lang).replace("_", getSearchKeyword(loader.getContent())), loader.getMessage(), a -> a);
                disableTimer();
            } else if(forms.size() == 1) {
                int param = checkParameters(loader.getContent());
                int mode = getMode(loader.getContent());
                boolean debug = (param & PARAM_DEBUG) > 0;
                boolean raw = (param & PARAM_RAW) > 0;
                boolean gif = (param & PARAM_GIF) > 0;
                int frame = getFrame(loader.getContent());

                Form f = forms.getFirst();

                if(forbidden.contains(f.unit.id.id)) {
                    replyToMessageSafely(ch, LangID.getStringByID("data.animation.gif.dummy", lang), loader.getMessage(), a -> a);

                    return;
                }

                if(raw && !isTrusted) {
                    ch.sendMessage(LangID.getStringByID("data.animation.gif.ignoring", lang)).queue();
                }

                int boostLevel = 0;

                if (ch instanceof GuildChannel) {
                    boostLevel = loader.getGuild().getBoostTier().getKey();
                }

                EntityHandler.generateFormAnim(f, ch, loader.getMessage(), boostLevel, mode, debug, frame, lang, raw && isTrusted, gif, () -> {
                    if(!StaticStore.conflictedAnimation.isEmpty()) {
                        StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + loader.getContent());
                        StaticStore.conflictedAnimation.clear();
                    }

                    if(raw && isTrusted) {
                        StaticStore.logger.uploadLog("Generated mp4 by user " + u.getName() + " for unit ID " + Data.trio(f.unit.id.id) + " with mode of " + mode);
                    }

                    if(raw && isTrusted) {
                        changeTime(TimeUnit.MINUTES.toMillis(1));
                    }
                }, () -> {
                    if(!StaticStore.conflictedAnimation.isEmpty()) {
                        StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + loader.getContent());
                        StaticStore.conflictedAnimation.clear();
                    }

                    disableTimer();
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(loader.getContent())));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                int param = checkParameters(loader.getContent());
                int mode = getMode(loader.getContent());
                int frame = getFrame(loader.getContent());

                boolean raw = (param & PARAM_RAW) > 0;
                boolean gif = (param &  PARAM_GIF) > 0;

                if(raw && !isTrusted) {
                    ch.sendMessage(LangID.getStringByID("data.animation.gif.ignoring", lang)).queue();
                }

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new FormAnimMessageHolder(forms, msg, u.getId(), ch.getId(), res, mode, frame, false, ((param & PARAM_DEBUG) > 0), lang, true, raw && isTrusted, gif));
                });

                disableTimer();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("formImage.fail.noParameter", lang)).queue();
            disableTimer();
        }
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("data.animation.mode.walk", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("data.animation.mode.idle", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("data.animation.mode.attack", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("data.animation.mode.kb", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("data.animation.mode.enter", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowDown", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("data.animation.mode.burrowMove", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("data.animation.mode.burrowUp", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
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

        return -1;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-d":
                    case "-debug":
                        if((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                    case "-r":
                    case "-raw":
                        if((result & PARAM_RAW) == 0) {
                            result |= PARAM_RAW;
                        } else {
                            break label;
                        }
                        break;
                    case "-f":
                    case "-fr":
                        if(i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i+1])) {
                            i++;
                        } else {
                            break label;
                        }
                        break;
                    case "-m":
                    case "-mode":
                        if(i < pureMessage.length -1) {
                            i++;
                        } else {
                            break label;
                        }
                        break;
                    case "-g":
                    case "-gif":
                        if((result & PARAM_GIF) == 0) {
                            result |= PARAM_GIF;
                        } else {
                            break label;
                        }
                        break;
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
        boolean raw = false;

        boolean mode = false;
        boolean frame = false;
        boolean gif = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-debug":
                case "-d":
                    if(!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-r":
                case "-raw":
                    if(!raw) {
                        raw = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-mode":
                case "-m":
                    if(!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-fr":
                case "-f":
                    if(!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-g":
                case "-gif":
                    if(!gif) {
                        gif = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                default:
                    result.append(contents[i]);
                    written = true;
            }

            if(written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private List<String> accumulateData(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= forms.size())
                break;

            Form f = forms.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            String name = StaticStore.safeMultiLangGet(f, lang);

            if(name != null)
                fname += name;

            data.add(fname);
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

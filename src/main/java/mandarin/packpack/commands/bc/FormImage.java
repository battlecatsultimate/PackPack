package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.FormAnimMessageHolder;
import mandarin.packpack.supporter.server.holder.segment.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public FormImage(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FORMIMAGE_ID, false);
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

        String[] list = getContent(event).split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            String search = filterCommand(getContent(event));

            if(search.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("fimg_more", lang), getMessage(event), a -> a);
                disableTimer();
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search, false, lang);

            if(forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formst_nounit", lang).replace("_", getSearchKeyword(getContent(event))), getMessage(event), a -> a);
                disableTimer();
            } else if(forms.size() == 1) {
                int param = checkParameters(getContent(event));
                int mode = getMode(getContent(event));
                int frame = getFrame(getContent(event));

                forms.get(0).anim.load();

                if(mode >= forms.get(0).anim.anims.length)
                    mode = 0;

                EAnimD<?> anim = forms.get(0).anim.getEAnim(ImageDrawing.getAnimType(mode, forms.get(0).anim.anims.length));

                File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0));

                forms.get(0).anim.unload();

                if(img != null) {
                    String fName = MultiLangCont.get(forms.get(0), lang);

                    if(fName == null || fName.isBlank())
                        fName = forms.get(0).names.toString();

                    if(fName.isBlank())
                        fName = LangID.getStringByID("data_unit", lang)+" "+ Data.trio(forms.get(0).uid.id)+" "+Data.trio(forms.get(0).fid);

                    sendMessageWithFile(ch, LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(mode, forms.get(0).anim.anims.length)).replace("=", String.valueOf(frame)), img, "result.png", getMessage(event));
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", getSearchKeyword(getContent(event))));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, forms.size(), data, lang));

                int param = checkParameters(getContent(event));
                int mode = getMode(getContent(event));
                int frame = getFrame(getContent(event));

                if(res != null) {
                    User u = getUser(event);

                    if(u != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(u.getId(), new FormAnimMessageHolder(forms, msg, res, ch.getId(), mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang, false, false, false));
                    }
                }

                disableTimer();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("fimg_more", lang)).queue();
            disableTimer();
        }
    }

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("fimg_walk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("fimg_idle", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("fimg_atk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("fimg_hb", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("fimg_enter", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrdown", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrmove", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("fimg_burrup", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
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

            if(written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private String getModeName(int mode, int max) {
        switch (mode) {
            case 1 -> {
                return LangID.getStringByID("fimg_idle", lang);
            }
            case 2 -> {
                return LangID.getStringByID("fimg_atk", lang);
            }
            case 3 -> {
                return LangID.getStringByID("fimg_hitback", lang);
            }
            case 4 -> {
                if (max == 5)
                    return LangID.getStringByID("fimg_enter", lang);
                else
                    return LangID.getStringByID("fimg_burrowdown", lang);
            }
            case 5 -> {
                return LangID.getStringByID("fimg_burrowmove", lang);
            }
            case 6 -> {
                return LangID.getStringByID("fimg_burrowup", lang);
            }
            default -> {
                return LangID.getStringByID("fimg_walk", lang);
            }
        }
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

package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.FormAnimHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class FormImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public FormImage(ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getMessage(event).split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            String search = filterCommand(getMessage(event));

            if(search.isBlank()) {
                ch.createMessage(LangID.getStringByID("fimg_more", lang)).subscribe();
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search);

            if(forms.isEmpty()) {
                ch.createMessage(LangID.getStringByID("formst_nounit", lang).replace("_", filterCommand(getMessage(event)))).subscribe();
            } else if(forms.size() == 1) {
                int param = checkParameters(getMessage(event));
                int mode = getMode(getMessage(event));
                int frame = getFrame(getMessage(event));

                File img = ImageDrawing.drawFormImage(forms.get(0), mode, frame, 1.0, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0));

                if(img != null) {
                    FileInputStream fis = new FileInputStream(img);

                    ch.createMessage(m -> {
                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        String fName = MultiLangCont.get(forms.get(0));

                        CommonStatic.getConfig().lang = oldConfig;

                        if(fName == null || fName.isBlank())
                            fName = forms.get(0).name;

                        if(fName == null || fName.isBlank())
                            fName = LangID.getStringByID("data_unit", lang)+" "+ Data.trio(forms.get(0).uid.id)+" "+Data.trio(forms.get(0).fid);

                        m.setContent(LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(mode, forms.get(0).anim.anims.length)).replace("=", String.valueOf(frame)));
                        m.addFile("result.png", fis);
                    }).subscribe(null, null, () -> {
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
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getMessage(event))));

                String check;

                if(forms.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                sb.append("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= forms.size())
                        break;

                    Form f = forms.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(forms.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(forms.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                int param = checkParameters(getMessage(event));
                int mode = getMode(getMessage(event));
                int frame = getFrame(getMessage(event));

                if(res != null) {
                    event.getMember().ifPresent(member -> StaticStore.formAnimHolder.put(member.getId().asString(), new FormAnimHolder(forms, res, mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang)));
                }
            }
        } else {
            ch.createMessage(LangID.getStringByID("fimg_more", lang)).subscribe();
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
                    case "-t":
                        if((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                        break;
                    case "-d":
                    case "-debug":
                        if((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
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

        boolean preParamEnd = false;

        boolean debug = false;
        boolean trans = false;

        boolean mode = false;
        boolean frame = false;

        for(int i = 1; i < contents.length; i++) {
            if(!preParamEnd) {
                if(contents[i].equals("-t")) {
                    if(!trans) {
                        trans = true;
                    } else {
                        result.append(contents[i]);

                        if(i < contents.length - 1)
                            result.append(" ");

                        preParamEnd = true;
                    }
                }
                else if(contents[i].equals("-debug") || contents[i].equals("-d")) {
                    if(!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);

                        if(i < contents.length - 1)
                            result.append(" ");

                        preParamEnd = true;
                    }
                } else {
                    result.append(contents[i]);

                    if(i < contents.length - 1)
                        result.append(" ");

                    preParamEnd = true;
                }
            } else {
                if(contents[i].equals("-mode") || contents[i].equals("-m")) {
                    if(!mode) {
                        if(i < contents.length - 1) {
                            mode = true;
                            i++;
                        } else
                            result.append(contents[i]);
                    } else
                        result.append(contents[i]);
                } else if(contents[i].equals("-fr") || contents[i].equals("-f")) {
                    if(!frame) {
                        if(i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                            frame = true;
                            i++;
                        } else {
                            result.append(contents[i]);
                        }
                    } else {
                        result.append(contents[i]);
                    }
                } else {
                    System.out.println("OH");
                    result.append(contents[i]);
                }

                if(i < contents.length - 1)
                    result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private String getModeName(int mode, int max) {
        switch (mode) {
            case 1:
                return LangID.getStringByID("fimg_idle", lang);
            case 2:
                return LangID.getStringByID("fimg_atk", lang);
            case 3:
                return LangID.getStringByID("fimg_hitback", lang);
            case 4:
                if(max == 5)
                    return LangID.getStringByID("fimg_enter", lang);
                else
                    return LangID.getStringByID("fimg_burrowdown", lang);
            case 5:
                return LangID.getStringByID("fimg_burrowmove", lang);
            case 6:
                return LangID.getStringByID("fimg_burrowup", lang);
            default:
                return LangID.getStringByID("fimg_walk", lang);
        }
    }
}

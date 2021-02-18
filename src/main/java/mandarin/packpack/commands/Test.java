package mandarin.packpack.commands;

import common.CommonStatic;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
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
import java.util.Set;

public class Test extends ConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public Test(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
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
                boolean debug = (param & PARAM_DEBUG) > 0;

                Form f = forms.get(0);

                if(f.unit == null || f.unit.id == null)
                    return;
                else if(!debug) {
                    String id = f.unit.id.pack+" - "+ Data.trio(f.unit.id.id)+" - "+Data.trio(f.fid) + " - " + Data.trio(mode);

                    String link = StaticStore.imgur.get(id);

                    if(link != null) {
                        ch.createMessage("Bringing cached image link\n"+link).subscribe();
                        return;
                    }
                }

                f.anim.load();

                if(f.anim.len(getAnimType(mode, f.anim.anims.length)) > 300) {
                    ch.createMessage("Length : "+f.anim.len(getAnimType(mode, f.anim.anims.length))+" (Limiting to 300f)").subscribe();
                } else {
                    ch.createMessage("Length : "+f.anim.len(getAnimType(mode, f.anim.anims.length))).subscribe();
                }

                CommonStatic.getConfig().ref = false;

                if(mode >= f.anim.anims.length)
                    mode = 0;

                Message msg = ch.createMessage("Analyzing Box...").block();

                if(msg == null)
                    return;

                long start = System.currentTimeMillis();

                File img = ImageDrawing.drawFormGif(f, msg, mode, 1.0, debug);

                long end = System.currentTimeMillis();

                String time = DataToString.df.format((end - start)/1000.0);

                FileInputStream fis;

                if(img == null) {
                    ch.createMessage("Failed to generate gif").subscribe();
                    return;
                }

                fis = new FileInputStream(img);

                int finalMode = mode;
                ch.createMessage(
                        m -> {
                            m.setContent("Done : Time = "+time+"sec ("+getFileSize(img)+")");
                            m.addFile("result.gif", fis);
                        }
                ).subscribe(m -> {
                    if(img.length() >= 1024 * 1024 && !debug) {
                        cacheImage(f, finalMode, m);
                    }
                }, null, () -> {
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
                    event.getMember().ifPresent(member -> StaticStore.formAnimHolder.put(member.getId().asString(), new FormAnimHolder(forms, res, mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang, false)));
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
                    if(LangID.getStringByID("fimg_walk", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("fimg_idle", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("fimg_atk", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("fimg_hb", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("fimg_enter", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrdown", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrmove", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("fimg_burrup", lang).toLowerCase(java.util.Locale.ENGLISH).contains(msg[i+1].toLowerCase(java.util.Locale.ENGLISH)))
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

    public static AnimU.UType getAnimType(int mode, int max) {
        switch (mode) {
            case 1:
                return AnimU.UType.IDLE;
            case 2:
                return AnimU.UType.ATK;
            case 3:
                return AnimU.UType.HB;
            case 4:
                if(max == 5)
                    return AnimU.UType.ENTER;
                else
                    return AnimU.UType.BURROW_DOWN;
            case 5:
                return AnimU.UType.BURROW_MOVE;
            case 6:
                return AnimU.UType.BURROW_UP;
            default:
                return AnimU.UType.WALK;
        }
    }

    private String getFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }

    private void cacheImage(Form f, int mode, Message msg) {
        if(f.unit == null || f.unit.id == null)
            return;

        String id = f.unit.id.pack+" - "+ Data.trio(f.unit.id.id)+" - "+Data.trio(f.fid) + " - " + Data.trio(mode);

        Set<Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Attachment a : att) {
            if (a.getFilename().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link);

                return;
            }
        }
    }
}

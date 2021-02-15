package mandarin.packpack.supporter.server;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FormAnimHolder {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    private final ArrayList<Form> form;
    private final Message msg;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final int lang;

    private int page = 0;
    private boolean expired = false;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public FormAnimHolder(ArrayList<Form> form, Message msg, int mode, int frame, boolean transparent, boolean debug, int lang) {
        this.form = form;
        this.msg = msg;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.lang = lang;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                msg.edit(m -> {
                    m.setContent(LangID.getStringByID("formst_expire", lang));

                    expired = true;

                    msg.getAuthor().ifPresent(u -> StaticStore.formAnimHolder.remove(u.getId().asString()));
                }).subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= form.size())
                return RESULT_STILL;

            page++;

            msg.edit(m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            msg.edit(m -> {
                String check;

                if(form.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= form.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(check);

                for(int i = 20 * page; i < 20 * (page +1); i++) {
                    if(i >= form.size())
                        break;

                    Form f = form.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(form.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.setContent(sb.toString());
            }).subscribe();

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            MessageChannel ch = event.getMessage().getChannel().block();

            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= form.size())
                return RESULT_STILL;

            if(ch != null) {
                try {
                    File img = ImageDrawing.drawFormImage(form.get(id), mode, frame, 1.0, transparent, debug);

                    if(img != null) {
                        FileInputStream fis = new FileInputStream(img);
                        CommonStatic.getConfig().lang = lang;

                        ch.createMessage(m -> {
                            String fName = MultiLangCont.get(form.get(id));

                            if(fName == null || fName.isBlank())
                                fName = form.get(id).name;

                            if(fName == null || fName.isBlank())
                                fName = LangID.getStringByID("data_unit", lang)+" "+ Data.trio(form.get(id).uid.id)+" "+Data.trio(form.get(id).fid);

                            m.setContent(LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(mode, form.get(id).anim.anims.length)).replace("=", String.valueOf(frame)));
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            msg.delete().subscribe();

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            msg.edit(m -> {
                m.setContent(LangID.getStringByID("formst_cancel", lang));
                expired = true;
            }).subscribe();

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if(StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1])-1;

                    if(p < 0 || p * 20 >= form.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    msg.edit(m -> {
                        String check;

                        if(form.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= form.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(check);

                        for(int i = 20 * page; i < 20 * (page +1); i++) {
                            if(i >= form.size())
                                break;

                            Form f = form.get(i);

                            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                            if(MultiLangCont.get(f) != null)
                                fname += MultiLangCont.get(f);

                            sb.append(i+1).append(". ").append(fname).append("\n");
                        }

                        if(form.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(form.size()/20 + 1)));

                        sb.append(LangID.getStringByID("formst_can", lang));
                        sb.append("```");

                        m.setContent(sb.toString());
                    }).subscribe();

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
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

package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.FormSpriteMessageHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class FormSprite extends TimedConstraintCommand {
    private static final int PARAM_UNI = 2;
    private static final int PARAM_UDI = 4;
    private static final int PARAM_EDI = 8;

    public FormSprite(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FORMSPRITE_ID, false);
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

        String[] contents = getContent(event).split(" ");

        if(contents.length == 1) {
            replyToMessageSafely(ch, LangID.getStringByID("fimg_more", lang), getMessage(event), a -> a);
        } else {
            String search = filterCommand(getContent(event));

            if(search.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("fimg_more", lang), getMessage(event), a -> a);
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search, lang);

            if(forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formst_nounit", lang).replace("_", filterCommand(getContent(event))), getMessage(event), a -> a);
                disableTimer();
            } else if(forms.size() == 1) {
                int param = checkParameter(getContent(event));

                EntityHandler.getFormSprite(forms.get(0), ch, getMessage(event), getModeFromParam(param), lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getContent(event))));

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

                int param = checkParameter(getContent(event));

                int mode = getModeFromParam(param);

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, forms.size(), data, lang));

                if(res != null) {
                    User u = getUser(event);

                    if(u != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(u.getId(), new FormSpriteMessageHolder(forms, msg, res, ch.getId(), mode, lang));
                    }
                }

                disableTimer();
            }
        }
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        label:
        for (String content : contents) {
            switch (content) {
                case "-uni":
                    res |= PARAM_UNI;
                    break label;
                case "-udi":
                    res |= PARAM_UDI;
                    break label;
                case "-edi":
                    res |= PARAM_EDI;
                    break label;
            }
        }

        return res;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean uni = false;
        boolean udi = false;
        boolean edi = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-uni":
                    if (!uni) {
                        uni = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-udi":
                    if(!udi) {
                        udi = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-edi":
                    if(!edi) {
                        edi = true;
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

    private int getModeFromParam(int param) {
        if((param & PARAM_UNI) > 0)
            return 1;
        if((param & PARAM_UDI) > 0)
            return 2;
        if((param & PARAM_EDI) > 0)
            return 3;
        else
            return 0;
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
}

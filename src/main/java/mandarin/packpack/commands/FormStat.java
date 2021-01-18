package mandarin.packpack.commands;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.FormStatHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.ArrayList;
import java.util.Locale;

public class FormStat extends ConstraintCommand {
    private static final int PARAM_TALENT = 2;
    private static final int PARAM_SECOND = 4;

    public FormStat(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getMessage(event).split(" ",2);

        if(list.length == 1 || filterCommand(getMessage(event)).isBlank()) {
            ch.createMessage(LangID.getStringByID("formst_noname", lang)).subscribe();
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(filterCommand(getMessage(event)));

            if (forms.size() == 1) {
                CommonStatic.getConfig().lang = lang;

                int param = checkParameters(getMessage(event));

                int lv = handleLevel(getMessage(event));

                boolean isFrame = (param & PARAM_SECOND) == 0;
                boolean talent = (param & PARAM_TALENT) > 0;

                EntityHandler.showUnitEmb(forms.get(0), ch, isFrame, talent, lv, lang);
            } else if (forms.size() == 0) {
                ch.createMessage(LangID.getStringByID("formst_nounit", lang)).subscribe();
            } else {
                CommonStatic.getConfig().lang = lang;

                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang));

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

                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                int param = checkParameters(getMessage(event));

                int lv = handleLevel(getMessage(event));

                if(res != null) {
                    event.getMember().ifPresent(member -> StaticStore.formHolder.put(member.getId().asString(), new FormStatHolder(forms, res, param, lv, lang)));
                }
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ROOT).replace(" ", "");

            for(int i = 0; i < 2; i++) {
                if(pureMessage.startsWith("-t")) {
                    if((result & PARAM_TALENT) == 0) {
                        result |= PARAM_TALENT;

                        pureMessage = pureMessage.substring(2);
                    }
                } else if(pureMessage.startsWith("-s")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;

                        pureMessage = pureMessage.substring(2);
                    }
                }
            }
        }

        return result;
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isLevel = false;
        boolean isTalent = false;

        boolean paramEnd = false;

        StringBuilder command = new StringBuilder();

        for(int i = 0; i < content.length; i++) {
            if (i == 0)
                continue;

            if(content[i].equals("-s"))
                if(isSec || paramEnd)
                    command.append(content[i]).append(" ");
                else {
                    isSec = true;
                }
            else if(content[i].equals("-t")) {
                if(isTalent || paramEnd)
                    command.append(content[i]).append(" ");
                else {
                    isTalent = true;
                }
            } else {
                paramEnd = true;

                if(content[i].equals("-lv") && i < content.length - 1 && StaticStore.isNumeric(content[i+1])) {
                    if(isLevel)
                        command.append(content[i]).append(" ");
                    else {
                        isLevel = true;
                    }

                    i++;
                } else
                    command.append(content[i]).append(" ");
            }
        }

        return command.substring(0, command.length()-1);
    }

    private int handleLevel(String msg) {
        int lv = 30;

        if(msg.contains("-lv")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-lv") && i != content.length -1) {
                    if(StaticStore.isNumeric(content[i+1])) {
                        lv = StaticStore.safeParseInt(content[i + 1]);
                        return lv;
                    }
                }
            }
        }

        return lv;
    }
}

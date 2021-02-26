package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.FormStatHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.ArrayList;

public class FormStat extends ConstraintCommand {
    private static final int PARAM_TALENT = 2;
    private static final int PARAM_SECOND = 4;

    public FormStat(ROLE role, int lang, IDHolder holder) {
        super(role, lang, holder);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        String[] list = getMessage(event).split(" ",2);

        if(list.length == 1 || filterCommand(getMessage(event)).isBlank()) {
            ch.createMessage(LangID.getStringByID("formst_noname", lang)).subscribe();
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(filterCommand(getMessage(event)));

            if (forms.size() == 1) {
                CommonStatic.getConfig().lang = lang;

                int param = checkParameters(getMessage(event));

                int[] lv = handleLevel(getMessage(event));

                boolean isFrame = (param & PARAM_SECOND) == 0;
                boolean talent = (param & PARAM_TALENT) > 0;

                EntityHandler.showUnitEmb(forms.get(0), ch, isFrame, talent, lv, lang);
            } else if (forms.size() == 0) {
                ch.createMessage(LangID.getStringByID("formst_nounit", lang).replace("_", filterCommand(getMessage(event)))).subscribe();
            } else {
                CommonStatic.getConfig().lang = lang;

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

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(f) != null)
                        fname += MultiLangCont.get(f);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(forms.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(forms.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                int param = checkParameters(getMessage(event));

                int[] lv = handleLevel(getMessage(event));

                if(res != null) {
                    event.getMember().ifPresent(member -> StaticStore.putHolder(member.getId().asString(), new FormStatHolder(forms, event.getMessage(), res, ch.getId().asString(), param, lv, lang)));
                }

            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-t")) {
                    if((result & PARAM_TALENT) == 0) {
                        result |= PARAM_TALENT;
                    } else
                        break;
                } else if(str.startsWith("-s")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;
                    } else
                        break;
                } else {
                    break;
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

                if(content[i].equals("-lv") && i < content.length - 1) {
                    if(isLevel)
                        command.append(content[i]).append(" ");
                    else {
                        isLevel = true;
                    }

                    String text = getLevelText(content, i+1);

                    if(text.contains(" ")) {
                        i += getLevelText(content, i + 1).split(" ").length;
                    } else if(msg.endsWith(text)) {
                        i++;
                    }
                } else
                    command.append(content[i]).append(" ");
            }
        }

        if(command.toString().isBlank()) {
            return "";
        }

        return command.substring(0, command.length()-1);
    }

    private int[] handleLevel(String msg) {
        if(msg.contains("-lv")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-lv") && i != content.length -1) {
                    String[] trial = getLevelText(content, i+1).replace(" ", "").split(",");

                    int length = 0;

                    for (String s : trial) {
                        if (StaticStore.isNumeric(s))
                            length++;
                        else
                            break;
                    }

                    if(length == 0)
                        return new int[] {-1};
                    else {
                        int[] lv = new int[length];

                        for (int j = 0; j < length; j++) {
                            lv[j] = StaticStore.safeParseInt(trial[j]);
                        }

                        return lv;
                    }
                }
            }
        } else {
            return new int[] {-1};
        }

        return new int[] {-1};
    }

    private String getLevelText(String[] trial, int index) {
        StringBuilder sb = new StringBuilder();

        for(int i = index; i < trial.length; i++) {
            sb.append(trial[i]);

            if(i != trial.length - 1)
                sb.append(" ");
        }

        StringBuilder fin = new StringBuilder();

        boolean commaStart = false;
        boolean beforeSpace = false;
        int numberLetter = 0;
        int commaAdd = 0;

        for(int i = 0; i < sb.length(); i++) {
            if(sb.charAt(i) == ',') {
                if(!commaStart && commaAdd <= 5) {
                    commaStart = true;
                    commaAdd++;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else {
                    break;
                }
            } else if(sb.charAt(i) == ' ') {
                beforeSpace = true;
                numberLetter = 0;
                fin.append(sb.charAt(i));
            } else {
                if(Character.isDigit(sb.charAt(i))) {
                    commaStart = false;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else if(beforeSpace) {
                    numberLetter = 0;
                    break;
                } else {
                    break;
                }

                beforeSpace = false;
            }

            if(i == sb.length() - 1)
                numberLetter = 0;
        }

        String result = fin.toString();

        result = result.substring(0, result.length() - numberLetter);

        return result;
    }
}

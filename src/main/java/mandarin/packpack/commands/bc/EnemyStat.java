package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.EnemyStatHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.ArrayList;

public class EnemyStat extends ConstraintCommand {
    private static final int PARAM_SECOND = 2;

    public EnemyStat(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        String[] list = getContent(event).split(" ", 2);

        if(list.length == 1 || filterCommand(getContent(event)).isBlank()) {
            ch.createMessage(LangID.getStringByID("formst_noname", lang)).subscribe();
        } else {
            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(filterCommand(getContent(event)), lang);

            if(enemies.size() == 1) {
                int param = checkParameters(getContent(event));

                int[] magnification = handleMagnification(getContent(event));

                boolean isFrame = (param & PARAM_SECOND) == 0;

                EntityHandler.showEnemyEmb(enemies.get(0), ch, isFrame, magnification, lang);
            } else if(enemies.size() == 0) {
                ch.createMessage(LangID.getStringByID("enemyst_noenemy", lang).replace("_", filterCommand(getContent(event)))).subscribe();
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getContent(event))));

                String check;

                if(enemies.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                sb.append("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= enemies.size())
                        break;

                    Enemy e = enemies.get(i);

                    String ename = e.id == null ? "UNKNOWN " : Data.trio(e.id.id)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(e) != null)
                        ename += MultiLangCont.get(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(ename).append("\n");
                }

                if(enemies.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(enemies.size() / 20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                int[] magnification = handleMagnification(getContent(event));
                boolean isFrame = (checkParameters(getContent(event)) & PARAM_SECOND) == 0;

                if(res != null) {
                    getMember(event).ifPresent(member -> {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(member.getId().asString(), new EnemyStatHolder(enemies, msg, res, ch.getId().asString(), magnification, isFrame, lang));
                    });
                }
            }
        }
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isLevel = false;

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
            else {
                paramEnd = true;

                if(content[i].equals("-m") && i < content.length - 1) {
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

        if(command.toString().isBlank())
            return "";

        return command.substring(0, command.length()-1);
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-s")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;
                    } else
                        break;
                }
            }
        }

        return result;
    }

    private int[] handleMagnification(String msg) {
        if(msg.contains("-m")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-m") && i != content.length -1) {
                    String[] trial = getLevelText(content, i+1).replace(" ", "").split(",");

                    int length = 0;

                    for (String s : trial) {
                        if (StaticStore.isNumeric(s))
                            length++;
                        else
                            break;
                    }

                    if(length == 0)
                        return new int[] {100};
                    else {
                        int[] lv = new int[length];

                        for (int j = 0; j < length; j++) {
                            if(trial[j].isBlank() && StaticStore.isNumeric(trial[j])) {
                                lv[j] = 100;
                            } else {
                                lv[j] = StaticStore.safeParseInt(trial[j]);
                            }
                        }

                        return lv;
                    }
                }
            }
        } else {
            return new int[] {100};
        }

        return new int[] {100};
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
                if(!commaStart && commaAdd <= 1) {
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

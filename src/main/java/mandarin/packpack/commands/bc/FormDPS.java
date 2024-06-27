package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.search.FormDPSHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FormDPS extends TimedConstraintCommand {
    private static final int PARAM_TALENT = 2;
    private static final int PARAM_TREASURE = 4;

    private final ConfigHolder config;

    public FormDPS(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder idHolder, ConfigHolder config, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_FORMDPS_ID, false);

        if(config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() throws Exception {
        super.prepare();

        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ",2);
        String[] segments = loader.getContent().split(" ");

        StringBuilder removeMistake = new StringBuilder();

        for(int i = 0; i < segments.length; i++) {
            if(segments[i].matches("-lv(l)?(\\d+(,)?)+")) {
                removeMistake.append("-lv ").append(segments[i].replace("-lvl", "").replace("-lv", ""));
            } else {
                removeMistake.append(segments[i]);
            }

            if(i < segments.length - 1)
                removeMistake.append(" ");
        }

        String command = removeMistake.toString();
        int param = checkParameters(command);

        Level lv = handleLevel(command);

        boolean talent = (param & PARAM_TALENT) > 0 || lv.getTalents().length > 0;
        boolean isTreasure = (param & PARAM_TREASURE) > 0 || config.treasure;

        if(list.length == 1 || filterCommand(command).isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("formst_noname", lang), loader.getMessage(), a -> a);
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(filterCommand(command), false, lang);

            if (forms.size() == 1) {
                Form f = forms.getFirst();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getMessage().getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showFormDPS(ch, loader.getMessage(), f, treasure, lv, config, talent, isTreasure, lang);
            } else if (forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formst_nounit", lang).replace("_", getSearchKeyword(loader.getContent())), loader.getMessage(), a -> a);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", getSearchKeyword(command)));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateListData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                    StaticStore.putHolder(u.getId(), new FormDPSHolder(forms, msg, config, res, ch.getId(), param, lv, treasure, lang));
                });
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            label:
            for(String str : pureMessage) {
                switch (str) {
                    case "-t" -> {
                        if ((result & PARAM_TALENT) == 0) {
                            result |= PARAM_TALENT;
                        } else
                            break label;
                    }
                    case "-tr", "-treasure" -> {
                        if ((result & PARAM_TREASURE) == 0) {
                            result |= PARAM_TREASURE;
                        } else
                            break label;
                    }
                }
            }
        }

        return result;
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isLevel = false;
        boolean isTalent = false;
        boolean isTreasure = false;

        StringBuilder command = new StringBuilder();

        for(int i = 1; i < content.length; i++) {
            boolean written = false;

            switch (content[i]) {
                case "-t" -> {
                    if (!isTalent)
                        isTalent = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-lv", "-lvl" -> {
                    if (!isLevel && i < content.length - 1) {
                        String text = getLevelText(content, i + 1);

                        if (text.contains(" ")) {
                            i += getLevelText(content, i + 1).split(" ").length;
                        } else if (msg.endsWith(text)) {
                            i++;
                        }

                        isLevel = true;
                    } else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-tr", "-treasure" -> {
                    if (!isTreasure)
                        isTreasure = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                default -> {
                    command.append(content[i]);
                    written = true;
                }
            }

            if(written && i < content.length - 1) {
                command.append(" ");
            }
        }

        if(command.toString().isBlank()) {
            return "";
        }

        return command.toString().trim();
    }

    private Level handleLevel(String msg) {
        if(msg.contains("-lv")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if((content[i].equals("-lv") || content[i].equals("-lvl")) && i != content.length -1) {
                    String[] trial = getLevelText(content, i+1).replace(" ", "").split(",");

                    int length = 0;

                    for (String s : trial) {
                        if (StaticStore.isNumeric(s))
                            length++;
                        else
                            break;
                    }

                    if(length == 0) {
                        Level res = new Level(0);

                        res.setLevel(-1);

                        return res;
                    }
                    else {
                        ArrayList<Integer> lv = new ArrayList<>();

                        for (int j = 0; j < length; j++) {
                            lv.add(StaticStore.safeParseInt(trial[j]));
                        }

                        Level level = new Level(0);

                        if(!lv.isEmpty())
                            level.setLevel(lv.getFirst());
                        else
                            level.setLevel(-1);

                        if(lv.size() > 1) {
                            int[] t = new int[lv.size() - 1];

                            for(int j = 1; j < lv.size(); j++) {
                                t[j - 1] = lv.get(j);
                            }

                            level.setTalents(t);
                        }

                        return level;
                    }
                }
            }
        } else {
            Level res = new Level(0);

            res.setLevel(-1);

            return res;
        }

        Level res = new Level(0);

        res.setLevel(-1);

        return res;
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
                if(!commaStart && commaAdd <= 8) {
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

    private List<String> accumulateListData(List<Form> forms) {
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

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

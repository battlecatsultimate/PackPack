package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.search.EnemyDPSHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnemyDPS extends TimedConstraintCommand {

    public EnemyDPS(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_ENEMYDPS_ID, false);
    }

    @Override
    public void prepare() throws Exception {
        super.prepare();

        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] list = loader.getContent().split(" ", 2);
        String[] segments = loader.getContent().split(" ");

        StringBuilder removeMistake = new StringBuilder();

        for(int i = 0; i < segments.length; i++) {
            if(segments[i].matches("-m(\\d+(,|%,|%$)?)+")) {
                removeMistake.append("-m ").append(segments[i].replace("-m", ""));
            } else {
                removeMistake.append(segments[i]);
            }

            if(i < segments.length - 1)
                removeMistake.append(" ");
        }

        String command = removeMistake.toString();

        if(list.length == 1 || filterCommand(command).isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("formst_noname", lang), loader.getMessage(), a -> a);
        } else {
            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(filterCommand(command), lang);

            if(enemies.size() == 1) {
                int magnification = handleMagnification(command);

                Message m = loader.getMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(m.getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showEnemyDPS(ch, loader.getMessage(), enemies.get(0), treasure, magnification, lang);
            } else if(enemies.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", getSearchKeyword(command)), loader.getMessage(), a -> a);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", getSearchKeyword(command)));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(enemies);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                    if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, enemies.size(), data, lang), res -> {
                    int magnification = handleMagnification(command);

                    if(res != null) {
                        User u = loader.getUser();

                        Message msg = loader.getMessage();

                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                        StaticStore.putHolder(u.getId(), new EnemyDPSHolder(enemies, msg, res, ch.getId(), treasure, magnification, lang));
                    }
                });
            }
        }
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isExtra = false;
        boolean isLevel = false;
        boolean isCompact = false;

        StringBuilder command = new StringBuilder();

        for(int i = 1; i < content.length; i++) {
            boolean written = false;

            switch (content[i]) {
                case "-s" -> {
                    if (!isSec)
                        isSec = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-e", "-extra" -> {
                    if (!isExtra)
                        isExtra = true;
                    else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-m" -> {
                    if (!isLevel && i < content.length - 1) {
                        String text = getLevelText(content, i + 1);

                        if (text.contains(" ")) {
                            i += text.split(" ").length;
                        } else if (msg.endsWith(text)) {
                            i++;
                        }

                        isLevel = true;
                    } else {
                        command.append(content[i]);
                        written = true;
                    }
                }
                case "-c", "-compact" -> {
                    if (!isCompact)
                        isCompact = true;
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

        if(command.toString().isBlank())
            return "";

        return command.toString().trim();
    }

    private int handleMagnification(String msg) {
        if(msg.contains("-m")) {
            String[] content = msg.split(" ");

            for(int i = 0; i < content.length; i++) {
                if(content[i].equals("-m") && i != content.length - 1 && StaticStore.isNumeric(content[i + 1])) {
                    return StaticStore.safeParseInt(content[i + 1]);
                }
            }
        }

        return 100;
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
        boolean percentage = false;
        boolean numberStart = false;
        int numberLetter = 0;
        int commaAdd = 0;

        for(int i = 0; i < sb.length(); i++) {
            if(sb.charAt(i) == ',') {
                if(!commaStart && commaAdd <= 1) {
                    commaStart = true;
                    numberStart = false;
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
            } else if(sb.charAt(i) == '%') {
                if(!percentage && numberStart) {
                    percentage = true;
                    numberStart = false;
                    fin.append(sb.charAt(i));
                    numberLetter++;
                } else {
                    break;
                }
            } else {
                if(Character.isDigit(sb.charAt(i))) {
                    numberStart = true;
                    commaStart = false;
                    percentage = false;
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

    private List<String> accumulateData(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename;

            if(e.id != null) {
                ename = Data.trio(e.id.id)+" ";
            } else {
                ename = " ";
            }

            String name = StaticStore.safeMultiLangGet(e, lang);

            if(name != null)
                ename += name;

            data.add(ename);
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

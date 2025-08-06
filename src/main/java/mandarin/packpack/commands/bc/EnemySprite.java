package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.EnemySpriteMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EnemySprite extends TimedConstraintCommand {
    private static final int PARAM_EDI = 2;

    private final ConfigHolder config;

    public EnemySprite(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_ENEMYSPRITE_ID, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if (contents.length == 1) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyImage.fail.noParameter", lang));
        } else {
            String enemyName = filterCommand(loader.getContent());

            if (enemyName.isBlank()) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyImage.fail.noParameter", lang));

                return;
            }

            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(enemyName, lang);

            if (enemies.isEmpty()) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("enemyStat.fail.noEnemy", lang).formatted(getSearchKeyword(loader.getContent())));

                disableTimer();
            } else if (enemies.size() == 1) {
                int param = checkParameter(loader.getContent());

                EntityHandler.generateEnemySprite(enemies.getFirst(), ch, loader.getMessage(), getModeFromParam(param), lang);
            } else {
                int param = checkParameter(loader.getContent());

                int mode = getModeFromParam(param);

                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    User u = loader.getUser();

                    StaticStore.putHolder(u.getId(), new EnemySpriteMessageHolder(enemies, loader.getMessage(), u.getId(), ch.getId(), msg, enemyName, config.searchLayout, mode, lang));
                }, getSearchComponents(enemies.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(enemyName, enemies.size()), enemies, this::accumulateTextData, config.searchLayout, lang));

                disableTimer();
            }
        }
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        for (String content : contents) {
            if ("-edi".equals(content)) {
                res |= PARAM_EDI;
                break;
            }
        }

        return res;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if (contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean edi = false;

        for (int i = 1; i < contents.length; i++) {
            boolean written = false;

            if (contents[i].equals("-edi")) {
                if (!edi) {
                    edi = true;
                } else {
                    result.append(contents[i]);
                    written = true;
                }
            } else {
                result.append(contents[i]);
                written = true;
            }

            if (written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private int getModeFromParam(int param) {
        if ((param & PARAM_EDI) > 0)
            return 3;
        else
            return 0;
    }

    private List<String> accumulateTextData(List<Enemy> enemies, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = 0; i < Math.min(enemies.size(), config.searchLayout.chunkSize); i++) {
            Enemy e = enemies.get(i);

            if (e.id == null)
                continue;

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(e.id.id);

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(e.id.id) + "`";

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(e.id.id);
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(e, lang);

                    if (text == null) {
                        text = Data.trio(e.id.id);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(e.id.id);
            }

            data.add(text);
        }

        return data;
    }

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if (result == null)
            return "";

        if (result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

package mandarin.packpack.supporter.server.holder.message.alias;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.AliasHolder;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AliasEnemyMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemies;
    private final AliasHolder.MODE mode;
    private final String aliasName;

    public AliasEnemyMessageHolder(ArrayList<Enemy> enemies, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, AliasHolder.MODE mode, CommonStatic.Lang.Locale lang, @Nonnull String keyword, @Nullable String aliasName) {
        super(author, userID, channelID, msg, keyword, ConfigHolder.SearchLayout.FANCY_LIST, lang);

        this.enemies = enemies;
        this.mode = mode;
        this.aliasName = aliasName;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
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

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        String eName = StaticStore.safeMultiLangGet(enemies.get(index), lang);

        if(eName == null || eName.isBlank())
            eName = enemies.get(index).names.toString();

        if(eName.isBlank())
            eName = Data.trio(enemies.get(index).id.id);

        ArrayList<String> alias = AliasHolder.getAlias(AliasHolder.TYPE.ENEMY, lang, enemies.get(index));

        switch (mode) {
            case GET -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(eName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                } else {
                    StringBuilder result = new StringBuilder(LangID.getStringByID("alias.aliases.enemy", lang).formatted(eName, alias.size()));
                    result.append("\n\n");

                    for (int i = 0; i < alias.size(); i++) {
                        String temp = "- " + alias.get(i);

                        if (result.length() + temp.length() > 1900) {
                            result.append("\n")
                                    .append(LangID.getStringByID("alias.etc", lang));
                            break;
                        }

                        result.append(temp);

                        if (i < alias.size() - 1) {
                            result.append("\n");
                        }
                    }

                    event.deferEdit()
                            .setComponents(TextDisplay.of(result.toString()))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                }
            }
            case ADD -> {
                if (alias == null)
                    alias = new ArrayList<>();

                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                if (alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.contain", lang).formatted(eName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.add(aliasName);

                AliasHolder.EALIAS.put(lang, enemies.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.added", lang).formatted(eName, aliasName)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias added\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + event.getUser().getAsMention());
            }
            case REMOVE -> {
                if (alias == null || alias.isEmpty()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.noAlias.unit", lang).formatted(eName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }
                if (aliasName.isBlank()) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.noName", lang)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }
                if (!alias.contains(aliasName)) {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("alias.failed.removeFail", lang).formatted(eName)))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    break;
                }

                alias.remove(aliasName);

                AliasHolder.EALIAS.put(lang, enemies.get(index), alias);

                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("alias.removed", lang).formatted(eName, aliasName)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.logger.uploadLog("Alias removed\n\nEnemy : " + eName + "\nAlias : " + aliasName + "\nBy : " + event.getUser().getAsMention());
            }
        }
    }

    @Override
    public int getDataSize() {
        return enemies.size();
    }

    @Override
    public void onExpire() {
        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("ui.search.expired", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }
}

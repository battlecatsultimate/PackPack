package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.EnemyGif;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.TimeBoolean;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EnemyAnimMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemies;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    private final String command;
    private final StringBuilder primary;

    public EnemyAnimMessageHolder(ArrayList<Enemy> enemies, Message author, String userID, String channelID, Message message, StringBuilder primary, String keyword, ConfigHolder.SearchLayout layout, int mode, int frame, boolean transparent, boolean debug, CommonStatic.Lang.Locale lang, boolean isGif, boolean raw, boolean gifMode) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.enemies = enemies;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        this.command = author.getContentRaw();
        this.primary = primary;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = chunk * page; i < chunk * (page + 1); i++) {
            if (i >= enemies.size())
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
        try {
            Enemy e = enemies.get(index);

            if(EnemyGif.forbidden.contains(e.id.id)) {
                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("data.animation.gif.dummy", lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                return;
            }

            if (gif) {
                TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                if (timeBoolean == null || timeBoolean.canDo) {
                    new Thread(() -> {
                        try {
                            Guild g = event.getGuild();

                            EntityHandler.generateEnemyAnim(e, message, getAuthorMessage(), primary, g == null ? 0 : g.getBoostTier().getKey(), mode, transparent, debug, frame, lang, raw, gifMode, () -> {
                                if(!StaticStore.conflictedAnimation.isEmpty()) {
                                    StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + command);
                                    StaticStore.conflictedAnimation.clear();
                                }

                                User u = event.getUser();

                                if(raw) {
                                    StaticStore.logger.uploadLog("Generated mp4 by user " + u.getName() + " for enemy ID " + Data.trio(e.id.id) + " with mode of " + mode);
                                }

                                long time = raw ? TimeUnit.MINUTES.toMillis(1) : TimeUnit.SECONDS.toMillis(30);

                                StaticStore.canDo.put("gif", new TimeBoolean(false, time));

                                StaticStore.executorHandler.postDelayed(time, () -> {
                                    System.out.println("Remove Process : gif");
                                    StaticStore.canDo.put("gif", new TimeBoolean(true));
                                });
                            }, () -> {
                                if(!StaticStore.conflictedAnimation.isEmpty()) {
                                    StaticStore.logger.uploadLog("Warning - Bot generated animation while this animation is already cached\n\nCommand : " + command);
                                    StaticStore.conflictedAnimation.clear();
                                }
                            });
                        } catch (Exception exception) {
                            StaticStore.logger.uploadErrorLog(exception, "E/EnemyAnimMessageHolder::onSelected - Failed to generate enemy animation");
                        }
                    }).start();
                } else {
                    event.deferEdit()
                            .setComponents(TextDisplay.of(LangID.getStringByID("bot.denied.reason.cooldown", lang).formatted(DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))))
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();
                }
            } else {
                User u = event.getUser();

                try {
                    if (StaticStore.timeLimit.containsKey(u.getId()) && StaticStore.timeLimit.get(u.getId()).containsKey(StaticStore.COMMAND_ENEMYIMAGE_ID)) {
                        long time = StaticStore.timeLimit.get(u.getId()).get(StaticStore.COMMAND_ENEMYIMAGE_ID);

                        if (System.currentTimeMillis() - time > 10000) {
                            EntityHandler.generateEnemyImage(e, event, getAuthorMessage(), debug, transparent, frame, mode, lang);

                            StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                        } else {
                            event.deferEdit()
                                    .setComponents(TextDisplay.of(LangID.getStringByID("bot.command.timeLimit", lang).formatted(DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))))
                                    .useComponentsV2()
                                    .setAllowedMentions(new ArrayList<>())
                                    .mentionRepliedUser(false)
                                    .queue();
                        }
                    } else if (StaticStore.timeLimit.containsKey(u.getId())) {
                        EntityHandler.generateEnemyImage(e, event, getAuthorMessage(), debug, transparent, frame, mode, lang);

                        StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                    } else {
                        EntityHandler.generateEnemyImage(e, event, getAuthorMessage(), debug, transparent, frame, mode, lang);

                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(u.getId(), memberLimit);
                    }
                } catch (Exception exception) {
                    StaticStore.logger.uploadErrorLog(exception, "E/EnemyAnimMessageHolder::onSelected - Failed to generate enemy image");
                }
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EnemyAnimMessageHolder::onSelected - Failed to handle enemy image/animation holder");
        }
    }

    @Override
    public int getDataSize() {
        return enemies.size();
    }
}

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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EnemyAnimMessageHolder extends SearchHolder {
    private final ArrayList<Enemy> enemy;

    private final int mode;
    private final int frame;
    private final boolean transparent;
    private final boolean debug;
    private final boolean gif;
    private final boolean raw;
    private final boolean gifMode;

    private final String command;

    public EnemyAnimMessageHolder(ArrayList<Enemy> enemy, Message author, String userID, String channelID, Message message, String keyword, ConfigHolder.SearchLayout layout, int mode, int frame, boolean transparent, boolean debug, CommonStatic.Lang.Locale lang, boolean isGif, boolean raw, boolean gifMode) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.enemy = enemy;

        this.mode = mode;
        this.frame = frame;
        this.transparent = transparent;
        this.debug = debug;
        this.gif = isGif;
        this.raw = raw;
        this.gifMode = gifMode;

        this.command = author.getContentRaw();
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for (int i = chunk * page; i < chunk * (page + 1); i++) {
            if (i >= enemy.size())
                break;

            Enemy e = enemy.get(i);

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
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        try {
            Enemy e = enemy.get(id);

            if(EnemyGif.forbidden.contains(e.id.id)) {
                ch.sendMessage(LangID.getStringByID("data.animation.gif.dummy", lang)).queue();

                message.delete().queue();

                return;
            }

            if (gif) {
                TimeBoolean timeBoolean = StaticStore.canDo.get("gif");

                if (timeBoolean == null || timeBoolean.canDo) {
                    new Thread(() -> {
                        try {
                            Guild g;

                            if(ch instanceof GuildChannel) {
                                g = event.getGuild();
                            } else {
                                g = null;
                            }

                            EntityHandler.generateEnemyAnim(e, ch, getAuthorMessage(), g == null ? 0 : g.getBoostTier().getKey(), mode, debug, frame, lang, raw, gifMode, () -> {
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
                    ch.sendMessage(LangID.getStringByID("bot.denied.reason.cooldown", lang).replace("_", DataToString.df.format((timeBoolean.totalTime - (System.currentTimeMillis() - StaticStore.canDo.get("gif").time)) / 1000.0))).queue();
                }
            } else {
                User u = event.getUser();

                try {
                    if (StaticStore.timeLimit.containsKey(u.getId()) && StaticStore.timeLimit.get(u.getId()).containsKey(StaticStore.COMMAND_ENEMYIMAGE_ID)) {
                        long time = StaticStore.timeLimit.get(u.getId()).get(StaticStore.COMMAND_ENEMYIMAGE_ID);

                        if (System.currentTimeMillis() - time > 10000) {
                            EntityHandler.generateEnemyImage(e, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

                            StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                        } else {
                            ch.sendMessage(LangID.getStringByID("bot.command.timeLimit", lang).replace("_", DataToString.df.format((System.currentTimeMillis() - time) / 1000.0))).queue();
                        }
                    } else if (StaticStore.timeLimit.containsKey(u.getId())) {
                        EntityHandler.generateEnemyImage(e, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

                        StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_ENEMYIMAGE_ID, System.currentTimeMillis());
                    } else {
                        EntityHandler.generateEnemyImage(e, ch, getAuthorMessage(), mode, frame, transparent, debug, lang);

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

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return enemy.size();
    }
}

package mandarin.packpack.commands.server;

import common.io.assets.UpdateCheck;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Image;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.AnimMixer;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class BoosterEmoji extends ConstraintCommand {
    public BoosterEmoji(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && temp.mkdirs()) {
            System.out.println("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        Guild g = getGuild(event).block();
        MessageChannel ch = getChannel(event);
        Message me = getMessage(event);

        if(ch == null || g == null || me == null)
            return;

        IDHolder holder = StaticStore.idHolder.get(g.getId().asString());

        if(holder.BOOSTER == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_norole", lang));
            return;
        }

        String id = getID(getContent(event));

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorole_invalidid", lang));
            return;
        }

        String name = getEmojiName(getContent(event));

        if(name == null || name.isBlank()) {
            createMessageWithNoPings(ch, LangID.getStringByID("booemo_noname", lang));
            return;
        }

        if(me.getAttachments().isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("booemo_nopng", lang));
            return;
        }

        g.getMemberById(Snowflake.of(id)).subscribe(m -> {
            try {
                if(!m.getRoleIds().contains(Snowflake.of(holder.BOOSTER))) {
                    createMessageWithNoPings(ch, LangID.getStringByID("boorole_noboost", lang).replace("_RRR_", holder.BOOSTER));
                    return;
                }

                if(StaticStore.boosterData.containsKey(g.getId().asString())) {
                    BoosterHolder bHolder = StaticStore.boosterData.get(g.getId().asString());

                    if(bHolder.serverBooster.containsKey(m.getId().asString())) {
                        BoosterData data = bHolder.serverBooster.get(m.getId().asString());

                        if(data.getRole() != null) {
                            createMessageWithNoPings(ch, LangID.getStringByID("booemo_already", lang));
                            return;
                        }
                    }
                }

                String emojiUrl = null;
                boolean gif = false;

                for(Attachment att : me.getAttachments()) {
                    if(att.getData().filename().endsWith(".png")) {
                        if(att.getData().size() >= 256 * 1024) {
                            createMessageWithNoPings(ch, LangID.getStringByID("booemo_bigpng", lang));
                            return;
                        } else {
                            Message mes = ch.createMessage(mess -> {
                                mess.setAllowedMentions(AllowedMentions.builder().build());
                                mess.setContent(LangID.getStringByID("booemo_down", lang));
                            }).block();

                            if(mes == null)
                                return;

                            String url = att.getUrl();

                            String fileName = StaticStore.findFileName(temp, StaticStore.extractFileName(att.getData().filename()), ".png");

                            File target = new File("./temp", fileName);
                            File tempo = new File("./temp", fileName+".tmp");

                            UpdateCheck.Downloader down = new UpdateCheck.Downloader(target, tempo, "", false, url);

                            AtomicReference<Long> currentTime = new AtomicReference<>(System.currentTimeMillis());

                            down.run(p -> {
                                long current = System.currentTimeMillis();

                                if(current - currentTime.get() > 1500) {
                                    currentTime.set(current);

                                    mes.edit(mess -> mess.setContent(LangID.getStringByID("booemo_down", lang).replace("-", DataToString.df.format(p * 100)))).subscribe();
                                }
                            });

                            mes.delete().subscribe();

                            if(target.exists() && !AnimMixer.validPng(target)) {
                                createMessageWithNoPings(ch, LangID.getStringByID("booemo_invpng", lang));

                                return;
                            } else if(!target.exists()) {
                                createMessageWithNoPings(ch, LangID.getStringByID("booemo_faildown", lang));

                                return;
                            } else {
                                emojiUrl = att.getUrl();

                                if(!target.delete()) {
                                    System.out.println("Can't delete file : "+target.getAbsolutePath());
                                }
                            }
                        }

                        break;
                    } else if(att.getData().filename().endsWith(".gif")) {
                        emojiUrl = att.getUrl();
                        gif = true;
                    }
                }

                if(emojiUrl == null) {
                    createMessageWithNoPings(ch, LangID.getStringByID("booemo_nopng", lang));
                    return;
                }

                Image img = Image.ofUrl(emojiUrl).block();

                if(img == null) {
                    createMessageWithNoPings(ch, LangID.getStringByID("booemo_noimg", lang));
                    return;
                }

                boolean finalGif = gif;
                g.createEmoji(e -> {
                    e.setName(name);
                    e.setImage(img);
                }).subscribe(e -> {
                    if(StaticStore.boosterData.containsKey(g.getId().asString())) {
                        BoosterHolder bHolder = StaticStore.boosterData.get(g.getId().asString());

                        if(bHolder.serverBooster.containsKey(m.getId().asString())) {
                            BoosterData data = bHolder.serverBooster.get(m.getId().asString());

                            int result = data.setEmoji(e.getId().asString());

                            if(result == BoosterData.ERR_ALREADY_EMOJI_SET) {
                                createMessageWithNoPings(ch, LangID.getStringByID("booemo_already", lang));
                            } else {
                                createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "booemo_gifsuccess" : "booemo_success", lang).replace("_III_", e.getId().asString()).replace("_MMM_", m.getId().asString()).replace("_EEE_", name));
                            }
                        } else {
                            BoosterData data = new BoosterData(e.getId().asString(), BoosterData.INITIAL.EMOJI);

                            bHolder.serverBooster.put(m.getId().asString(), data);

                            createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "booemo_gifsuccess" : "booemo_success", lang).replace("_III_", e.getId().asString()).replace("_MMM_", m.getId().asString()).replace("_EEE_", name));
                        }
                    } else {
                        BoosterHolder bHolder = new BoosterHolder();

                        BoosterData data = new BoosterData(e.getId().asString(), BoosterData.INITIAL.EMOJI);

                        bHolder.serverBooster.put(m.getId().asString(), data);

                        StaticStore.boosterData.put(g.getId().asString(), bHolder);

                        createMessageWithNoPings(ch, LangID.getStringByID(finalGif ? "booemo_gifsuccess" : "booemo_success", lang).replace("_III_", e.getId().asString()).replace("_MMM_", m.getId().asString()).replace("_EEE_", name));
                    }
                }, e -> {
                    e.printStackTrace();

                    createMessageWithNoPings(ch, LangID.getStringByID("booemo_fail", lang));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String getID(String message) {
        String[] content = message.split(" ");

        if(content.length < 2)
            return null;

        String id = content[1].replaceAll("<@!?", "").replace(">", "");

        if(StaticStore.isNumeric(id))
            return id;
        else
            return null;
    }

    private String getEmojiName(String message) {
        String[] content = message.split(" ", 3);

        if(content.length < 3)
            return null;

        if(content[2].isBlank())
            return null;

        return content[2].strip();
    }
}

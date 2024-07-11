package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.Data;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Castle extends ConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, CastleImg cs) throws Exception {
        Interaction interaction = event.getInteraction();

        CommonStatic.Lang.Locale lang = CommonStatic.Lang.Locale.EN;

        User u = interaction.getUser();

        if(StaticStore.config.containsKey(u.getId())) {
            lang = StaticStore.config.get(u.getId()).lang;

            if (lang == null)
                lang = CommonStatic.Lang.Locale.EN;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());

            return;
        }

        File img = StaticStore.generateTempFile(temp, "castle", ".png", false);

        if(img == null) {
            return;
        }

        if(cs != null) {
            int code = switch (cs.getCont().getSID()) {
                case "000001" -> 1;
                case "000002" -> 2;
                case "000003" -> 3;
                default -> 0;
            };

            FakeImage castle;

            if (code == 1 && lang != CommonStatic.Lang.Locale.JP) {
                VFile vf = VFile.get("./org/img/ec/ec" + Data.trio(cs.id.id) + "_" + getLocale(lang) + ".png");

                if (vf != null) {
                    castle = vf.getData().getImg();
                } else {
                    castle = cs.img.getImg();
                }
            } else {
                castle = cs.img.getImg();
            }

            CountDownLatch waiter = new CountDownLatch(1);

            StaticStore.renderManager.createRenderer(castle.getWidth(), castle.getHeight(), temp, connector -> {
                connector.queue(g -> {
                    g.drawImage(castle, 0f, 0f);

                    return null;
                });

                return null;
            }, progress -> img, () -> {
                waiter.countDown();

                return null;
            });

            waiter.await();

            String castleCode;

            if (code == 0)
                castleCode = "RC";
            else if (code == 1)
                castleCode = "EC";
            else if (code == 2)
                castleCode = "WC";
            else
                castleCode = "SC";

            event.deferReply()
                    .setAllowedMentions(new ArrayList<>())
                    .setContent(LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(cs.id.id)).replace("_BBB_", String.valueOf(cs.boss_spawn)))
                    .addFiles(FileUpload.fromData(img, "result.png"))
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/Castle::performButton - Failed to upload castle image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    private static String getLocale(CommonStatic.Lang.Locale lang) {
        return switch (lang) {
            case KR -> "ko";
            case ZH -> "tw";
            default -> "en";
        };
    }

    private static final int PARAM_RC = 2;
    private static final int PARAM_EC = 4;
    private static final int PARAM_WC = 8;
    private static final int PARAM_SC = 16;

    private CastleImg cs;

    public Castle(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    public Castle(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, CastleImg cs) {
        super(role, lang, id, false);

        this.cs = cs;
    }

    private int startIndex = 1;

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File img = StaticStore.generateTempFile(temp, "castle", ".png", false);

        if(img == null) {
            return;
        }

        if(cs != null) {
            int code = switch (cs.getCont().getSID()) {
                case "000001" -> 1;
                case "000002" -> 2;
                case "000003" -> 3;
                default -> 0;
            };

            FakeImage castle;

            if(code == 1 && lang != CommonStatic.Lang.Locale.JP) {
                VFile vf = VFile.get("./org/img/ec/ec"+Data.trio(cs.id.id)+"_"+getLocale(lang)+".png");

                if(vf != null) {
                    castle = vf.getData().getImg();
                } else {
                    castle = cs.img.getImg();
                }
            } else {
                castle = cs.img.getImg();
            }

            CountDownLatch waiter = new CountDownLatch(1);

            StaticStore.renderManager.createRenderer(castle.getWidth(), castle.getHeight(), temp, connector -> {
                connector.queue(g -> {
                    g.drawImage(castle, 0f, 0f);

                    return null;
                });

                return null;
            }, progress -> img, () -> {
                waiter.countDown();

                return null;
            });

            waiter.await();

            int finalId = cs.id.id;

            String castleCode;

            if(code == 0)
                castleCode = "RC";
            else if(code == 1)
                castleCode = "EC";
            else if(code == 2)
                castleCode = "WC";
            else
                castleCode = "SC";

            sendMessageWithFile(ch, LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(finalId)).replace("_BBB_", String.valueOf(cs.boss_spawn)), img, "result.png", loader.getMessage());
        } else {
            String[] list = loader.getContent().split(" ");

            if(list.length >= 2) {
                int param = checkParameters(loader.getContent());

                String[] messages = loader.getContent().split(" ", startIndex+1);

                if(messages.length <= startIndex) {
                    replyToMessageSafely(ch, LangID.getStringByID("castle_more", lang).replace("_", holder == null ? StaticStore.globalPrefix : holder.config.prefix), loader.getMessage(), a -> a);

                    return;
                }

                String msg = messages[startIndex];

                int id;

                if(StaticStore.isNumeric(msg)) {
                    id = StaticStore.safeParseInt(msg);
                } else {
                    replyToMessageSafely(ch, LangID.getStringByID("castle_number", lang), loader.getMessage(), a -> a);

                    return;
                }

                ArrayList<CastleList> castleLists = new ArrayList<>(CastleList.defset());

                List<CastleImg> imgs;
                int code;

                if((param & PARAM_EC) > 0) {
                    imgs = castleLists.get(1).getList();
                    code = 1;
                } else if((param & PARAM_WC) > 0) {
                    imgs = castleLists.get(2).getList();
                    code = 2;
                } else if((param & PARAM_SC) > 0) {
                    imgs = castleLists.get(3).getList();
                    code = 3;
                } else {
                    imgs = castleLists.getFirst().getList();
                    code = 0;
                }

                if(id >= imgs.size())
                    id = imgs.size() - 1;

                if(id < 0)
                    id = 0;

                CastleImg image = imgs.get(id);

                FakeImage castle;

                if(code == 1 && lang != CommonStatic.Lang.Locale.JP) {
                    VFile vf = VFile.get("./org/img/ec/ec"+Data.trio(image.id.id)+"_"+getLocale(lang)+".png");

                    if(vf != null) {
                        castle = vf.getData().getImg();
                    } else {
                        castle = image.img.getImg();
                    }
                } else {
                    castle = image.img.getImg();
                }

                CountDownLatch waiter = new CountDownLatch(1);

                StaticStore.renderManager.createRenderer(castle.getWidth(), castle.getHeight(), temp, connector -> {
                    connector.queue(g -> {
                        g.drawImage(castle, 0f, 0f);

                        return null;
                    });

                    return null;
                }, progress -> img, () -> {
                    waiter.countDown();

                    return null;
                });

                waiter.await();

                String castleCode;

                if(code == 0)
                    castleCode = "RC";
                else if(code == 1)
                    castleCode = "EC";
                else if(code == 2)
                    castleCode = "WC";
                else
                    castleCode = "SC";

                sendMessageWithFile(ch, LangID.getStringByID("castle_result", lang).replace("_CCC_", castleCode).replace("_III_", Data.trio(id)).replace("_BBB_", String.valueOf(image.boss_spawn)), img, "result.png", loader.getMessage());
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("castle_argu", lang).replace("_", holder == null ? StaticStore.globalPrefix : holder.config.prefix), loader.getMessage(), a -> a);
            }
        }
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result =  1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-rc")) {
                    if((result & PARAM_RC) == 0) {
                        result |= PARAM_RC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-ec")) {
                    if((result & PARAM_EC) == 0) {
                        result |= PARAM_EC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-wc")) {
                    if((result & PARAM_WC) == 0) {
                        result |= PARAM_WC;
                        startIndex++;
                    }

                    break;
                } else if(str.startsWith("-sc")) {
                    if((result & PARAM_SC) == 0) {
                        result |= PARAM_SC;
                        startIndex++;
                    }

                    break;
                } else {
                    break;
                }
            }
        }

        return result;
    }
}

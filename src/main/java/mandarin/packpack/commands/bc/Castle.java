package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.Data;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import kotlin.Unit;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class Castle extends ConstraintCommand {
    public static void performButton(ButtonInteractionEvent event, CastleImg cs) throws Exception {
        Interaction interaction = event.getInteraction();

        CommonStatic.Lang.Locale lang = CommonStatic.Lang.Locale.EN;

        User u = interaction.getUser();

        if (StaticStore.config.containsKey(u.getId())) {
            lang = StaticStore.config.get(u.getId()).lang;

            if (lang == null)
                lang = CommonStatic.Lang.Locale.EN;
        }

        File temp = new File("./temp");

        if (!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : " + temp.getAbsolutePath());

            return;
        }

        File img = StaticStore.generateTempFile(temp, "castle", ".png", false);

        if (img == null) {
            return;
        }

        if (cs != null) {
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

                    return Unit.INSTANCE;
                });

                return Unit.INSTANCE;
            }, progress -> img, () -> {
                waiter.countDown();

                return Unit.INSTANCE;
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

            List<ContainerChildComponent> children = new ArrayList<>();

            children.add(TextDisplay.of(LangID.getStringByID("castle.result.title", lang)));

            children.add(Separator.create(true, Separator.Spacing.LARGE));

            children.add(TextDisplay.of(
                    LangID.getStringByID("castle.result.id", lang).formatted(castleCode, Data.trio(cs.id.id)) + "\n" +
                            LangID.getStringByID("castle.result.boss", lang).formatted(cs.boss_spawn)
            ));

            children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

            Container container = Container.of(children);

            event.deferReply()
                    .setComponents(container)
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue(m -> StaticStore.deleteFile(img, true), e -> {
                        StaticStore.deleteFile(img, true);

                        StaticStore.logger.uploadErrorLog(e, "E/Castle::performButton - Failed to send castle message");
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

    public Castle(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    private int startIndex = 1;

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        File temp = new File("./temp");

        if (!temp.exists()) {
            boolean res = temp.mkdirs();

            if (!res) {
                System.out.println("Can't create folder : " + temp.getAbsolutePath());
                return;
            }
        }

        File img = StaticStore.generateTempFile(temp, "castle", ".png", false);

        if (img == null) {
            return;
        }

        int id;
        int code;

        if (loader.fromMessage) {
            String[] list = loader.getContent().split(" ");

            if (list.length < 2) {
                replyToMessageSafely(loader.getChannel(), loader.getMessage(), TextDisplay.of(LangID.getStringByID("castle.fail.noParameter", lang)));

                return;
            }

            int param = checkParameters(loader.getContent());

            String[] messages = loader.getContent().split(" ", startIndex + 1);

            if (messages.length <= startIndex) {
                replyToMessageSafely(loader.getChannel(), loader.getMessage(), TextDisplay.of(LangID.getStringByID("castle.fail.noID", lang).formatted(holder == null ? StaticStore.globalPrefix : holder.config.prefix)));

                return;
            }

            String msg = messages[startIndex];

            if (StaticStore.isNumeric(msg)) {
                id = StaticStore.safeParseInt(msg);
            } else {
                replyToMessageSafely(loader.getChannel(), loader.getMessage(), TextDisplay.of(LangID.getStringByID("castle.fail.notNumber", lang)));

                return;
            }

            if ((param & PARAM_EC) > 0) {
                code = 1;
            } else if ((param & PARAM_WC) > 0) {
                code = 2;
            } else if ((param & PARAM_SC) > 0) {
                code = 3;
            } else {
                code = 0;
            }
        } else {
            id = loader.getOptions().getOption("id", 0);
            code = switch (loader.getOptions().getOption("type", "").toUpperCase(Locale.ENGLISH)) {
                case "EC" -> 1;
                case "WC" -> 2;
                case "SC" -> 3;
                default -> 0;
            };
        }

        List<CastleImg> images = new ArrayList<>(CastleList.defset()).get(code).getList();

        if (id >= images.size())
            id = images.size() - 1;

        if (id < 0)
            id = 0;

        CastleImg castle = images.get(id);

        FakeImage castleImage;

        if (code == 1 && lang != CommonStatic.Lang.Locale.JP) {
            VFile vf = VFile.get("./org/img/ec/ec" + Data.trio(castle.id.id) + "_" + getLocale(lang) + ".png");

            if (vf != null) {
                castleImage = vf.getData().getImg();
            } else {
                castleImage = castle.img.getImg();
            }
        } else {
            castleImage = castle.img.getImg();
        }

        CountDownLatch waiter = new CountDownLatch(1);

        StaticStore.renderManager.createRenderer(castleImage.getWidth(), castleImage.getHeight(), temp, connector -> {
            connector.queue(g -> {
                g.drawImage(castleImage, 0f, 0f);

                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        }, progress -> img, () -> {
            waiter.countDown();

            return Unit.INSTANCE;
        });

        waiter.await();

        String castleCode = switch(code) {
            case 1 -> "EC";
            case 2 -> "WC";
            case 3 -> "SC";
            default -> "RC";
        };

        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of(LangID.getStringByID("castle.result.title", lang)));

        children.add(Separator.create(true, Separator.Spacing.LARGE));

        children.add(TextDisplay.of(
                LangID.getStringByID("castle.result.id", lang).formatted(castleCode, Data.trio(castle.id.id)) + "\n" +
                        LangID.getStringByID("castle.result.boss", lang).formatted(castle.boss_spawn)
        ));

        children.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(img))));

        Container container = Container.of(children);

        if (loader.fromMessage) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), msg -> StaticStore.deleteFile(img, true), e -> {
                StaticStore.deleteFile(img, true);

                StaticStore.logger.uploadErrorLog(e, "Castle::doSomething - Failed to upload castle image");
            }, container);
        } else {
            loader.getInteractionEvent().deferReply()
                    .setComponents(container)
                    .useComponentsV2()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue(m -> StaticStore.deleteFile(img, true), e -> {
                        StaticStore.deleteFile(img, true);

                        StaticStore.logger.uploadErrorLog(e, "E/Castle::performButton - Failed to send castle message");
                    });
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

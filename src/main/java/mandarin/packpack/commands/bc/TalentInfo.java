package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.component.search.TalentMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TalentInfo extends ConstraintCommand {
    private final ConfigHolder config;

    public TalentInfo(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id, false);

        if(config == null) {
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        } else {
            this.config = config;
        }
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        boolean isFrame;
        String name;

        if (loader.fromMessage) {
            name = filterCommand(loader.getContent());
            isFrame = isFrame(loader.getContent());
        } else {
            name = loader.getOptions().getOption("name", "");
            isFrame = loader.getOptions().getOption("frame", config.useFrame);
        }

        if(name.isBlank()) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("formStat.fail.noName", lang));
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("formStat.fail.noName", lang));
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, true, lang);

            if (forms.size() == 1) {
                Form f = forms.getFirst();

                if(f.unit.forms.length < 3) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("talentInfo.failed.noTrueForm", lang));

                    return;
                }

                Form trueForm = f.unit.forms[2];

                if(trueForm.du == null || trueForm.du.getPCoin() == null) {
                    if (loader.fromMessage) {
                        replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("talentInfo.failed.noTalent", lang));
                    } else {
                        replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("talentInfo.failed.noTalent", lang));
                    }

                    return;
                }

                Object sender;

                if (loader.fromMessage) {
                    sender = ch;
                } else {
                    sender = loader.getInteractionEvent();
                }

                EntityHandler.generateTalentEmbed(sender, loader.getNullableMessage(), trueForm, isFrame, false, false, lang);
            } else if (forms.isEmpty()) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("formStat.fail.noUnit", lang).formatted(getSearchKeyword(name)));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("formStat.fail.noUnit", lang).formatted(getSearchKeyword(name)));
                }
            } else {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, loader.getMessage(), msg -> {
                        User u = loader.getUser();

                        StaticStore.putHolder(u.getId(), new TalentMessageHolder(loader.getMessage(), u.getId(), ch.getId(), msg, name, config.searchLayout, forms, isFrame, lang));
                    }, getSearchComponents(forms.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(getSearchKeyword(name), forms.size()), forms, this::accumulateTextData, config.searchLayout, lang));
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), msg -> {
                        User u = loader.getUser();

                        StaticStore.putHolder(u.getId(), new TalentMessageHolder(loader.getNullableMessage(), u.getId(), ch.getId(), msg, name, config.searchLayout, forms, isFrame, lang));
                    }, getSearchComponents(forms.size(), LangID.getStringByID("ui.search.severalResult", lang).formatted(getSearchKeyword(name), forms.size()), forms, this::accumulateTextData, config.searchLayout, lang));
                }
            }
        }
    }

    private boolean isFrame(String message) {
        String[] msg = message.split(" ");

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.equals("-s"))
                    return false;
                else if (str.equals("-f") || str.equals("-fr") || str.equals("-frame"))
                    return true;
            }
        }

        return true;
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;
        boolean isFrame = false;

        StringBuilder command = new StringBuilder();

        for(int i = 1; i < content.length; i++) {
            boolean written = false;

            if ("-s".equals(content[i]) || "-second".equals(content[i])) {
                if (!isSec)
                    isSec = true;
                else {
                    command.append(content[i]);
                    written = true;
                }
            } else if ("-f".equals(content[i]) || "-fr".equals(content[i]) || "-frame".equals(content[i])) {
                if (!isFrame)
                    isFrame = true;
                else {
                    command.append(content[i]);
                    written = true;
                }
            } else {
                command.append(content[i]);
                written = true;
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

    private List<String> accumulateTextData(List<Form> forms, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(forms.size(), config.searchLayout.chunkSize); i++) {
            Form f = forms.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + " ";

                        if (StaticStore.safeMultiLangGet(f, lang) != null) {
                            text += StaticStore.safeMultiLangGet(f, lang);
                        }
                    } else {
                        text = "`" + Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + "` ";

                        String formName = StaticStore.safeMultiLangGet(f, lang);

                        if (formName == null || formName.isBlank()) {
                            formName = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                        }

                        text += "**" + formName + "**";
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(f, lang);

                    if (text == null) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
            }

            data.add(text);
        }

        return data;
    }

    private String getSearchKeyword(String name) {
        String result = name;

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

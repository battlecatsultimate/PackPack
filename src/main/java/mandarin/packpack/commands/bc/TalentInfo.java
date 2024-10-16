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
                replyToMessageSafely(ch, LangID.getStringByID("formStat.fail.noName", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("formStat.fail.noName", lang), a -> a);
            }
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(name, true, lang);

            if (forms.size() == 1) {
                Form f = forms.getFirst();

                if(f.unit.forms.length < 3) {
                    createMessageWithNoPings(ch, LangID.getStringByID("talentInfo.failed.noTrueForm", lang));

                    return;
                }

                Form trueForm = f.unit.forms[2];

                if(trueForm.du == null || trueForm.du.getPCoin() == null) {
                    if (loader.fromMessage) {
                        replyToMessageSafely(ch, LangID.getStringByID("talentInfo.failed.noTalent", lang), loader.getMessage(), a -> a);
                    } else {
                        replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("talentInfo.failed.noTalent", lang), a -> a);
                    }

                    return;
                }

                Object sender;

                if (loader.fromMessage) {
                    sender = ch;
                } else {
                    sender = loader.getInteractionEvent();
                }

                EntityHandler.showTalentEmbed(sender, loader.getNullableMessage(), trueForm, isFrame, false, lang);
            } else if (forms.isEmpty()) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, LangID.getStringByID("formStat.fail.noUnit", lang).replace("_", getSearchKeyword(name)), loader.getMessage(), a -> a);
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("formStat.fail.noUnit", lang).replace("_", getSearchKeyword(name)), a -> a);
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(name)));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateListData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                if (loader.fromMessage) {
                    replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                        User u = loader.getUser();

                        StaticStore.putHolder(u.getId(), new TalentMessageHolder(loader.getMessage(), u.getId(), ch.getId(), res, forms, isFrame, lang));
                    });
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), sb.toString(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                        User u = loader.getUser();

                        StaticStore.putHolder(u.getId(), new TalentMessageHolder(loader.getNullableMessage(), u.getId(), ch.getId(), res, forms, isFrame, lang));
                    });
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

    private String getSearchKeyword(String name) {
        String result = name;

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

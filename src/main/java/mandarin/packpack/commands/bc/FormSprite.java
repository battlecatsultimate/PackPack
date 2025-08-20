package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.FormSpriteMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FormSprite extends TimedConstraintCommand {
    private static final int PARAM_UNI = 2;
    private static final int PARAM_UDI = 4;
    private static final int PARAM_EDI = 8;

    private final ConfigHolder config;

    public FormSprite(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_FORMSPRITE_ID, false);

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

        if(contents.length == 1) {
            replyToMessageSafely(ch, LangID.getStringByID("formImage.fail.noParameter", lang), loader.getMessage(), a -> a);
        } else {
            String search = filterCommand(loader.getContent());

            if(search.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("formImage.fail.noParameter", lang), loader.getMessage(), a -> a);
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search, false, lang);

            if(forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formStat.fail.noUnit", lang).replace("_", getSearchKeyword(loader.getContent())), loader.getMessage(), a -> a);
                disableTimer();
            } else if(forms.size() == 1) {
                int param = checkParameter(loader.getContent());

                EntityHandler.generateUnitSprite(forms.getFirst(), ch, loader.getMessage(), getModeFromParam(param), lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(loader.getContent())));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                    int totalPage = forms.size() / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

                    if(forms.size() % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                int param = checkParameter(loader.getContent());

                int mode = getModeFromParam(param);

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, forms.size(), data, lang), res -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new FormSpriteMessageHolder(forms, msg, u.getId(), ch.getId(), res, search, config.searchLayout, mode, lang));
                });

                disableTimer();
            }
        }
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        label:
        for (String content : contents) {
            switch (content) {
                case "-uni":
                    res |= PARAM_UNI;
                    break label;
                case "-udi":
                    res |= PARAM_UDI;
                    break label;
                case "-edi":
                    res |= PARAM_EDI;
                    break label;
            }
        }

        return res;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean uni = false;
        boolean udi = false;
        boolean edi = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-uni" -> {
                    if (!uni) {
                        uni = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-udi" -> {
                    if (!udi) {
                        udi = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                case "-edi" -> {
                    if (!edi) {
                        edi = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                }
                default -> {
                    result.append(contents[i]);
                    written = true;
                }
            }

            if(written && i < contents.length - 1)
                result.append(" ");
        }

        return result.toString().trim();
    }

    private int getModeFromParam(int param) {
        if((param & PARAM_UNI) > 0)
            return 1;
        if((param & PARAM_UDI) > 0)
            return 2;
        if((param & PARAM_EDI) > 0)
            return 3;
        else
            return 0;
    }

    private List<String> accumulateData(List<Form> forms) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
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

    private String getSearchKeyword(String command) {
        String result = filterCommand(command);

        if(result == null)
            return "";

        if(result.length() > 1500)
            result = result.substring(0, 1500) + "...";

        return result;
    }
}

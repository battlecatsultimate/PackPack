package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import mandarin.packpack.supporter.server.holder.TalentMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class TalentInfo extends ConstraintCommand {
    private final ConfigHolder config;

    public TalentInfo(ROLE role, int lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id);

        if(config == null) {
            this.config = holder.config;
        } else {
            this.config = config;
        }
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        String[] list = getContent(event).split(" ",2);

        if(list.length == 1 || filterCommand(getContent(event)).isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("formst_noname", lang), getMessage(event), a -> a);
        } else {
            ArrayList<Form> forms = EntityFilter.findUnitWithName(filterCommand(getContent(event)), lang);

            if (forms.size() == 1) {
                boolean isFrame = isFrame(getContent(event)) && config.useFrame;

                Form f = forms.get(0);

                if(f.unit.forms.length < 3) {
                    createMessageWithNoPings(ch, LangID.getStringByID("talentinfo_notf", lang));

                    return;
                }

                Form trueForm = f.unit.forms[2];

                if(trueForm.du == null || trueForm.du.getPCoin() == null) {
                    replyToMessageSafely(ch, LangID.getStringByID("talentinfo_notal", lang), getMessage(event), a -> a);

                    return;
                }

                EntityHandler.showTalentEmbed(ch, getMessage(event), trueForm, isFrame, lang);
            } else if (forms.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("formst_nounit", lang).replace("_", filterCommand(getContent(event))), getMessage(event), a -> a);
            } else {
                boolean isFrame = isFrame(getContent(event)) && config.useFrame;

                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getContent(event))));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateListData(forms);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(forms.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = forms.size() / SearchHolder.PAGE_CHUNK;

                    if(forms.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, forms.size(), data, lang));

                if(res != null) {
                    Member m = getMember(event);

                    StaticStore.putHolder(m.getId(), new TalentMessageHolder(res, getMessage(event), ch.getId(), forms, isFrame, lang));
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
            }
        }

        return true;
    }

    private String filterCommand(String msg) {
        String[] content = msg.split(" ");

        boolean isSec = false;

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
}

package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;
import mandarin.packpack.supporter.server.MedalHolder;

import java.util.ArrayList;

public class Medal extends ConstraintCommand {
    public Medal(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length >= 2) {
            String[] realContents = getContent(event).split(" ", 2);

            ArrayList<Integer> id = EntityFilter.findMedalByName(realContents[1], lang);

            if(id.isEmpty()) {
                ch.createMessage(LangID.getStringByID("medal_nomed", lang).replace("_", realContents[1])).subscribe();
            } else if(id.size() == 1) {
                EntityHandler.showMedalEmbed(id.get(0), ch, lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", realContents[1]));

                String check;

                if(id.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                sb.append("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= id.size())
                        break;

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String medalName = Data.trio(id.get(i)) + " " + StaticStore.MEDNAME.getCont(id.get(i));

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(medalName).append("\n");
                }

                if(id.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(id.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                Message res = ch.createMessage(sb.toString()).block();

                if(res != null) {
                    getMember(event).ifPresent(m -> {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(m.getId().asString(), new MedalHolder(id, msg, res, lang, ch.getId().asString()));
                    });
                }
            }
        } else {
            ch.createMessage(LangID.getStringByID("medal_more", lang)).subscribe();
        }
    }
}

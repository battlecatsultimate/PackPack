 package mandarin.packpack.supporter.server.holder;

 import common.CommonStatic;
 import common.util.Data;
 import common.util.lang.MultiLangCont;
 import common.util.unit.Enemy;
 import discord4j.core.event.domain.message.MessageCreateEvent;
 import discord4j.core.object.entity.Message;
 import discord4j.core.object.entity.channel.MessageChannel;
 import mandarin.packpack.commands.Command;
 import mandarin.packpack.supporter.StaticStore;
 import mandarin.packpack.supporter.bc.EntityHandler;
 import mandarin.packpack.supporter.lang.LangID;
 import mandarin.packpack.supporter.server.data.ConfigHolder;

 import java.util.ArrayList;

 public class EnemyStatMessageHolder extends MessageHolder<MessageCreateEvent> {
    private final ArrayList<Enemy> enemy;
    private final Message msg;
    private final String channelID;

     private int page = 0;

    private final boolean isFrame;
    private final boolean isExtra;
    private final int[] magnification;
    private final int lang;

    private final ArrayList<Message> cleaner = new ArrayList<>();

    public EnemyStatMessageHolder(ArrayList<Enemy> enemy, Message author, Message msg, String channelID, int[] magnification, boolean isFrame, boolean isExtra, int lang) {
        super(MessageCreateEvent.class);

        this.enemy = enemy;
        this.msg = msg;
        this.channelID = channelID;

        this.magnification = magnification;
        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.lang = lang;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public int handleEvent(MessageCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getMessage().getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        String content = event.getMessage().getContent();

        if(content.equals("n")) {
            if(20 * (page + 1) >= enemy.size())
                return RESULT_STILL;

            page++;

            Command.editMessage(msg, m -> {
                String check;

                if(enemy.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= enemy.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                    if(i >= enemy.size())
                        break;

                    Enemy e = enemy.get(i);

                    String ename = Data.trio(e.id.id)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(e) != null)
                        ename += MultiLangCont.get(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(ename).append("\n");
                }

                if(enemy.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.content(wrap(sb.toString()));
            });

            cleaner.add(event.getMessage());
        } else if(content.equals("p")) {
            if(page == 0)
                return RESULT_STILL;

            page--;

            Command.editMessage(msg, m -> {
                String check;

                if(enemy.size() <= 20)
                    check = "";
                else if(page == 0)
                    check = LangID.getStringByID("formst_next", lang);
                else if((page + 1) * 20 >= enemy.size())
                    check = LangID.getStringByID("formst_pre", lang);
                else
                    check = LangID.getStringByID("formst_nexpre", lang);

                StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                    if(i >= enemy.size())
                        break;

                    Enemy e = enemy.get(i);

                    String ename = Data.trio(e.id.id)+" ";

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    if(MultiLangCont.get(e) != null)
                        ename += MultiLangCont.get(e);

                    CommonStatic.getConfig().lang = oldConfig;

                    sb.append(i+1).append(". ").append(ename).append("\n");
                }

                if(enemy.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                m.content(wrap(sb.toString()));
            });

            cleaner.add(event.getMessage());
        } else if(StaticStore.isNumeric(content)) {
            int id = StaticStore.safeParseInt(content)-1;

            if(id < 0 || id >= enemy.size())
                return RESULT_STILL;

            msg.delete().subscribe();

            try {
                EntityHandler.showEnemyEmb(enemy.get(id), ch, isFrame, isExtra, magnification, lang);
            } catch (Exception e) {
                e.printStackTrace();
            }

            expired = true;

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.equals("c")) {
            Command.editMessage(msg, m -> {
                m.content(wrap(LangID.getStringByID("formst_cancel" ,lang)));
                expired = true;
            });

            cleaner.add(event.getMessage());

            return RESULT_FINISH;
        } else if(content.startsWith("n ")) {
            String[] contents = content.split(" ");

            if(contents.length == 2) {
                if (StaticStore.isNumeric(contents[1])) {
                    int p = StaticStore.safeParseInt(contents[1]) - 1;

                    if (p < 0 || p * 20 >= enemy.size()) {
                        return RESULT_STILL;
                    }

                    page = p;

                    Command.editMessage(msg, m -> {
                        String check;

                        if(enemy.size() <= 20)
                            check = "";
                        else if(page == 0)
                            check = LangID.getStringByID("formst_next", lang);
                        else if((page + 1) * 20 >= enemy.size())
                            check = LangID.getStringByID("formst_pre", lang);
                        else
                            check = LangID.getStringByID("formst_nexpre", lang);

                        StringBuilder sb = new StringBuilder("```md\n").append(LangID.getStringByID("formst_pick", lang)).append(check);

                        for(int i = 20 * page; i < 20 * (page + 1) ; i++) {
                            if(i >= enemy.size())
                                break;

                            Enemy e = enemy.get(i);

                            String ename = Data.trio(e.id.id)+" ";

                            int oldConfig = CommonStatic.getConfig().lang;
                            CommonStatic.getConfig().lang = lang;

                            if(MultiLangCont.get(e) != null)
                                ename += MultiLangCont.get(e);

                            CommonStatic.getConfig().lang = oldConfig;

                            sb.append(i+1).append(". ").append(ename).append("\n");
                        }

                        if(enemy.size() > 20)
                            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page+1)).replace("-", String.valueOf(enemy.size()/20 + 1)));

                        sb.append(LangID.getStringByID("formst_can", lang));
                        sb.append("```");

                        m.content(wrap(sb.toString()));
                    });

                    cleaner.add(event.getMessage());
                }
            }
        }

        return RESULT_STILL;
    }

    @Override
    public void clean() {
        for(Message m : cleaner) {
            if(m != null)
                m.delete().subscribe();
        }
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));
    }
}

package mandarin.packpack.supporter.bc;

import com.google.gson.JsonObject;
import common.CommonStatic;
import common.battle.data.MaskUnit;
import common.battle.data.PCoin;
import common.pack.PackData;
import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import common.util.pack.Soul;
import common.util.stage.MapColc;
import common.util.stage.SCDef;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import common.util.stage.info.DefStageInfo;
import common.util.unit.*;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.awt.FG2D;
import mandarin.packpack.supporter.awt.FIBI;
import mandarin.packpack.supporter.bc.cell.*;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class EntityHandler {
    private static final DecimalFormat df;
    private static Font font;

    static {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        df = (DecimalFormat) nf;
        df.applyPattern("#.##");

        File fon = new File("./data/ForceFont.otf");

        try {
            font = Font.createFont(Font.TRUETYPE_FONT, fon).deriveFont(24f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Message performUnitEmb(Form f, GenericCommandInteractionEvent event, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, ArrayList<Integer> lv, int lang) throws Exception {
        ReplyCallbackAction action = event.deferReply();

        int level = lv.get(0);
        int levelp = 0;

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else {
                if(config == null)
                    level = 30;
                else
                    level = config.defLevel;
            }
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv.set(0, level + levelp);

        String l;

        if(levelp == 0)
            l = "" + level;
        else
            l = level + " + " + levelp;

        File img = generateIcon(f);
        File cf;

        if(extra)
            cf = generateCatfruit(f);
        else
            cf = null;

        EmbedBuilder spec = new EmbedBuilder();

        int c;

        if(f.fid == 0)
            c = StaticStore.rainbow[4];
        else if(f.fid == 1)
            c = StaticStore.rainbow[3];
        else
            c = StaticStore.rainbow[2];

        ArrayList<Integer> t;

        if(talent && f.du.getPCoin() != null) {
            t = new ArrayList<>(f.du.getPCoin().max);
            t.set(0, lv.get(0));
        } else
            t = null;

        if(t != null)
            t = handleTalent(lv, t);
        else {
            t = new ArrayList<>();

            t.add(lv.get(0));
        }

        spec.setTitle(DataToString.getTitle(f, lang));

        if(talent && f.du.getPCoin() != null && talentExists(t)) {
            spec.setDescription(LangID.getStringByID("data_talent", lang));
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");
        spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), true);
        spec.addField(LangID.getStringByID("data_level", lang), l, true);
        spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f.du, f.unit.lv, talent, t), true);
        spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f.du, talent, t), true);
        spec.addField(LangID.getStringByID("data_cooldown", lang), DataToString.getCD(f.du,isFrame, talent, t), true);
        spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(f.du, talent, t), true);
        spec.addField(LangID.getStringByID("data_cost", lang), DataToString.getCost(f.du, talent, t), true);
        spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(f.du), true);
        spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(f.du, isFrame), true);
        spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(f.du, lang), true);
        spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f.du, f.unit.lv, talent, t), true);
        spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(f.du, lang), true);
        spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f.du, f.unit.lv, talent, t), true);
        spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(f.du, talent, t, true, lang), true);

        MaskUnit du;

        if(f.du.getPCoin() != null)
            if(talent)
                du = f.du.getPCoin().improve(t);
            else
                du = f.du;
        else
            du = f.du;

        List<String> abis = Interpret.getAbi(du, true, lang);
        abis.addAll(Interpret.getProc(du, !isFrame, true, lang, 1.0, 1.0));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append("⦁ ").append(abis.get(i));
            else
                sb.append("⦁ ").append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data_none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(du, false, lang);
            abis.addAll(Interpret.getProc(du, !isFrame, false, lang, 1.0, 1.0));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append("⦁ ").append(abis.get(i));
                else
                    sb.append("⦁ ").append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data_ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(f, lang);

            if(explanation != null)
                spec.addField(LangID.getStringByID("data_udesc", lang), explanation, false);

            String catfruit = DataToString.getCatruitEvolve(f, lang);

            if(catfruit != null)
                spec.addField(LangID.getStringByID("data_evolve", lang), catfruit, false);

            spec.setImage("attachment://cf.png");
        }

        if(talentExists(t))
            spec.setFooter(DataToString.getTalent(f.du, t, lang), null);

        action = action.addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        if(cf != null)
            action = action.addFiles(FileUpload.fromData(cf, "cf.png"));

        InteractionHook hook = action.complete();

        if(hook != null) {
            Message msg = hook.retrieveOriginal().complete();
            MessageChannel ch = msg.getChannel();

            Guild g = msg.getGuild();

            if(ch instanceof GuildChannel && g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)) {
                if(canFirstForm(f)) {
                    msg.addReaction(EmojiStore.TWO_PREVIOUS).queue();
                }

                if(canPreviousForm(f)) {
                    msg.addReaction(EmojiStore.PREVIOUS).queue();
                }

                if(canNextForm(f)) {
                    msg.addReaction(EmojiStore.NEXT).queue();
                }

                if(canFinalForm(f)) {
                    msg.addReaction(EmojiStore.TWO_NEXT).queue();
                }
            }

            Timer timer = new Timer();

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(img != null && img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }

                    if(cf != null && cf.exists() && !cf.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+cf.getAbsolutePath());
                    }

                    timer.cancel();
                }
            };

            timer.schedule(task, 5000);

            return msg;
        }

        return null;
    }

    public static Message showUnitEmb(Form f, MessageChannel ch, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, ArrayList<Integer> lv, int lang, boolean addEmoji, boolean compact) throws Exception {
        int level = lv.get(0);
        int levelp = 0;

        if(level <= 0) {
            if(f.unit.rarity == 0)
                level = 110;
            else {
                if(config == null)
                    level = 30;
                else
                    level = config.defLevel;
            }
        }

        if(level > f.unit.max) {
            levelp = level - f.unit.max;
            level = f.unit.max;

            if(levelp > f.unit.maxp)
                levelp = f.unit.maxp;

            if(levelp < 0)
                levelp = 0;
        }

        lv.set(0, level + levelp);

        String l;

        if(levelp == 0)
            l = "" + level;
        else
            l = level + " + " + levelp;

        File img = generateIcon(f);

        File cf;

        if(extra)
            cf = generateCatfruit(f);
        else
            cf = null;

        EmbedBuilder spec = new EmbedBuilder();

        int c;

        if(f.fid == 0)
            c = StaticStore.rainbow[4];
        else if(f.fid == 1)
            c = StaticStore.rainbow[3];
        else
            c = StaticStore.rainbow[2];

        ArrayList<Integer> t;

        if(talent && f.du.getPCoin() != null) {
            t = new ArrayList<>(f.du.getPCoin().max);
            t.set(0, lv.get(0));
        } else
            t = null;

        if(t != null)
            t = handleTalent(lv, t);
        else {
            t = new ArrayList<>();

            t.add(lv.get(0));
        }

        if(talent && f.du.getPCoin() != null && talentExists(t)) {
            spec.setDescription(LangID.getStringByID("data_talent", lang));
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");

        if(compact) {
            spec.setTitle(DataToString.getCompactTitle(f, lang));

            spec.addField(LangID.getStringByID("data_level", lang), l, false);
            spec.addField(LangID.getStringByID("data_hpkb", lang), DataToString.getHealthHitback(f.du, f.unit.lv, talent, t), false);
            spec.addField(LangID.getStringByID("data_cocosp", lang), DataToString.getCostCooldownSpeed(f.du, isFrame, talent, t), true);
            spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(f.du), true);
            spec.addField(LangID.getStringByID("data_times", lang), DataToString.getCompactAtkTimings(f.du, isFrame), false);
            spec.addField(LangID.getStringByID("data_atkdps", lang).replace("_TTT_", DataToString.getSiMu(f.du, lang)), DataToString.getCompactAtk(f.du, talent, f.unit.lv, t), false);
        } else {
            spec.setTitle(DataToString.getTitle(f, lang));

            spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(f.uid.id, f.fid), true);
            spec.addField(LangID.getStringByID("data_level", lang), l, true);
            spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(f.du, f.unit.lv, talent, t), true);
            spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(f.du, talent, t), true);
            spec.addField(LangID.getStringByID("data_cooldown", lang), DataToString.getCD(f.du,isFrame, talent, t), true);
            spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(f.du, talent, t), true);
            spec.addField(LangID.getStringByID("data_cost", lang), DataToString.getCost(f.du, talent, t), true);
            spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(f.du), true);
            spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(f.du, isFrame), true);
            spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(f.du, lang), true);
            spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(f.du, f.unit.lv, talent, t), true);
            spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(f.du, lang), true);
            spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(f.du, f.unit.lv, talent, t), true);
        }

        spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(f.du, talent, t, true, lang), true);

        MaskUnit du;

        if(f.du.getPCoin() != null)
            if(talent)
                du = f.du.getPCoin().improve(t);
            else
                du = f.du;
        else
            du = f.du;

        List<String> abis = Interpret.getAbi(du, true, lang);
        abis.addAll(Interpret.getProc(du, !isFrame, true, lang, 1.0, 1.0));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append("⦁ ").append(abis.get(i));
            else
                sb.append("⦁ ").append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data_none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(du, false, lang);
            abis.addAll(Interpret.getProc(du, !isFrame, false, lang, 1.0, 1.0));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append("⦁ ").append(abis.get(i));
                else
                    sb.append("⦁ ").append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data_ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(f, lang);

            if(explanation != null)
                spec.addField(LangID.getStringByID("data_udesc", lang), explanation, false);

            String catfruit = DataToString.getCatruitEvolve(f, lang);

            if(catfruit != null)
                spec.addField(LangID.getStringByID("data_evolve", lang), catfruit, false);

            spec.setImage("attachment://cf.png");
        }

        if(talentExists(t))
            spec.setFooter(DataToString.getTalent(f.du, t, lang));

        MessageCreateAction action = ch.sendMessageEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        if(cf != null)
            action = action.addFiles(FileUpload.fromData(cf, "cf.png"));

        ArrayList<ActionComponent> components = new ArrayList<>();

        if(addEmoji) {
            if(canFirstForm(f)) {
                components.add(Button.secondary("first", LangID.getStringByID("button_firf", lang)).withEmoji(Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)));
            }

            if(canPreviousForm(f)) {
                components.add(Button.secondary("pre", LangID.getStringByID("button_pref", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)));
            }

            if(canNextForm(f)) {
                components.add(Button.secondary("next", LangID.getStringByID("button_nexf", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)));
            }

            if(canFinalForm(f)) {
                components.add(Button.secondary("final", LangID.getStringByID("button_finf", lang)).withEmoji(Emoji.fromCustom(EmojiStore.TWO_NEXT)));
            }

            if(talent && f.du.getPCoin() != null) {
                components.add(Button.secondary("talent", LangID.getStringByID("button_talent", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NP)));
            }
        }

        if(StaticStore.availableUDP.contains(f.unit.id.id)) {
            components.add(Button.link("https://thanksfeanor.pythonanywhere.com/UDP/"+Data.trio(f.unit.id.id), "UDP").withEmoji(Emoji.fromCustom(EmojiStore.UDP)));
        }

        if(!components.isEmpty()) {
            action = action.setComponents(ActionRow.of(components));
        }

        Message msg = action.complete();

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(img != null && img.exists() && !img.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                }

                if(cf != null && cf.exists() && !cf.delete()) {
                    StaticStore.logger.uploadLog("Failed to delete file : "+cf.getAbsolutePath());
                }

                timer.cancel();
            }
        };

        timer.schedule(task, 5000);

        f.anim.unload();

        return msg;
    }

    public static void showTalentEmbed(MessageChannel ch, Form unit, boolean isFrame, int lang) throws Exception {
        if(unit.du == null || unit.du.getPCoin() == null)
            throw new IllegalStateException("E/EntityHandler::showTalentEmbed - Unit which has no talent has been passed");

        ArrayList<Integer> levels = new ArrayList<>();

        for(int i = 0; i < unit.du.getPCoin().max.size(); i++) {
            levels.add(1);
        }

        MaskUnit improved = unit.du.getPCoin().improve(levels);

        EmbedBuilder spec = new EmbedBuilder();

        PCoin talent = unit.du.getPCoin();

        File img = generateIcon(unit);

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String unitName = MultiLangCont.get(unit);

        CommonStatic.getConfig().lang = oldConfig;

        if(unitName == null)
            unitName = Data.trio(unit.unit.id.id);

        spec.setTitle(LangID.getStringByID("talentinfo_title", lang).replace("_", unitName));

        for(int i = 0; i < talent.info.size(); i++) {
            String title = DataToString.getTalentTitle(unit.du, i, lang);
            String desc = DataToString.getTalentExplanation(unit.du, improved, i, isFrame, lang);

            if(title.isBlank() || desc.isBlank())
                continue;

            spec.addField(title, desc, false);
        }

        spec.setColor(StaticStore.rainbow[2]);

        if(img != null)
            spec.setThumbnail("attachment://icon.png");

        spec.setFooter(DataToString.accumulateNpCost(unit.du.getPCoin(), lang));

        MessageCreateAction action = ch.sendMessageEmbeds(spec.build())
                .setAllowedMentions(new ArrayList<>());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        action.queue();
    }

    private static ArrayList<Integer> handleTalent(ArrayList<Integer> lv, ArrayList<Integer> t) {
        ArrayList<Integer> res = new ArrayList<>(t);

        res.set(0, lv.get(0));

        for(int i = 0; i < t.size(); i++) {
            if(i >= lv.size())
                res.set(i, t.get(i));
            else
                res.set(i, Math.min(t.get(i), lv.get(i)));

            if(res.get(i) < 0)
                res.set(i, t.get(i));
        }

        return res;
    }

    private static boolean talentExists(ArrayList<Integer> t) {
        boolean empty = true;

        for(int i = 1; i < t.size(); i++) {
            empty &= t.get(i) == 0;
        }

        return !empty;
    }

    public static void showEnemyEmb(Enemy e, MessageChannel ch, boolean isFrame, boolean extra, boolean compact, int[] magnification, int lang) throws Exception {
        File img = generateIcon(e);

        EmbedBuilder spec = new EmbedBuilder();

        int c = StaticStore.rainbow[0];

        int[] mag = new int[2];

        if(magnification.length == 1) {
            if(magnification[0] <= 0) {
                mag[0] = mag[1] = 100;
            } else {
                mag[0] = mag[1] = magnification[0];
            }
        } else if(magnification.length == 2) {
            mag = magnification;

            if(mag[0] <= 0)
                mag[0] = 100;

            if(mag[1] < 0)
                mag[1] = 0;
        }

        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");

        if(compact) {
            spec.setTitle(DataToString.getCompactTitle(e, lang));

            spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag, 100), false);
            spec.addField(LangID.getStringByID("data_hpkb", lang), DataToString.getHealthHitback(e.de, mag[0]), false);
            spec.addField(LangID.getStringByID("data_drbasp", lang), DataToString.getDropBarrierSpeed(e.de, lang), true);
            spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(e.de), true);
            spec.addField(LangID.getStringByID("data_times", lang), DataToString.getCompactAtkTimings(e.de, isFrame), false);
            spec.addField(LangID.getStringByID("data_atkdps", lang).replace("_TTT_", DataToString.getSiMu(e.de, lang)), DataToString.getCompactAtk(e.de, mag[1]), false);
            spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(e.de, true, lang), false);
        } else {
            spec.setTitle(DataToString.getTitle(e, lang));

            spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), true);
            spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag, 100), true);
            spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e.de, mag[0]), true);
            spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e.de), true);
            spec.addField(LangID.getStringByID("data_barrier", lang), DataToString.getBarrier(e.de, lang), true);
            spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(e.de), true);
            spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(e.de, isFrame), true);
            spec.addField(LangID.getStringByID("data_drop", lang), DataToString.getDrop(e.de), true);
            spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(e.de), true);
            spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(e.de, lang), true);
            spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(e.de, mag[1]), true);
            spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(e.de, lang), true);
            spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(e.de, mag[1]), true);
            spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(e.de, true, lang), true);
        }

        List<String> abis = Interpret.getAbi(e.de, true, lang);
        abis.addAll(Interpret.getProc(e.de, !isFrame, true, lang, mag[0] / 100.0, mag[1] / 100.0));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append(abis.get(i));
            else
                sb.append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data_none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(e.de, false, lang);
            abis.addAll(Interpret.getProc(e.de, !isFrame, false, lang, mag[0] / 100.0, mag[1] / 100.0));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append(abis.get(i));
                else
                    sb.append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data_ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(e, lang);

            if(explanation != null) {
                spec.addField(LangID.getStringByID("data_edesc", lang), explanation, false);
            }
        }

        spec.setFooter(LangID.getStringByID("enemyst_source", lang));

        MessageCreateAction action = ch.sendMessageEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        action.queue(msg -> {
            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("W/EntityHandlerEnemyEmb | Can't delete file : "+img.getAbsolutePath());
            }
        }, err -> {
            StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::showEnemyEmb - Error happend while trying to dispaly enemy embed");

            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("W/EntityHandlerEnemyEmb | Can't delete file : "+img.getAbsolutePath());
            }
        });

        e.anim.unload();
    }

    public static void performEnemyEmb(Enemy e, GenericCommandInteractionEvent event, boolean isFrame, boolean extra, int[] magnification, int lang) throws Exception {
        File img = generateIcon(e);

        EmbedBuilder spec = new EmbedBuilder();

        int c = StaticStore.rainbow[0];

        int[] mag = new int[2];

        if(magnification.length == 1) {
            if(magnification[0] <= 0) {
                mag[0] = mag[1] = 100;
            } else {
                mag[0] = mag[1] = magnification[0];
            }
        } else if(magnification.length == 2) {
            mag = magnification;

            if(mag[0] <= 0)
                mag[0] = 100;

            if(mag[1] < 0)
                mag[1] = 0;
        }

        spec.setTitle(DataToString.getTitle(e, lang));
        spec.setColor(c);
        spec.setThumbnail("attachment://icon.png");
        spec.addField(LangID.getStringByID("data_id", lang), DataToString.getID(e.id.id), true);
        spec.addField(LangID.getStringByID("data_magnif", lang), DataToString.getMagnification(mag, 100), true);
        spec.addField(LangID.getStringByID("data_hp", lang), DataToString.getHP(e.de, mag[0]), true);
        spec.addField(LangID.getStringByID("data_hb", lang), DataToString.getHitback(e.de), true);
        spec.addField(LangID.getStringByID("data_barrier", lang), DataToString.getBarrier(e.de, lang), true);
        spec.addField(LangID.getStringByID("data_speed", lang), DataToString.getSpeed(e.de), true);
        spec.addField(LangID.getStringByID("data_atktime", lang), DataToString.getAtkTime(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data_preatk", lang), DataToString.getPre(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data_postatk", lang), DataToString.getPost(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data_tba", lang), DataToString.getTBA(e.de, isFrame), true);
        spec.addField(LangID.getStringByID("data_drop", lang), DataToString.getDrop(e.de), true);
        spec.addField(LangID.getStringByID("data_range", lang), DataToString.getRange(e.de), true);
        spec.addField(LangID.getStringByID("data_atktype", lang), DataToString.getSiMu(e.de, lang), true);
        spec.addField(LangID.getStringByID("data_dps", lang), DataToString.getDPS(e.de, mag[1]), true);
        spec.addField(LangID.getStringByID("data_abilt", lang), DataToString.getAbilT(e.de, lang), true);
        spec.addField(LangID.getStringByID("data_atk", lang), DataToString.getAtk(e.de, mag[1]), true);
        spec.addField(LangID.getStringByID("data_trait", lang), DataToString.getTrait(e.de, true, lang), true);

        List<String> abis = Interpret.getAbi(e.de, true, lang);
        abis.addAll(Interpret.getProc(e.de, !isFrame, true, lang, mag[0] / 100.0, mag[1] / 100.0));

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < abis.size(); i++) {
            if(i == abis.size() - 1)
                sb.append(abis.get(i));
            else
                sb.append(abis.get(i)).append("\n");
        }

        String res = sb.toString();

        if(res.isBlank())
            res = LangID.getStringByID("data_none", lang);
        else if(res.length() > 1024) {
            abis = Interpret.getAbi(e.de, false, lang);
            abis.addAll(Interpret.getProc(e.de, !isFrame, false, lang, mag[0] / 100.0, mag[1] / 100.0));

            sb = new StringBuilder();

            for(int i = 0; i < abis.size(); i++) {
                if(i == abis.size() - 1)
                    sb.append(abis.get(i));
                else
                    sb.append(abis.get(i)).append("\n");
            }

            res = sb.toString();
        }

        spec.addField(LangID.getStringByID("data_ability", lang), res, false);

        if(extra) {
            String explanation = DataToString.getDescription(e, lang);

            if(explanation != null) {
                spec.addField(LangID.getStringByID("data_edesc", lang), explanation, false);
            }
        }

        spec.setFooter(LangID.getStringByID("enemyst_source", lang), null);

        ReplyCallbackAction action = event.deferReply().addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "icon.png"));

        action.queue(h -> {
            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("Failed to remove file : "+img.getAbsolutePath());
            }
        }, err -> {
            StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::performEnemyEmb - Erro happened while trying to dispaly enemy embed");

            if(img != null && img.exists() && !img.delete()) {
                StaticStore.logger.uploadLog("Failed to remove file : "+img.getAbsolutePath());
            }
        });

        e.anim.unload();
    }

    private static File generateIcon(Enemy e) throws IOException {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(img == null)
            return null;

        Object image;

        if(e.anim.getEdi() != null)
            image = e.anim.getEdi().getImg().bimg();
        else
            return null;

        ImageIO.write((BufferedImage) image, "PNG", img);

        return img;
    }

    private static File generateIcon(Form f) throws IOException {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(img == null) {
            return null;
        }

        Object image;

        if(f.anim.getUni() != null)
            image = f.anim.getUni().getImg().bimg();
        else if(f.anim.getEdi() != null)
            image = f.anim.getEdi().getImg().bimg();
        else
            return null;

        ImageIO.write((BufferedImage) image, "PNG", img);

        return img;
    }

    private static File generateCatfruit(Form f) throws IOException {
        if(f.unit == null)
            return null;

        if(f.unit.info.evo == null)
            return null;

        File tmp = new File("./temp");

        if(!tmp.exists()) {
            boolean res = tmp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+tmp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(tmp, "result", ".png", false);

        if(img == null) {
            return null;
        }

        BufferedImage image = new BufferedImage(600, 150, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(image.getGraphics());

        g.setFont(font);

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setStroke(2f);
        g.setColor(47, 49, 54, 255);

        g.fillRect(0, 0, 600, 150);

        g.setColor(238, 238, 238, 128);

        g.drawRect(0, 0, 600, 150);

        for(int i = 1; i < 6; i++) {
            g.drawLine(100 * i, 0, 100* i , 150);
        }

        g.drawLine(0, 100, 600, 100);

        g.setColor(238, 238, 238, 255);

        for(int i = 0; i < 6; i++) {
            if(i == 0) {
                VFile vf = VFile.get("./org/page/catfruit/xp.png");

                if(vf != null) {
                    BufferedImage icon = (BufferedImage) vf.getData().getImg().bimg();
                    FakeImage fi = FIBI.build(icon);

                    g.drawImage(fi, 510, 10, 80, 80);
                    g.drawCenteredText(String.valueOf(f.unit.info.evo[i][0]), 550, 125);
                }
            } else {
                if(f.unit.info.evo[i][0] != 0) {
                    VFile vf = VFile.get("./org/page/catfruit/gatyaitemD_"+f.unit.info.evo[i][0]+"_f.png");

                    if(vf != null) {
                        BufferedImage icon = (BufferedImage) vf.getData().getImg().bimg();
                        FakeImage fi = FIBI.build(icon);

                        g.drawImage(fi, 100 * (i-1)+5, 10, 80, 80);
                        g.drawCenteredText(String.valueOf(f.unit.info.evo[i][1]), 100 * (i-1) + 50, 125);
                    }
                }
            }
        }

        ImageIO.write(image, "PNG", img);

        return img;
    }

    public static Message showStageEmb(Stage st, MessageChannel ch, boolean isFrame, boolean isExtra, boolean isCompact, int level, int lang) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
            stmMagnification = stm.stars[sta];
        }

        if(stm != null) {
            MapColc mc = stm.getCont();

            if(mc != null && mc.getSID().equals("000003") && stm.id.id == 9) {
                if(st.id.id == 49) {
                    sta = 1;
                } else if(st.id.id == 50) {
                    sta = 2;
                }
            }
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification);

        EmbedBuilder spec = new EmbedBuilder();

        if(!(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).diff == -1)
            spec.setColor(new Color(217, 217, 217).getRGB());
        else
            spec.setColor(DataToString.getDifficultyColor(((DefStageInfo) st.info).diff));

        String name = "";

        if(stm == null) {
            return ch.sendMessageEmbeds(spec.build()).complete();
        }

        MapColc mc = stm.getCont();

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String mcName = MultiLangCont.get(mc);

        CommonStatic.getConfig().lang = oldConfig;

        if(mcName == null || mcName.isBlank())
            mcName = mc.getSID();

        name += mcName+" - ";

        CommonStatic.getConfig().lang = lang;

        String stmName = MultiLangCont.get(stm);

        CommonStatic.getConfig().lang = oldConfig;

        if(stmName == null || stmName.isBlank())
            if(stm.id != null)
                stmName = Data.trio(stm.id.id);
            else
                stmName = "Unknown";

        name += stmName + " - ";

        CommonStatic.getConfig().lang = lang;

        String stName = MultiLangCont.get(st);

        CommonStatic.getConfig().lang = oldConfig;

        if(stName == null || stName.isBlank())
            if(st.id != null)
                stName = Data.trio(st.id.id);
            else
                stName = "Unknown";

        name += stName;

        spec.setTitle(name);

        if(isCompact) {
            spec.addField(LangID.getStringByID("data_iddile", lang), DataToString.getIdDifficultyLevel(st, sta, lang), false);

            String secondField = DataToString.getEnergyBaseXP(st, lang);

            if(secondField.contains("!!drink!!")) {
                secondField = secondField.replace("!!drink!!", "");

                spec.addField(LangID.getStringByID("data_cabaxp", lang), secondField, false);
            } else {
                spec.addField(LangID.getStringByID("data_enbaxp", lang), secondField, false);
            }

            spec.addField(LangID.getStringByID("data_encole", lang), DataToString.getEnemyContinuableLength(st, lang), false);
            spec.addField(LangID.getStringByID("data_mubaca", lang).replace("_BBB_", st.mush + ""), DataToString.getMusciBackgroundCastle(st, lang), false);
            spec.setFooter(LangID.getStringByID("data_minres", lang).replace("_RRR_", DataToString.getMinSpawn(st, isFrame)));
        } else {
            spec.addField(LangID.getStringByID("data_id", lang), DataToString.getStageCode(st), true);
            spec.addField(LangID.getStringByID("data_level", lang), DataToString.getStar(st, sta), true);

            String energy = DataToString.getEnergy(st, lang);

            if(energy.endsWith("!!drink!!")) {
                spec.addField(LangID.getStringByID("data_catamin", lang), energy.replace("!!drink!!", ""), true);
            } else {
                spec.addField(LangID.getStringByID("data_energy", lang), energy, true);
            }

            spec.addField(LangID.getStringByID("data_base", lang), DataToString.getBaseHealth(st), true);
            spec.addField(LangID.getStringByID("data_xp", lang), DataToString.getXP(st), true);
            spec.addField(LangID.getStringByID("data_diff", lang), DataToString.getDifficulty(st, lang), true);
            spec.addField(LangID.getStringByID("data_continuable", lang), DataToString.getContinuable(st, lang), true);
            spec.addField(LangID.getStringByID("data_music", lang), DataToString.getMusic(st, lang), true);
            spec.addField(DataToString.getMusicChange(st), DataToString.getMusic1(st, lang) , true);
            spec.addField(LangID.getStringByID("data_maxenem", lang), DataToString.getMaxEnemy(st), true);
            spec.addField(LangID.getStringByID("data_bg", lang), DataToString.getBackground(st, lang),true);
            spec.addField(LangID.getStringByID("data_castle", lang), DataToString.getCastle(st, lang), true);
            spec.addField(LangID.getStringByID("data_length", lang), DataToString.getLength(st), true);
            spec.addField(LangID.getStringByID("data_minspawn", lang), DataToString.getMinSpawn(st, isFrame), true);
        }

        ArrayList<String> limit = DataToString.getLimit(st.getLim(sta), lang);

        StringBuilder sb = new StringBuilder();

        if(limit.isEmpty()) {
            sb.append(LangID.getStringByID("data_none", lang));
        } else {
            for(int i = 0; i < limit.size(); i ++) {
                sb.append(limit.get(i));

                if(i < limit.size()-1)
                    sb.append("\n");
            }
        }

        spec.addField(LangID.getStringByID("data_limit", lang), sb.toString(), false);

        if(isExtra) {
            List<String> misc = DataToString.getMiscellaneous(st, lang);

            if(!misc.isEmpty()) {
                StringBuilder sbuilder = new StringBuilder();

                for(int i = 0; i < misc.size(); i++) {
                    sbuilder.append("⦁ ").append(misc.get(i));

                    if(i < misc.size() - 1) {
                        sbuilder.append("\n");
                    }
                }

                spec.addField(LangID.getStringByID("data_misc", lang), sbuilder.toString(), false);
            }

            spec.addField(LangID.getStringByID("data_exstage", lang), DataToString.getEXStage(st, lang), false);
        }

        String drops = DataToString.getRewards(st, lang);

        if(drops != null) {
            if(drops.endsWith("!!number!!")) {
                spec.addField(LangID.getStringByID("data_numreward", lang), drops.replace("!!number!!", ""), false);
            } else if(drops.endsWith("!!nofail!!")) {
                spec.addField(LangID.getStringByID("data_chanrewardnofail", lang), drops.replace("!!nofail!!", ""), false);
            } else {
                spec.addField(LangID.getStringByID("data_chanreward", lang), drops, false);
            }
        }

        String score = DataToString.getScoreDrops(st, lang);

        if(score != null) {
            spec.addField(LangID.getStringByID("data_score", lang), score, false);
        }

        if(img != null) {
            spec.addField(LangID.getStringByID("data_scheme", lang), "** **", false);
            spec.setImage("attachment://scheme.png");
        }

        MessageCreateAction action = ch.sendMessageEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "scheme.png"));

        ArrayList<Button> buttons = new ArrayList<>();

        buttons.add(Button.secondary("castle", LangID.getStringByID("button_castle", lang)).withEmoji(Emoji.fromCustom(EmojiStore.CASTLE)));
        buttons.add(Button.secondary("bg", LangID.getStringByID("button_bg", lang)).withEmoji(Emoji.fromCustom(EmojiStore.BACKGROUND)));

        if(st.mus0 != null) {
            buttons.add(Button.secondary("music", LangID.getStringByID("button_mus", lang)).withEmoji(Emoji.fromCustom(EmojiStore.MUSIC)));
        }

        if(hasTwoMusic(st)) {
            buttons.add(Button.secondary("music2", LangID.getStringByID("button_mus2", lang)).withEmoji(Emoji.fromCustom(EmojiStore.MUSIC_BOSS)));
        }

        action = action.setComponents(ActionRow.of(buttons));

        Message msg = action.complete();

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(img != null && img.exists() && !img.delete()) {
                    StaticStore.logger.uploadLog("Can't delete file : "+img.getAbsolutePath());
                }

                timer.cancel();
            }
        };

        timer.schedule(task, 5000);

        return msg;
    }

    public static Message performStageEmb(Stage st, GenericCommandInteractionEvent event, boolean isFrame, boolean isExtra, int level, int lang) throws Exception {
        StageMap stm = st.getCont();

        int sta;
        int stmMagnification;

        if(stm == null) {
            sta = 0;
            stmMagnification = 100;
        } else {
            sta = Math.min(Math.max(level-1, 0), st.getCont().stars.length-1);
            stmMagnification = stm.stars[sta];
        }

        File img = generateScheme(st, isFrame, lang, stmMagnification);

        EmbedBuilder spec = new EmbedBuilder();

        if(!(st.info instanceof DefStageInfo) || ((DefStageInfo) st.info).diff == -1)
            spec.setColor(new Color(217, 217, 217).getRGB());
        else
            spec.setColor(DataToString.getDifficultyColor(((DefStageInfo) st.info).diff));

        String name = "";

        if(stm == null)
            return null;

        MapColc mc = stm.getCont();

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String mcName = MultiLangCont.get(mc);

        CommonStatic.getConfig().lang = oldConfig;

        if(mcName == null || mcName.isBlank())
            mcName = mc.getSID();

        name += mcName+" - ";

        CommonStatic.getConfig().lang = lang;

        String stmName = MultiLangCont.get(stm);

        CommonStatic.getConfig().lang = oldConfig;

        if(stmName == null || stmName.isBlank())
            if(stm.id != null)
                stmName = Data.trio(stm.id.id);
            else
                stmName = "Unknown";

        name += stmName + " - ";

        CommonStatic.getConfig().lang = lang;

        String stName = MultiLangCont.get(st);

        CommonStatic.getConfig().lang = oldConfig;

        if(stName == null || stName.isBlank())
            if(st.id != null)
                stName = Data.trio(st.id.id);
            else
                stName = "Unknown";

        name += stName;

        spec.setTitle(name);
        spec.addField(LangID.getStringByID("data_id", lang), DataToString.getStageCode(st), true);
        spec.addField(LangID.getStringByID("data_level", lang), DataToString.getStar(st, sta), true);

        String energy = DataToString.getEnergy(st, lang);

        if(energy.endsWith("!!drink!!")) {
            spec.addField(LangID.getStringByID("data_catamin", lang), energy.replace("!!drink!!", ""), true);
        } else {
            spec.addField(LangID.getStringByID("data_energy", lang), energy, true);
        }

        spec.addField(LangID.getStringByID("data_base", lang), DataToString.getBaseHealth(st), true);
        spec.addField(LangID.getStringByID("data_xp", lang), DataToString.getXP(st), true);
        spec.addField(LangID.getStringByID("data_diff", lang), DataToString.getDifficulty(st, lang), true);
        spec.addField(LangID.getStringByID("data_continuable", lang), DataToString.getContinuable(st, lang), true);
        spec.addField(LangID.getStringByID("data_music", lang), DataToString.getMusic(st, lang), true);
        spec.addField(DataToString.getMusicChange(st), DataToString.getMusic1(st, lang) , true);
        spec.addField(LangID.getStringByID("data_maxenem", lang), DataToString.getMaxEnemy(st), true);
        spec.addField(LangID.getStringByID("data_bg", lang), DataToString.getBackground(st, lang),true);
        spec.addField(LangID.getStringByID("data_castle", lang), DataToString.getCastle(st, lang), true);
        spec.addField(LangID.getStringByID("data_length", lang), DataToString.getLength(st), true);
        spec.addField(LangID.getStringByID("data_minspawn", lang), DataToString.getMinSpawn(st, isFrame), true);

        ArrayList<String> limit = DataToString.getLimit(st.getLim(sta), lang);

        StringBuilder sb = new StringBuilder();

        if(limit.isEmpty()) {
            sb.append(LangID.getStringByID("data_none", lang));
        } else {
            for(int i = 0; i < limit.size(); i ++) {
                sb.append(limit.get(i));

                if(i < limit.size()-1)
                    sb.append("\n");
            }
        }

        spec.addField(LangID.getStringByID("data_limit", lang), sb.toString(), false);

        if(isExtra) {
            List<String> misc = DataToString.getMiscellaneous(st, lang);

            if(!misc.isEmpty()) {
                StringBuilder sbuilder = new StringBuilder();

                for(int i = 0; i < misc.size(); i++) {
                    sbuilder.append("⦁ ").append(misc.get(i));

                    if(i < misc.size() - 1) {
                        sbuilder.append("\n");
                    }
                }

                spec.addField(LangID.getStringByID("data_misc", lang), sbuilder.toString(), false);
            }

            spec.addField(LangID.getStringByID("data_exstage", lang), DataToString.getEXStage(st, lang), false);
        }

        String drops = DataToString.getRewards(st, lang);

        if(drops != null) {
            if(drops.endsWith("!!number!!")) {
                spec.addField(LangID.getStringByID("data_numreward", lang), drops.replace("!!number!!", ""), false);
            } else if(drops.endsWith("!!nofail!!")) {
                spec.addField(LangID.getStringByID("data_chanrewardnofail", lang), drops.replace("!!nofail!!", ""), false);
            } else {
                spec.addField(LangID.getStringByID("data_chanreward", lang), drops, false);
            }
        }

        String score = DataToString.getScoreDrops(st, lang);

        if(score != null) {
            spec.addField(LangID.getStringByID("data_score", lang), score, false);
        }

        if(img != null) {
            spec.addField(LangID.getStringByID("data_scheme", lang), "** **", false);
            spec.setImage("attachment://scheme.png");
        }

        ReplyCallbackAction action = event.deferReply().addEmbeds(spec.build());

        if(img != null)
            action = action.addFiles(FileUpload.fromData(img, "scheme.png"));

        InteractionHook hook = action.complete();

        Message msg = hook.retrieveOriginal().complete();

        if(msg != null) {
            Guild g = msg.getGuild();
            MessageChannel ch = msg.getChannel();

            if(ch instanceof GuildChannel && g.getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)) {
                msg.addReaction(EmojiStore.CASTLE).queue();
                msg.addReaction(EmojiStore.BACKGROUND).queue();

                if(st.mus0 != null) {
                    msg.addReaction(EmojiStore.MUSIC).queue();
                }

                if(hasTwoMusic(st)) {
                    msg.addReaction(EmojiStore.MUSIC_BOSS).queue();
                }
            }

            Timer timer = new Timer();

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(img != null && img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Can't delete file : "+img.getAbsolutePath());
                    }

                    timer.cancel();
                }
            };

            timer.schedule(task, 5000);
        }

        return msg;
    }

    private static File generateScheme(Stage st, boolean isFrame, int lang, int star) throws Exception {
        File temp = new File("./temp/");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return null;
            }
        }

        File img = StaticStore.generateTempFile(temp, "scheme", ".png", false);

        if(img == null) {
            return null;
        }

        Canvas cv = new Canvas();
        FontMetrics fm = cv.getFontMetrics(font);

        boolean needBoss = false;
        boolean needRespect = false;
        boolean needCount = false;

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            if(!needBoss && line.boss != 0)
                needBoss = true;

            if(!needRespect && (line.spawn_0 < 0 || line.spawn_1 < 0))
                needRespect = true;

            if(!needCount && line.kill_count != 0)
                needCount = true;
        }

        ArrayList<String> enemies = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        ArrayList<String> magnifs = new ArrayList<>();
        ArrayList<String> isBoss = new ArrayList<>();
        ArrayList<String> baseHealth = new ArrayList<>();
        ArrayList<String> startRespawn = new ArrayList<>();
        ArrayList<String> layers = new ArrayList<>();
        ArrayList<String> respects = new ArrayList<>();
        ArrayList<String> killCounts = new ArrayList<>();

        for(int i = st.data.datas.length - 1; i >= 0; i--) {
            SCDef.Line line = st.data.datas[i];

            AbEnemy enemy = line.enemy.get();

            if(enemy == null)
                continue;

            if(enemy instanceof Enemy) {
                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String eName = MultiLangCont.get(enemy);

                CommonStatic.getConfig().lang = oldConfig;

                if(eName == null || eName.isBlank())
                    eName = ((Enemy) enemy).names.toString();

                if(eName.isBlank())
                    eName = DataToString.getPackName(((Enemy) enemy).id.pack, lang)+" - "+Data.trio(((Enemy) enemy).id.id);

                enemies.add(eName);
            } else if(enemy instanceof EneRand) {
                String name = ((EneRand) enemy).name;

                if(name == null || name.isBlank()) {
                    name = DataToString.getPackName(((EneRand) enemy).id.pack, lang)+"-"+Data.trio(((EneRand) enemy).id.id);
                }

                enemies.add(name);
            } else
                continue;

            String number;

            if(line.number == 0)
                number = LangID.getStringByID("data_infinite", lang);
            else
                number = String.valueOf(line.number);

            numbers.add(number);

            int[] magnification;

            if(st.getCont() != null && st.getCont().getCont() != null && st.getCont().getCont().getSID().equals("000003") && st.getCont().id.id == 9) {
                magnification = new int[] {100, 100};
            } else {
                magnification = new int[] {line.multiple, line.mult_atk};
            }

            String magnif = DataToString.getMagnification(magnification, star);

            magnifs.add(magnif);

            String start;

            if(line.spawn_1 == 0)
                if(isFrame)
                    start = Math.abs(line.spawn_0)+"f";
                else
                    start = df.format(Math.abs(line.spawn_0) / 30.0)+"s";
            else {
                int minSpawn = Math.abs(Math.min(line.spawn_0, line.spawn_1));
                int maxSpawn = Math.abs(Math.max(line.spawn_0, line.spawn_1));

                if(isFrame)
                    start = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    start = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String respawn;

            if(line.respawn_0 == line.respawn_1)
                if(isFrame)
                    respawn = line.respawn_0+"f";
                else
                    respawn = df.format(line.respawn_0/30.0)+"s";
            else {
                int minSpawn = Math.min(line.respawn_0, line.respawn_1);
                int maxSpawn = Math.max(line.respawn_0, line.respawn_1);

                if(isFrame)
                    respawn = minSpawn+"f ~ "+maxSpawn+"f";
                else
                    respawn = df.format(minSpawn/30.0)+"s ~ "+df.format(maxSpawn/30.0)+"s";
            }

            String startResp = start+" ("+respawn+")";

            startRespawn.add(startResp);

            String baseHP;

            if(st.trail) {
                baseHP = line.castle_0 + "";
            } else {
                if(line.castle_0 == line.castle_1 || line.castle_1 == 0)
                    baseHP = line.castle_0+"%";
                else {
                    int minHealth = Math.min(line.castle_0, line.castle_1);
                    int maxHealth = Math.max(line.castle_0, line.castle_1);

                    baseHP = minHealth + " ~ " + maxHealth + "%";
                }
            }

            baseHealth.add(baseHP);

            String layer;

            if(line.layer_0 != line.layer_1) {
                int minLayer = Math.min(line.layer_0, line.layer_1);
                int maxLayer = Math.max(line.layer_0, line.layer_1);

                layer = minLayer + " ~ " + maxLayer;
            } else {
                layer = String.valueOf(line.layer_0);
            }

            layers.add(layer);

            if(needBoss) {
                String boss;

                if(line.boss == 0)
                    boss = "";
                else if(line.boss == 1)
                    boss = LangID.getStringByID("data_boss", lang);
                else
                    boss = LangID.getStringByID("data_bossshake", lang);

                isBoss.add(boss);
            }

            if(needRespect) {
                String respect = (line.spawn_0 < 0 || line.spawn_1 < 0) ? LangID.getStringByID("data_true", lang) : "";

                respects.add(respect);
            }

            if(needCount) {
                killCounts.add(line.kill_count + "");
            }
        }

        double eMax = fm.stringWidth(LangID.getStringByID("data_enemy", lang));
        double nMax = fm.stringWidth(LangID.getStringByID("data_number", lang));
        double mMax = fm.stringWidth(LangID.getStringByID("data_magnif", lang));
        double iMax = fm.stringWidth(LangID.getStringByID("data_isboss", lang));
        double bMax = fm.stringWidth(LangID.getStringByID(st.trail ? "data_basedealt" : "data_basehealth", lang));
        double sMax = fm.stringWidth(LangID.getStringByID("data_startres", lang));
        double lMax = fm.stringWidth(LangID.getStringByID("data_layer", lang));
        double rMax = fm.stringWidth(LangID.getStringByID("data_respect", lang));
        double kMax = fm.stringWidth(LangID.getStringByID("data_killcount", lang));

        for(int i = 0; i < enemies.size(); i++) {
            eMax = Math.max(eMax, fm.stringWidth(enemies.get(i)));

            nMax = Math.max(nMax, fm.stringWidth(numbers.get(i)));

            mMax = Math.max(mMax, fm.stringWidth(magnifs.get(i)));

            bMax = Math.max(bMax, fm.stringWidth(baseHealth.get(i)));

            sMax = Math.max(sMax, fm.stringWidth(startRespawn.get(i)));

            lMax = Math.max(lMax, fm.stringWidth(layers.get(i)));

            if(needBoss)
                iMax = Math.max(iMax, fm.stringWidth(isBoss.get(i)));

            if(needRespect)
                rMax = Math.max(rMax, fm.stringWidth(respects.get(i)));

            if(needCount)
                kMax = Math.max(kMax, fm.stringWidth(killCounts.get(i)));
        }

        int xGap = 16;
        int yGap = 10;

        eMax += xGap + 93;
        nMax += xGap;
        mMax += xGap;
        bMax += xGap;
        sMax += xGap;
        lMax += xGap;
        rMax += xGap;
        kMax += xGap;
        iMax += xGap;

        int ySeg = Math.max(fm.getHeight() + yGap, 32 + yGap);

        int w = (int) (eMax + nMax + mMax + bMax + sMax + lMax);

        if(needBoss)
            w += iMax;

        if(needRespect)
            w += rMax;

        if(needCount)
            w += kMax;

        int h = ySeg * (enemies.size() + 1);

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(image.getGraphics());

        g.setRenderingHint(3, 1);
        g.enableAntialiasing();

        g.setFont(font);

        g.setColor(47, 49, 54, 255);
        g.fillRect(0, 0, w, h);

        g.setColor(54, 57, 63, 255);
        g.fillRect(0, 0, w, ySeg);

        g.setColor(32, 34, 37, 255);
        g.setStroke(4f);

        g.drawRect(0, 0, w, h);

        g.setStroke(2f);

        for(int i = 1; i < enemies.size() + 1; i++) {
            g.drawLine(0, ySeg * i, w, ySeg * i);
        }

        int x = (int) eMax;

        g.drawLine(x, 0, x, h);

        x += (int) nMax;

        g.drawLine(x, 0, x, h);

        x += (int) bMax;

        g.drawLine(x, 0, x, h);

        x += (int) mMax;

        g.drawLine(x, 0, x, h);

        x += (int) sMax;

        g.drawLine(x, 0, x, h);

        x += (int) lMax;

        g.drawLine(x, 0, x, h);

        if(needRespect) {
            x += (int) rMax;

            g.drawLine(x, 0, x, h);
        }

        if(needCount) {
            x += (int) kMax;

            g.drawLine(x, 0, x, h);
        }

        if(needBoss) {
            x += (int) iMax;

            g.drawLine(x, 0, x, h);
        }

        g.setColor(238, 238, 238, 255);

        int initX = (int) (eMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_enemy", lang), initX, ySeg / 2);

        initX += (int) (eMax / 2 + nMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_number", lang), initX, ySeg / 2);

        initX += (int) (nMax / 2 + bMax / 2);

        g.drawCenteredText(LangID.getStringByID(st.trail ? "data_basedealt" : "data_basehealth", lang), initX, ySeg / 2);

        initX += (int) (bMax / 2 + mMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_magnif", lang), initX, ySeg / 2);

        initX += (int) (mMax / 2 + sMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_startres", lang), initX, ySeg / 2);

        initX += (int) (sMax / 2 + lMax / 2);

        g.drawCenteredText(LangID.getStringByID("data_layer", lang), initX, ySeg / 2);

        initX += (int) (lMax / 2);

        if(needRespect) {
            initX += (int) (rMax / 2);

            g.drawCenteredText(LangID.getStringByID("data_respect", lang), initX, ySeg / 2);

            initX += (int) (rMax / 2);
        }

        if(needCount) {
            initX += (int) (kMax / 2);

            g.drawCenteredText(LangID.getStringByID("data_killcount", lang), initX, ySeg / 2);

            initX += (int) (kMax / 2);
        }

        if(needBoss) {
            initX += (int) (iMax / 2);

            g.drawCenteredText(LangID.getStringByID("data_isboss", lang), initX, ySeg / 2);
        }

        for(int i = 0; i < enemies.size(); i++) {
            AbEnemy e = st.data.datas[st.data.datas.length - 1 - i].enemy.get();

            if(e != null) {
                BufferedImage edi;

                if(e instanceof Enemy) {
                    if(((Enemy) e).anim.getEdi() != null) {
                        edi = (BufferedImage) ((Enemy) e).anim.getEdi().getImg().bimg();
                    } else {
                        edi = (BufferedImage) CommonStatic.getBCAssets().ico[0][0].getImg().bimg();
                    }
                } else {
                    edi = (BufferedImage) CommonStatic.getBCAssets().ico[0][0].getImg().bimg();
                }

                FakeImage fi = FIBI.build(edi);

                g.drawImage(fi, xGap / 2.0, ySeg * (i + 1) + yGap / 2.0);
            } else
                continue;

            int px = 93 + xGap / 2;
            int py = ySeg * (i + 2) - ySeg / 2;

            g.drawVerticalCenteredText(enemies.get(i), px, py);

            px = (int) eMax + xGap / 2;

            g.drawVerticalCenteredText(numbers.get(i), px, py);

            px += (int) nMax;

            g.drawVerticalCenteredText(baseHealth.get(i), px, py);

            px += (int) bMax;

            g.drawVerticalCenteredText(magnifs.get(i), px, py);

            px += (int) mMax;

            g.drawVerticalCenteredText(startRespawn.get(i), px, py);

            px += (int) sMax;

            g.drawVerticalCenteredText(layers.get(i), px, py);

            px += (int) lMax;

            if(needRespect) {
                g.drawVerticalCenteredText(respects.get(i), px, py);

                px += (int) rMax;
            }

            if(needCount) {
                g.drawVerticalCenteredText(killCounts.get(i), px, py);

                px += (int) kMax;
            }

            if(needBoss) {
                g.drawVerticalCenteredText(isBoss.get(i), px, py);
            }
        }

        ImageIO.write(image, "PNG", img);

        return img;
    }

    public static void generateFormImage(Form f, MessageChannel ch, int mode, int frame, boolean transparent, boolean debug, int lang) throws Exception {
        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = f.anim.getEAnim(ImageDrawing.getAnimType(mode, f.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, transparent, debug);

        f.anim.unload();

        if(img != null) {
            CommonStatic.getConfig().lang = lang;

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String fName = MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

            if(fName == null || fName.isBlank())
                fName = f.names.toString();

            if(fName.isBlank())
                fName = LangID.getStringByID("data_unit", lang)+" "+ Data.trio(f.uid.id)+" "+Data.trio(f.fid);

            ch.sendMessage(LangID.getStringByID("fimg_result", lang).replace("_", fName).replace(":::", getModeName(mode, f.anim.anims.length, lang)).replace("=", String.valueOf(frame)))
                    .addFiles(FileUpload.fromData(img, "result.png"))
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerFormImage | Can't delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormImage - Error happened while trying to show form image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerFormImage | Can't delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    public static void generateEnemyImage(Enemy en, MessageChannel ch, int mode, int frame, boolean transparent, boolean debug, int lang) throws Exception {
        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = en.anim.getEAnim(ImageDrawing.getAnimType(mode, en.anim.anims.length));

        File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, transparent, debug);

        en.anim.unload();

        if(img != null) {
            CommonStatic.getConfig().lang = lang;

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String eName = MultiLangCont.get(en);

            CommonStatic.getConfig().lang = oldConfig;

            if(eName == null || eName.isBlank())
                eName = en.names.toString();

            if(eName.isBlank())
                eName = LangID.getStringByID("data_enemy", lang)+" "+ Data.trio(en.id.id);

            ch.sendMessage(LangID.getStringByID("fimg_result", lang).replace("_", eName).replace(":::", getModeName(mode, en.anim.anims.length, lang)).replace("=", String.valueOf(frame)))
                    .addFiles(FileUpload.fromData(img, "result.png"))
                    .queue(m -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerFormImage | Can't delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormImage - Error happened while trying to show form image");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerFormImage | Can't delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    private static String getModeName(int mode, int max, int lang) {
        switch (mode) {
            case 1:
                return LangID.getStringByID("fimg_idle", lang);
            case 2:
                return LangID.getStringByID("fimg_atk", lang);
            case 3:
                return LangID.getStringByID("fimg_hitback", lang);
            case 4:
                if(max == 5)
                    return LangID.getStringByID("fimg_enter", lang);
                else
                    return LangID.getStringByID("fimg_burrowdown", lang);
            case 5:
                return LangID.getStringByID("fimg_burrowmove", lang);
            case 6:
                return LangID.getStringByID("fimg_burrowup", lang);
            default:
                return LangID.getStringByID("fimg_walk", lang);
        }
    }

    public static boolean generateFormAnim(Form f, MessageChannel ch, int booster, int mode, boolean debug, int limit, int lang, boolean raw, boolean gif) throws Exception {
        if(f.unit == null || f.unit.id == null)
            return false;
        else if(!debug && limit <= 0) {
            String id = generateID(f, mode);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                ch.sendMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).queue();
                return false;
            }
        }

        JDA client = ch.getJDA();

        f.anim.load();

        if(mode >= f.anim.anims.length)
            mode = 0;

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"").replace("-", limit+"")).queue();
        } else if(!raw && f.anim.len(getAnimType(mode, f.anim.anims.length)) >= 300) {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"").replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("gif_length", lang).replace("_", f.anim.len(getAnimType(mode, f.anim.anims.length))+"")).queue();
        }

        CommonStatic.getConfig().ref = false;

        if(mode >= f.anim.anims.length)
            mode = 0;

        Message msg = ch.sendMessage(LangID.getStringByID("gif_anbox", lang)).complete();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        EAnimD<?> anim = f.getEAnim(getAnimType(mode, f.anim.anims.length));

        File img;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, false, limit, lang);
        }

        f.anim.unload();

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        long max;

        if(debug || limit > 0)
            max = getBoosterFileLimit(booster) * 1024L * 1024;
        else
            max = 8 * 1024 * 1024;

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("gif_faile", lang)).queue();

            return false;
        } else if(img.length() >= max && img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
            Message m = ch.sendMessage(LangID.getStringByID("gif_filesize", lang)).complete();

            if(m == null) {
                ch.sendMessage(LangID.getStringByID("gif_failcommand", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });

                return false;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.editMessage(LangID.getStringByID("gif_failimgur", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });
            } else {
                if(!debug && limit <= 0) {
                    String id = generateID(f, mode);

                    StaticStore.imgur.put(id, link, raw);
                }

                long finalEnd = System.currentTimeMillis();

                m.editMessage(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });

                GuildChannel chan = client.getGuildChannelById(StaticStore.UNITARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    ((GuildMessageChannel) chan).sendMessage(generateID(f, mode)+"\n\n"+link).queue();
                }
            }

            return true;
        } else if(img.length() < max) {
            final int fMode = mode;

            if(debug || limit > 0) {
                ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                        .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                        .queue(m -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                GuildChannel chan = client.getGuildChannelById(StaticStore.UNITARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    String siz = getFileSize(img);

                    ((GuildMessageChannel) chan).sendMessage(generateID(f, fMode))
                            .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                            .queue(m -> {
                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }

                                for(int i = 0; i < m.getAttachments().size(); i++) {
                                    Message.Attachment at = m.getAttachments().get(i);

                                    if(at.getFileName().startsWith("result.")) {
                                        ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl()).queue();
                                        break;
                                    }
                                }

                                cacheImage(f, fMode, m);
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateFormAnim - Failed to display form anim");

                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }
                            });
                }
            }
        } else {
            ch.sendMessage(LangID.getStringByID("gif_toobig", lang)).queue();
        }

        return true;
    }

    public static boolean generateEnemyAnim(Enemy en, MessageChannel ch, int booster, int mode, boolean debug, int limit, int lang, boolean raw, boolean gif) throws Exception {
        if(en.id == null)
            return false;
        else if(!debug && limit <= 0) {
            String id = generateID(en, mode);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                ch.sendMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).queue();
                return false;
            }
        }

        JDA client = ch.getJDA();

        en.anim.load();

        if(mode >= en.anim.anims.length)
            mode = 0;

        EAnimD<?> anim = en.getEAnim(getAnimType(mode, en.anim.anims.length));

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", limit+"")).queue();
        } else if(!raw && anim.len() >= 300) {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("gif_length", lang).replace("_", anim.len()+"")).queue();
        }

        CommonStatic.getConfig().ref = false;

        Message msg = ch.sendMessage(LangID.getStringByID("gif_anbox", lang)).complete();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        File img;

        long max;

        if(debug || limit > 0)
            max = getBoosterFileLimit(booster) * 1024L * 1024;
        else
            max = 8 * 1024 * 1024;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, false, limit, lang);
        }

        en.anim.unload();

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("gif_faile", lang)).queue();

            return false;
        } else if(img.length() >= max && img.length() < (raw ? 200 * 1024 * 1024 : 8 * 1024 * 1024)) {
            Message m = ch.sendMessage(LangID.getStringByID("gif_filesize", lang)).complete();

            if(m == null) {
                ch.sendMessage(LangID.getStringByID("gif_failcommand", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });
                return false;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.editMessage(LangID.getStringByID("gif_failimgur", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });
            } else {
                if(!debug && limit <= 0) {
                    String id = generateID(en, mode);

                    StaticStore.imgur.put(id, link, raw);
                }

                long finalEnd = System.currentTimeMillis();

                m.editMessage(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });

                GuildChannel chan = client.getGuildChannelById(StaticStore.ENEMYARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    ((GuildMessageChannel) chan).sendMessage(generateID(en, mode)+"\n\n"+link).queue();
                }
            }

            return true;
        } else if(img.length() < max) {
            final int fMode = mode;

            if(debug || limit > 0) {
                ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                        .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                        .queue(m -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                GuildChannel chan = client.getGuildChannelById(StaticStore.ENEMYARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    String siz = getFileSize(img);

                    ((GuildMessageChannel) chan).sendMessage(generateID(en, fMode))
                            .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                            .queue(m -> {
                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }

                                for(int i = 0; i < m.getAttachments().size(); i++) {
                                    Message.Attachment at = m.getAttachments().get(i);

                                    if(at.getFileName().startsWith("result.")) {
                                        ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl()).queue();
                                    }
                                }

                                cacheImage(en, fMode, m);
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }
                            });
                }
            }
        }

        return true;
    }

    public static void generateAnim(MessageChannel ch, AnimMixer mixer, int booster, int lang, boolean debug, int limit, boolean raw, boolean transparent, int index) throws Exception {
        boolean mix = mixer.mix();

        if(!mix) {
            ch.sendMessage("Failed to mix Anim").queue();
            return;
        }

        EAnimD<?> anim = mixer.getAnim(index);

        if(anim == null) {
            ch.sendMessage("Failed to generate anim instance").queue();
            return;
        }

        ch.sendMessage(LangID.getStringByID("gif_length", lang).replace("_", anim.len()+"")).queue();

        CommonStatic.getConfig().ref = false;

        Message msg = ch.sendMessage(LangID.getStringByID("gif_anbox", lang)).complete();

        if(msg == null)
            return;

        long start = System.currentTimeMillis();

        File img;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, transparent, lang, limit);
        }

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start)/1000.0);

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("gif_faile", lang)).queue();
        } else if(img.length() >= (long) getBoosterFileLimit(booster) * 1024 * 1024 && img.length() < (raw ? 200 * 1024 * 1024 : 10 * 1024 * 1024)) {
            Message m = ch.sendMessage(LangID.getStringByID("gif_filesize", lang)).complete();

            if(m == null) {
                ch.sendMessage(LangID.getStringByID("gif_failcommand", lang))
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate mixed anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });

                return;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.editMessage(LangID.getStringByID("gif_failimgur", lang))
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate mixed anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                long finalEnd = System.currentTimeMillis();

                m.editMessage(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate mixed anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });
            }
        } else if(img.length() < (long) getBoosterFileLimit(booster) * 1024 * 1024) {
            ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                    .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                    .queue(message -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateAnim - Failed to generate mixed anim");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerAnim | Can't delete file : "+img.getAbsolutePath());
                        }
                    });
        }
    }

    public static boolean generateBCAnim(MessageChannel ch, int booster, AnimMixer mixer, int lang) throws Exception {
        boolean mix = mixer.mix();

        if(!mix) {
            ch.sendMessage("Failed to mix Anim").queue();
            return false;
        }

        CommonStatic.getConfig().ref = false;

        Message msg = ch.sendMessage(LangID.getStringByID("gif_anbox", lang)).complete();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        File img = ImageDrawing.drawBCAnim(mixer, msg, 1.0, lang);

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start) / 1000.0);

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("gif_faile", lang)).queue();
        } else if(img.length() >= (long) getBoosterFileLimit(booster) * 1024 * 1024 && img.length() < 200 * 1024 * 1024) {
            Message m = ch.sendMessage(LangID.getStringByID("gif_filesize", lang)).complete();

            if(m == null) {
                ch.sendMessage(LangID.getStringByID("gif_failcommand", lang))
                        .queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                    }
                });

                return true;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.editMessage(LangID.getStringByID("gif_failimgur", lang))
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                long finalEnd = System.currentTimeMillis();

                m.editMessage(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                            }
                        });
            }
        } else if(img.length() < (long) getBoosterFileLimit(booster) * 1024 * 1024) {
            ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                    .addFiles(FileUpload.fromData(img, "result.mp4"))
                    .queue(message -> {
                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBCAnim - Failed to generate BC anim");

                        if(img.exists() && !img.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerBCAnim | Can't delete file : "+img.getAbsolutePath());
                        }
                    });
        }

        return true;
    }

    public static boolean generateBGAnim(MessageChannel ch, Background bg, int lang) throws Exception {
        Message message = ch.sendMessage(LangID.getStringByID("bg_prepare", lang)).complete();

        if(message == null)
            return false;

        JDA client = ch.getJDA();

        long start = System.currentTimeMillis();

        File result = ImageDrawing.drawBGAnimEffect(bg, message, lang);

        long end = System.currentTimeMillis();

        if(result == null) {
            ch.sendMessage(LangID.getStringByID("bg_fail", lang)).queue();
        } else if(result.length() >= 8 * 1024 * 1024) {
            ch.sendMessage(LangID.getStringByID("bg_toobig", lang).replace("_SSS_", getFileSize(result))).queue();
        } else {
            GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

            if(chan instanceof MessageChannel) {
                String siz = getFileSize(result);

                ((MessageChannel) chan).sendMessage("BG - "+Data.trio(bg.id.id))
                        .addFiles(FileUpload.fromData(result, "result.mp4"))
                        .queue(m -> {
                            if(result.exists() && !result.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBGAnim | Can't delete file : "+result.getAbsolutePath());
                            }

                            for(int i = 0; i < m.getAttachments().size(); i++) {
                                Message.Attachment at = m.getAttachments().get(i);

                                if(at.getFileName().startsWith("result.")) {
                                    ch.sendMessage(LangID.getStringByID("bg_animres", lang).replace("_SSS_", siz).replace("_TTT_", DataToString.df.format((end - start) / 1000.0))+"\n\n"+at.getUrl()).queue();

                                    StaticStore.imgur.put("BG - "+Data.trio(bg.id.id), at.getUrl(), true);
                                }
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateBGAnim - Failed to generate BG anim");

                            if(result.exists() && !result.delete()) {
                                StaticStore.logger.uploadLog("W/EntityHandlerBGAnim | Can't delete file : "+result.getAbsolutePath());
                            }
                        });
            }
        }

        return true;
    }

    public static boolean generateSoulAnim(Soul s, MessageChannel ch, int booster, boolean debug, int limit, int lang, boolean raw, boolean gif) throws Exception {
        if(s.getID() == null)
            return false;
        else if(!debug && limit <= 0) {
            String id = "SOUL - " + Data.trio(s.getID().id);

            String link = StaticStore.imgur.get(id, gif, raw);

            if(link != null) {
                ch.sendMessage(LangID.getStringByID("gif_cache", lang).replace("_", link)).queue();

                return false;
            }
        }

        JDA client = ch.getJDA();

        s.load();

        EAnimD<?> anim = s.getEAnim(Soul.SoulType.DEF);

        if(limit > 0)  {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", limit+"")).queue();
        } else if(!raw && anim.len() >= 300) {
            ch.sendMessage(LangID.getStringByID("gif_lengthlim", lang).replace("_", anim.len()+"").replace("-", 300+"")).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("gif_length", lang).replace("_", anim.len()+"")).queue();
        }

        CommonStatic.getConfig().ref = false;

        Message msg = ch.sendMessage(LangID.getStringByID("gif_anbox", lang)).complete();

        if(msg == null)
            return false;

        long start = System.currentTimeMillis();

        File img;

        long max;

        if(debug || limit > 0)
            max = getBoosterFileLimit(booster) * 1024L * 1024L;
        else
            max = 8 * 1024 * 1024;

        if(raw) {
            img = ImageDrawing.drawAnimMp4(anim, msg, 1.0, debug, limit, lang);
        } else {
            img = ImageDrawing.drawAnimGif(anim, msg, 1.0, debug, false, limit, lang);
        }

        s.unload();

        long end = System.currentTimeMillis();

        String time = DataToString.df.format((end - start) / 1000.0);

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("gif_faile", lang)).queue();

            return false;
        } else if(img.length() >= max && img.length() < (raw ? 200 * 1024 * 1024 : 8 * 1024 * 1024)) {
            Message m = ch.sendMessage(LangID.getStringByID("gif_filesize", lang)).complete();

            if(m == null) {
                ch.sendMessage(LangID.getStringByID("gif_failcommand", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });
                return false;
            }

            String link = StaticStore.imgur.uploadFile(img);

            if(link == null) {
                m.editMessage(LangID.getStringByID("gif_failimgur", lang)).queue(message -> {
                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                    if(img.exists() && !img.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                    }
                });
            } else {
                if(!debug && limit <= 0) {
                    String id = "SOUL - " + Data.trio(s.getID().id);

                    StaticStore.imgur.put(id, link, raw);
                }

                long finalEnd = System.currentTimeMillis();

                m.editMessage(LangID.getStringByID("gif_uploadimgur", lang).replace("_FFF_", getFileSize(img)).replace("_TTT_", DataToString.df.format((end-start) / 1000.0)).replace("_ttt_", DataToString.df.format((finalEnd-start) / 1000.0))+"\n"+link)
                        .queue(message -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateSoulAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });

                GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    ((GuildMessageChannel) chan).sendMessage("SOUL - " + Data.trio(s.getID().id) + "\n\n"+link).queue();
                }
            }

            return true;
        } else if(img.length() < max) {
            if(debug || limit > 0) {
                ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", getFileSize(img)))
                        .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                        .queue(m -> {
                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        }, e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                            if(img.exists() && !img.delete()) {
                                StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                            }
                        });
            } else {
                GuildChannel chan = client.getGuildChannelById(StaticStore.MISCARCHIVE);

                if(chan instanceof GuildMessageChannel) {
                    String siz = getFileSize(img);

                    ((GuildMessageChannel) chan).sendMessage("SOUL - " + Data.trio(s.getID().id))
                            .addFiles(FileUpload.fromData(img, raw ? "result.mp4" : "result.gif"))
                            .queue(m -> {
                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }

                                for(int i = 0; i < m.getAttachments().size(); i++) {
                                    Message.Attachment at = m.getAttachments().get(i);

                                    if(at.getFileName().startsWith("result.")) {
                                        ch.sendMessage(LangID.getStringByID("gif_done", lang).replace("_TTT_", time).replace("_FFF_", siz)+"\n\n"+at.getUrl()).queue();

                                        StaticStore.imgur.put("SOUL - " + Data.trio(s.getID().id), at.getUrl(), raw);
                                    }
                                }
                            }, e -> {
                                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyAnim - Failed to display enemy anim");

                                if(img.exists() && !img.delete()) {
                                    StaticStore.logger.uploadLog("Failed to delete file : "+img.getAbsolutePath());
                                }
                            });
                }
            }
        }

        return true;
    }

    private static String getFileSize(File f) {
        String[] unit = {"B", "KB", "MB"};

        double size = f.length();

        for (String s : unit) {
            if (size < 1024) {
                return DataToString.df.format(size) + s;
            } else {
                size /= 1024.0;
            }
        }

        return DataToString.df.format(size)+unit[2];
    }

    private static void cacheImage(Form f, int mode, Message msg) {
        if(f.unit == null || f.unit.id == null)
            return;

        String id = generateID(f, mode);

        List<Message.Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Message.Attachment a : att) {
            if (a.getFileName().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFileName().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    public static void getFormSprite(Form f, MessageChannel ch, int mode, int lang) throws Exception {
        if(f.unit == null || f.unit.id == null) {
            ch.sendMessage(LangID.getStringByID("fsp_cantunit", lang)).queue();
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        f.anim.load();

        FakeImage img;

        switch (mode) {
            case 0:
                img = f.anim.getNum();
                break;
            case 1:
                img = f.anim.getUni().getImg();
                break;
            case 2:
                if(f.unit.getCont() instanceof PackData.DefPack) {
                    String code;

                    if(f.fid == 0)
                        code = "f";
                    else if(f.fid == 1)
                        code = "c";
                    else
                        code = "s";

                    VFile vf = VFile.get("./org/unit/"+Data.trio(f.unit.id.id)+"/"+code+"/udi"+Data.trio(f.unit.id.id)+"_"+code+".png");

                    if(vf != null) {
                        img = vf.getData().getImg();
                    } else {
                        img = null;
                    }
                } else {
                    img = null;
                }
                break;
            case 3:
                img = f.anim.getEdi().getImg();
                break;
            default:
                throw new IllegalStateException("Mode in sprite getter is incorrect : "+mode);
        }

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("fsp_nodata", lang).replace("_", getIconName(mode, lang))).queue();
            return;
        }

        BufferedImage result = (BufferedImage) img.bimg();

        ImageIO.write(result, "PNG", image);

        String fName = StaticStore.safeMultiLangGet(f, lang);

        if(fName == null || fName.isBlank()) {
            fName = Data.trio(f.unit.id.id)+"-"+Data.trio(f.fid);
        }

        ch.sendMessage(LangID.getStringByID("fsp_result", lang).replace("_", fName).replace("===", getIconName(mode, lang)))
                .addFiles(FileUpload.fromData(image, "result.png"))
                .queue(m -> {
                    if(image.exists() && !image.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerFormSprite | Can't delete file : "+image.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::getFromSprite - Error happened while trying to display form sprite");

                    if(image.exists() && !image.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerFormSprite | Can't delete file : "+image.getAbsolutePath());
                    }
                });

        f.anim.unload();
    }

    public static void getEnemySprite(Enemy e, MessageChannel ch, int mode, int lang) throws Exception {
        if(e.id == null) {
            ch.sendMessage(LangID.getStringByID("esp_cantunit", lang)).queue();
            return;
        }

        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : "+temp.getAbsolutePath());
                return;
            }
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        e.anim.load();

        FakeImage img;

        switch (mode) {
            case 0:
                img = e.anim.getNum();
                break;
            case 1:
                img = e.anim.getUni().getImg();
                break;
            case 3:
                img = e.anim.getEdi().getImg();
                break;
            default:
                throw new IllegalStateException("Mode in sprite getter is incorrect : "+mode);
        }

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("fsp_nodata", lang).replace("_", getIconName(mode, lang))).queue();
            return;
        }

        BufferedImage result = (BufferedImage) img.bimg();

        ImageIO.write(result, "PNG", image);

        String fName = StaticStore.safeMultiLangGet(e, lang);

        if(fName == null || fName.isBlank()) {
            fName = Data.trio(e.id.id);
        }

        ch.sendMessage(LangID.getStringByID("fsp_result", lang).replace("_", fName).replace("===", getIconName(mode, lang)))
                .addFiles(FileUpload.fromData(image, "result.png"))
                .queue(m -> {
                    if(image.exists() && !image.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerEnemySprite | Can't delete file : "+image.getAbsolutePath());
                    }
                }, err -> {
                    StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::getEnemySprite - Error happened while trying to upload enemy sprite");

                    if(image.exists() && !image.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandlerEnemySprite | Can't delete file : "+image.getAbsolutePath());
                    }
                });

        e.anim.unload();
    }

    public static void getSoulSprite(Soul s, MessageChannel ch, int lang) throws Exception {
        if(s.getID() == null) {
            ch.sendMessage(LangID.getStringByID("soul_nosoul", lang)).queue();

            return;
        }

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/EntityHandler::getSoulSprite - Failed to create folder : " + temp.getAbsolutePath());

            return;
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null)
            return;

        s.load();

        FakeImage img = s.getNum();

        if(img == null) {
            ch.sendMessage(LangID.getStringByID("soul_nosoul", lang)).queue();

            return;
        }

        BufferedImage result = (BufferedImage) img.bimg();

        ImageIO.write(result, "PNG", image);

        Command.sendMessageWithFile(
                ch,
                LangID.getStringByID("soulspr_success", lang).replace("_", Data.trio(s.getID().id)),
                image
        );
    }

    public static void showMedalEmbed(int id, MessageChannel ch, int lang) throws  Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        File image = StaticStore.generateTempFile(temp, "result", ".png", false);

        if(image == null) {
            return;
        }

        String medalName = "./org/page/medal/medal_"+Data.trio(id);

        if(id <= 13 && lang != 3) {
            medalName += "_"+getLocaleName(lang);
        } else if(id == 90 && lang != 3) {
            medalName += "_en";
        }

        medalName += ".png";

        VFile vf = VFile.get(medalName);

        if(vf == null)
            ch.sendMessage(LangID.getStringByID("medal_nopng", lang)).queue();
        else {
            BufferedImage img = (BufferedImage) vf.getData().getImg().bimg();

            ImageIO.write(img, "PNG", image);

            EmbedBuilder e = new EmbedBuilder();

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String name = StaticStore.MEDNAME.getCont(id);
            String desc = StaticStore.MEDEXP.getCont(id);

            CommonStatic.getConfig().lang = oldConfig;

            if(StaticStore.medalData != null) {
                JsonObject obj = StaticStore.medalData.getAsJsonArray().get(id).getAsJsonObject();

                int grade = obj.get("grade").getAsInt();

                e.setColor(StaticStore.grade[grade]);
            }

            e.addField(name, desc, false);
            e.setImage("attachment://medal.png");

            ch.sendMessageEmbeds(e.build())
                    .addFiles(FileUpload.fromData(image, "medal.png"))
                    .queue(m -> {
                        if(image.exists() && !image.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerMedal | Can't delete file : "+image.getAbsolutePath());
                        }
                    }, err -> {
                        StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::showMedalEmbed - Error happened while trying to show medal embed");

                        if(image.exists() && !image.delete()) {
                            StaticStore.logger.uploadLog("W/EntityHandlerMedal | Can't delete file : "+image.getAbsolutePath());
                        }
                    });
        }
    }

    public static void showComboEmbed(MessageChannel ch, Combo c, int lang) throws Exception {
        File icon = generateComboImage(c);

        EmbedBuilder e = new EmbedBuilder();

        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String comboName = MultiLangCont.getStatic().COMNAME.getCont(c);

        CommonStatic.getConfig().lang = oldConfig;

        if(comboName == null || comboName.isBlank()) {
            comboName = "Combo "+c.name;
        }

        e.setTitle(comboName);

        if(c.lv == 0) {
            e.setColor(StaticStore.rainbow[4]);
        } else if(c.lv == 1) {
            e.setColor(StaticStore.rainbow[3]);
        } else if(c.lv == 2) {
            e.setColor(StaticStore.rainbow[2]);
        } else {
            e.setColor(StaticStore.rainbow[0]);
        }

        e.addField(DataToString.getComboType(c, lang), DataToString.getComboDescription(c, lang), false);

        if(icon != null) {
            e.setImage("attachment://combo.png");
        }

        MessageCreateAction action = ch.sendMessageEmbeds(e.build());

        if(icon != null)
            action = action.addFiles(FileUpload.fromData(icon, "combo.png"));

        action.queue(m -> {
            if(icon != null && icon.exists() && !icon.delete()) {
                StaticStore.logger.uploadLog("W/EntityHandlerCombo | Can't delete file : "+icon.getAbsolutePath());
            }
        }, err -> {
            StaticStore.logger.uploadErrorLog(err, "E/EntityHandler::showComboEmbed - Error happened while trying to show combo embed");

            if(icon != null && icon.exists() && !icon.delete()) {
                StaticStore.logger.uploadLog("W/EntityHandlerCombo | Can't delete file : "+icon.getAbsolutePath());
            }
        });
    }

    public static void generateStatImage(MessageChannel ch, List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskUnit[] units, String[] name, File container, File itemContainer, int lv, boolean isFrame, int[] egg, int[][] trueForm, boolean trueFormMode, int uid, int lang) throws Exception {
        List<List<CellDrawer>> cellGroup = new ArrayList<>();

        for(int i = 0; i < units.length; i++) {
            cellGroup.add(addCell(data, procData, abilData, traitData, units[i], lang, lv, isFrame));
        }

        String type = getRarity(units[0].rarity, lang);

        File result = ImageDrawing.drawStatImage(units, cellGroup, lv, name, type, container, itemContainer, trueFormMode, uid, egg, trueForm);

        if(result == null) {
            ch.sendMessage(LangID.getStringByID("stat_fail", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("stat_success", lang))
                    .addFiles(FileUpload.fromData(result, "stat.png"))
                    .queue(m -> {
                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStatImage - Failed to create stat image");

                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    });
        }
    }

    public static void generateEnemyStatImage(MessageChannel ch, List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskEnemy enemy, String name, File container, int m, boolean isFrame, int eid, int lang) throws Exception {
        List<CellDrawer> cellGroup = getEnemyCell(data, procData, abilData, traitData, enemy, lang, m, isFrame);

        File result = ImageDrawing.drawEnemyStatImage(cellGroup, LangID.getStringByID("stat_magnif", lang).replace("_", m+""), name, container, eid);

        if(result == null) {
            ch.sendMessage(LangID.getStringByID("stat_fail", lang)).queue();
        } else {
            ch.sendMessage(LangID.getStringByID("stat_success", lang))
                    .addFiles(FileUpload.fromData(result, "stat.png"))
                    .queue(msg -> {
                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    }, e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateEnemyStatImage - Failed to create stat image");

                        if(result.exists() && !result.delete()) {
                            StaticStore.logger.uploadLog("Failed to delete file : "+result.getAbsolutePath());
                        }
                    });
        }
    }

    public static void generateStageStatImage(MessageChannel ch, CustomStageMap map, int lv, boolean isFrame, int lang, String[] name, String code) throws Exception {
        List<List<CellDrawer>> cellGroups = new ArrayList<>();

        if(map.customIndex.isEmpty()) {
            for(int i = 0; i < map.list.size(); i++) {
                cellGroups.add(getStageCell(map, i, lang, lv, isFrame));
            }
        } else {
            for(int i = 0; i < map.customIndex.size(); i++) {
                cellGroups.add(getStageCell(map, map.customIndex.get(i), lang, lv, isFrame));
            }
        }

        List<File> results = new ArrayList<>();

        if(map.customIndex.isEmpty()) {
            for(int i = 0; i < map.list.size(); i++) {
                File result = ImageDrawing.drawStageStatImage(map, cellGroups.get(i), isFrame, lv, name[i], code + " - " + Data.trio(map.mapID % 1000) + " - " + Data.trio(i), i, lang);

                if(result != null) {
                    results.add(result);
                }
            }
        } else {
            for(int i = 0; i < map.customIndex.size(); i++) {
                File result = ImageDrawing.drawStageStatImage(map, cellGroups.get(i), isFrame, lv, name[i], code + " - " + Data.trio(map.mapID % 1000) + " - " + Data.trio(map.customIndex.get(i)), map.customIndex.get(i), lang);

                if(result != null) {
                    results.add(result);
                }
            }
        }

        int i = 0;

        Queue<File> done = new ArrayDeque<>();

        while(i < results.size()) {
            MessageCreateAction action = ch.sendMessage("Analyzed stage image");

            long fileSize = 0;

            while (i < results.size() && fileSize + results.get(i).length() < 8 * 1024 * 1024) {
                action = action.addFiles(FileUpload.fromData(results.get(i)));
                done.add(results.get(i));

                fileSize += results.get(i).length();
                i++;
            }

            action.queue(m -> {
                while(done.size() > 0) {
                    File target = done.poll();

                    if(target != null && target.exists() && !target.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandler::generateStageStatImage - Failed to delete file : " + target.getAbsolutePath());
                    }
                }
            }, e -> {
                StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::generateStageStatImage - Error happened while trying to upload analyzed stage image");

                while(done.size() > 0) {
                    File target = done.poll();

                    if(target != null && target.exists() && !target.delete()) {
                        StaticStore.logger.uploadLog("W/EntityHandler::generateStageStatImage - Failed to delete file : " + target.getAbsolutePath());
                    }
                }
            });
        }
    }

    private static List<CellDrawer> addCell(List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskUnit u, int lang, int lv, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        ArrayList<Integer> lvs = new ArrayList<>();

        lvs.add(lv);

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_hp", lang), LangID.getStringByID("data_hb", lang), LangID.getStringByID("data_speed", lang)},
                new String[] {DataToString.getHP(u, u.curve, false, lvs), DataToString.getHitback(u, false, lvs), DataToString.getSpeed(u, false , lvs)}
        ));

        cells.add(new NormalCellDrawer(new String[] {LangID.getStringByID("data_atk", lang)}, new String[] {DataToString.getAtk(u, u.curve, false, lvs)}));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_dps", lang), LangID.getStringByID("data_atktime", lang), LangID.getStringByID("data_abilt", lang)},
                new String[] {DataToString.getDPS(u, u.curve, false, lvs), DataToString.getAtkTime(u, isFrame), DataToString.getAbilT(u, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_preatk", lang), LangID.getStringByID("data_postatk", lang), LangID.getStringByID("data_tba", lang)},
                new String[] {DataToString.getPre(u, isFrame), DataToString.getPost(u, isFrame), DataToString.getTBA(u, isFrame)}
        ));

        StringBuilder trait = new StringBuilder(DataToString.getTrait(u, false, lvs, false, lang));

        for(int i = 0; i < traitData.size(); i++) {
            String t = traitData.get(i).dataToString(u.data);

            if(!t.isBlank()) {
                trait.append(", ").append(t);
            }
        }

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_trait", lang)},
                new String[] {trait.toString()}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_atktype", lang), LangID.getStringByID("data_cost", lang), LangID.getStringByID("data_range", lang)},
                new String[] {DataToString.getSiMu(u, lang), DataToString.getCost(u, false, lvs), DataToString.getRange(u)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_cooldown", lang)},
                new String[] {DataToString.getCD(u, isFrame, false, lvs)}
        ));

        List<List<CellData>> cellGroup = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            CellData d = data.get(i);

            List<CellData> group = new ArrayList<>();

            if(d.oneLine) {
                group.add(d);

                cellGroup.add(group);
            } else {
                int j = i;

                while(group.size() < 3 && !data.get(j).oneLine) {
                    group.add(data.get(j));

                    j++;

                    if(j >= data.size()) {
                        break;
                    }
                }

                j--;

                cellGroup.add(group);

                if(j > i) {
                    i = j;
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellData> group = cellGroup.get(i);

            String[] names = new String[group.size()];
            String[] contents = new String[group.size()];

            for(int j = 0; j < group.size(); j++) {
                names[j] = group.get(j).name;
                String c = group.get(j).dataToString(u.data, isFrame);

                if(c.isBlank()) {
                    contents[j] = LangID.getStringByID("data_none", lang);
                } else {
                    contents[j] = c;
                }
            }

            cells.add(new NormalCellDrawer(names, contents));
        }

        List<String> abil = Interpret.getAbi(u, false, lang);

        for(int i = 0; i < abilData.size(); i++) {
            String a = abilData.get(i).dataToString(u.data);

            if(!a.isBlank()) {
                abil.add(a);
            }
        }

        abil.addAll(Interpret.getProc(u, !isFrame, false, lang, 1.0, 1.0));

        for(int i = 0; i < procData.size(); i++) {
            String p = procData.get(i).beautify(u.data, isFrame);

            if(!p.isBlank()) {
                abil.add(p);
            }
        }

        if(abil.isEmpty()) {
            cells.add(new AbilityCellDrawer(LangID.getStringByID("data_ability", lang), new String[] {LangID.getStringByID("data_none", lang)}));
        } else {
            List<String> finalAbil = new ArrayList<>();

            for(int i = 0; i < abil.size(); i++) {
                finalAbil.add(" · " + abil.get(i));
            }

            cells.add(new AbilityCellDrawer(LangID.getStringByID("data_ability", lang), finalAbil.toArray(new String[0])));
        }

        return cells;
    }

    private static List<CellDrawer> getEnemyCell(List<CellData> data, List<AbilityData> procData, List<FlagCellData> abilData, List<FlagCellData> traitData, CustomMaskEnemy e, int lang, int m, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_hp", lang), LangID.getStringByID("data_hb", lang), LangID.getStringByID("data_speed", lang)},
                new String[] {DataToString.getHP(e, m), DataToString.getHitback(e), DataToString.getSpeed(e)}
        ));

        cells.add(new NormalCellDrawer(new String[] {LangID.getStringByID("data_atk", lang)}, new String[] {DataToString.getAtk(e, m)}));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_dps", lang), LangID.getStringByID("data_atktime", lang), LangID.getStringByID("data_abilt", lang)},
                new String[] {DataToString.getDPS(e, m), DataToString.getAtkTime(e, isFrame), DataToString.getAbilT(e, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_preatk", lang), LangID.getStringByID("data_postatk", lang), LangID.getStringByID("data_tba", lang)},
                new String[] {DataToString.getPre(e, isFrame), DataToString.getPost(e, isFrame), DataToString.getTBA(e, isFrame)}
        ));

        StringBuilder trait = new StringBuilder(DataToString.getTrait(e, false, lang));

        for(int i = 0; i < traitData.size(); i++) {
            String t = traitData.get(i).dataToString(e.data);

            if(!t.isBlank()) {
                trait.append(", ").append(t);
            }
        }

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_trait", lang)},
                new String[] {trait.toString()}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_atktype", lang), LangID.getStringByID("data_drop", lang), LangID.getStringByID("data_range", lang)},
                new String[] {DataToString.getSiMu(e, lang), DataToString.getDrop(e), DataToString.getRange(e)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_barrier", lang)},
                new String[] {DataToString.getBarrier(e, lang)}
        ));

        List<List<CellData>> cellGroup = new ArrayList<>();

        for(int i = 0; i < data.size(); i++) {
            CellData d = data.get(i);

            List<CellData> group = new ArrayList<>();

            if(d.oneLine) {
                group.add(d);

                cellGroup.add(group);
            } else {
                int j = i;

                while(group.size() < 3 && !data.get(j).oneLine) {
                    group.add(data.get(j));

                    j++;

                    if(j >= data.size()) {
                        break;
                    }
                }

                j--;

                cellGroup.add(group);

                if(j > i) {
                    i = j;
                }
            }
        }

        for(int i = 0; i < cellGroup.size(); i++) {
            List<CellData> group = cellGroup.get(i);

            String[] names = new String[group.size()];
            String[] contents = new String[group.size()];

            for(int j = 0; j < group.size(); j++) {
                names[j] = group.get(j).name;
                String c = group.get(j).dataToString(e.data, isFrame);

                if(c.isBlank()) {
                    contents[j] = LangID.getStringByID("data_none", lang);
                } else {
                    contents[j] = c;
                }
            }

            cells.add(new NormalCellDrawer(names, contents));
        }

        List<String> abil = Interpret.getAbi(e, false, lang);

        for(int i = 0; i < abilData.size(); i++) {
            String a = abilData.get(i).dataToString(e.data);

            if(!a.isBlank()) {
                abil.add(a);
            }
        }

        abil.addAll(Interpret.getProc(e, !isFrame, false, lang, 1.0, 1.0));

        for(int i = 0; i < procData.size(); i++) {
            String p = procData.get(i).beautify(e.data, isFrame);

            if(!p.isBlank()) {
                abil.add(p);
            }
        }

        if(abil.isEmpty()) {
            cells.add(new AbilityCellDrawer(LangID.getStringByID("data_ability", lang), new String[] {LangID.getStringByID("data_none", lang)}));
        } else {
            List<String> finalAbil = new ArrayList<>();

            for(int i = 0; i < abil.size(); i++) {
                finalAbil.add(" · " + abil.get(i));
            }

            cells.add(new AbilityCellDrawer(LangID.getStringByID("data_ability", lang), finalAbil.toArray(new String[0])));
        }

        return cells;
    }

    private static List<CellDrawer> getStageCell(CustomStageMap map, int index, int lang, int lv, boolean isFrame) {
        List<CellDrawer> cells = new ArrayList<>();

        Stage st = map.list.get(index);

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_energy", lang), LangID.getStringByID("data_base", lang), LangID.getStringByID("data_xp", lang), LangID.getStringByID("data_level", lang)},
                new String[] {DataToString.getEnergy(st, lang), DataToString.getBaseHealth(st), DataToString.getXP(st), DataToString.getLevelMagnification(map)},
                new BufferedImage[] {null, null, null, drawLevelImage(map.stars.length, lv)},
                new boolean[] {false, false ,false, true}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_music", lang), DataToString.getMusicChange(st), LangID.getStringByID("data_bg", lang), LangID.getStringByID("data_castle", lang)},
                new String[] {DataToString.getMusic(st, lang), DataToString.getMusic1(st, lang), DataToString.getBackground(st, lang), DataToString.getCastle(st, lang)}
        ));

        cells.add(new NormalCellDrawer(
                new String[] {LangID.getStringByID("data_continuable", lang), LangID.getStringByID("data_maxenem", lang), LangID.getStringByID("data_minspawn", lang), LangID.getStringByID("data_length", lang)},
                new String[] {DataToString.getContinuable(st, lang), DataToString.getMaxEnemy(st), DataToString.getMinSpawn(st, isFrame), DataToString.getLength(st)}
        ));

        List<String> limits = DataToString.getLimit(st.lim, map, lang);

        if(limits.isEmpty())
            limits.add(LangID.getStringByID("data_none", lang));

        cells.add(new AbilityCellDrawer(
                LangID.getStringByID("data_limit", lang),
                limits.toArray(new String[0])
        ));

        List<String> misc = DataToString.getMiscellaneous(st, lang);

        for(int i = 0; i < misc.size(); i++) {
            misc.set(i, " - " + misc.get(i));
        }

        if(misc.isEmpty())
            misc.add(LangID.getStringByID("data_none", lang));

        cells.add(new AbilityCellDrawer(
                LangID.getStringByID("data_misc", lang),
                misc.toArray(new String[0])
        ));

        return cells;
    }

    private static String getRarity(int type, int lang) {
        String rarity;

        if(type == 0)
            rarity = LangID.getStringByID("data_basic", lang);
        else if(type == 1)
            rarity = LangID.getStringByID("data_ex", lang);
        else if(type == 2)
            rarity = LangID.getStringByID("data_rare", lang);
        else if(type == 3)
            rarity = LangID.getStringByID("data_sr", lang);
        else if(type == 4)
            rarity = LangID.getStringByID("data_ur", lang);
        else if(type == 5)
            rarity = LangID.getStringByID("data_lr", lang);
        else
            rarity = "Unknown";

        return rarity;
    }

    private static File generateComboImage(Combo c) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists()) {
            boolean res = temp.mkdirs();

            if(!res) {
                System.out.println("Can't create folder : " + temp.getAbsolutePath());
                return null;
            }
        }

        File image = StaticStore.generateTempFile(temp, "combo", ".png", false);

        if(image == null) {
            return null;
        }

        BufferedImage img = new BufferedImage(600, 95, BufferedImage.TYPE_INT_ARGB);
        FG2D g = new FG2D(img.getGraphics());

        g.setRenderingHint(3, 2);
        g.enableAntialiasing();

        g.setColor(47, 49, 54, 255);
        g.fillRect(0, 0, 650, 105);

        g.setStroke(2f);
        g.setColor(230, 230, 230, 255);
        g.drawRect(0, 0, 600, 95);

        for(int i = 1; i < 5; i++) {
            g.drawLine(120 * i, 0, 120 * i, 95);
        }

        for(int i = 0; i < 5; i++) {
            if(i >= c.forms.length || c.forms[i] == null)
                continue;

            Unit u = c.forms[i].unit;

            if(u == null)
                continue;

            Form f = c.forms[i];

            f.anim.load();

            FakeImage icon = f.anim.getUni().getImg();

            g.drawImage(icon, 120 * i + 5, 5);

            f.anim.unload();
        }

        ImageIO.write(img, "PNG", image);

        return image;
    }

    private static String getIconName(int mode, int lang) {
        if(mode == 0)
            return LangID.getStringByID("fsp_sprite", lang);
        else if(mode == 1)
            return LangID.getStringByID("fsp_uni", lang);
        else if(mode == 2)
            return LangID.getStringByID("fsp_udi", lang);
        else
            return LangID.getStringByID("fsp_edi", lang);
    }

    private static void cacheImage(Enemy e, int mode, Message msg) {
        if(e.id == null)
            return;

        String id = generateID(e, mode);

        List<Message.Attachment> att = msg.getAttachments();

        if(att.isEmpty())
            return;

        for(Message.Attachment a : att) {
            if (a.getFileName().equals("result.gif")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, false);

                return;
            } else if(a.getFileName().equals("result.mp4")) {
                String link = a.getUrl();

                StaticStore.imgur.put(id, link, true);

                return;
            }
        }
    }

    private static String generateID(Enemy e, int mode) {
        if(e.id == null)
            return "";

        return "E - "+e.id.pack+" - "+Data.trio(e.id.id)+" - "+Data.trio(mode);
    }

    private static String generateID(Form f, int mode) {
        if(f.unit == null || f.unit.id == null)
            return "";

        return "F - "+f.unit.id.pack+" - "+ Data.trio(f.unit.id.id)+" - "+Data.trio(f.fid) + " - " + Data.trio(mode);
    }

    private static AnimU.UType getAnimType(int mode, int max) {
        switch (mode) {
            case 1:
                return AnimU.UType.IDLE;
            case 2:
                return AnimU.UType.ATK;
            case 3:
                return AnimU.UType.HB;
            case 4:
                if(max == 5)
                    return AnimU.UType.ENTER;
                else
                    return AnimU.UType.BURROW_DOWN;
            case 5:
                return AnimU.UType.BURROW_MOVE;
            case 6:
                return AnimU.UType.BURROW_UP;
            default:
                return AnimU.UType.WALK;
        }
    }

    private static String getLocaleName(int lang) {
        if(lang == 0)
            return "en";
        else if(lang == 1)
            return "tw";
        else if(lang == 2)
            return "kr";
        else
            return "";
    }

    private static boolean hasTwoMusic(Stage st) {
        return st.mush != 0 && st.mush != 100 && st.mus1 != null && st.mus0 != null && st.mus1.id != st.mus0.id;
    }

    private static boolean canFirstForm(Form f) {
        return f.unit != null && f.fid - 2 >= 0;
    }

    private static boolean canPreviousForm(Form f) {
        return f.unit != null && f.fid - 1 >= 0;
    }

    private static boolean canNextForm(Form f) {
        return f.unit != null && f.fid + 1 < f.unit.forms.length;
    }

    private static boolean canFinalForm(Form f) {
        return f.unit != null && f.fid + 2 < f.unit.forms.length;
    }

    private static int getBoosterFileLimit(int level) {
        switch (level) {
            case 2:
                return 50;
            case 3:
                return 100;
            default:
                return 8;
        }
    }

    private static BufferedImage drawLevelImage(int max, int lv) {
        try {
            BufferedImage crownOn = ImageIO.read(new File("./data/bot/icons/crownOn.png"));
            BufferedImage crownOff = ImageIO.read(new File("./data/bot/icons/crownOff.png"));

            BufferedImage result = new BufferedImage(crownOn.getWidth() * max + 10 * (max - 1), crownOn.getHeight(), BufferedImage.TYPE_INT_ARGB);

            FG2D g = new FG2D(result.getGraphics());

            int x = 0;

            for(int i = 0; i < max; i++) {
                if(i > lv) {
                    g.drawImage(crownOff, x, 0);
                } else {
                    g.drawImage(crownOn, x, 0);
                }

                x += crownOn.getWidth() + 10;
            }

            return result;
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EntityHandler::drawLevelImage - Failed to generate level image");
        }

        return null;
    }
}

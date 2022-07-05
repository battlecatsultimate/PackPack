package mandarin.packpack.supporter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;

public class EmoteStore {
    public static void initialize(JDA jda) {
        TWO_PREVIOUS = StaticStore.getEmoteWitNameAndID(jda, "TwoPrevious", 993716493458083900L, false, true);
        PREVIOUS = StaticStore.getEmoteWitNameAndID(jda, "Previous", 993716355817807883L, false, true);
        NEXT = StaticStore.getEmoteWitNameAndID(jda, "Next", 993716605450203146L, false, true);
        TWO_NEXT = StaticStore.getEmoteWitNameAndID(jda, "TwoNext", 993716631589113857L, false, true);
        CASTLE = StaticStore.getEmoteWitNameAndID(jda, "Castle", 993716694436544574L, false, true);
        BACKGROUND = StaticStore.getEmoteWitNameAndID(jda, "Background", 993716717123555408L, false, true);
        MUSIC = StaticStore.getEmoteWitNameAndID(jda, "Music", 993716741421158420L, false, true);
        MUSIC_BOSS = StaticStore.getEmoteWitNameAndID(jda, "MusicBoss", 993716761855787069L, false, true);
        CROWN_OFF = StaticStore.getEmoteWitNameAndID(jda, "CrownOff", 993716814389444709L, false, true);
        CROWN_ON = StaticStore.getEmoteWitNameAndID(jda, "CrownOn", 993716790813261884L, false, true);
        TREASURE_RADAR = StaticStore.getEmoteWitNameAndID(jda, "TreasureRadar", 993716433387261992L, false, true);
        UDP = StaticStore.getEmoteWitNameAndID(jda, "UDP", 993716659904847912L, false, true);
    }    
    
    public static Emote TWO_PREVIOUS;
    public static Emote PREVIOUS;
    public static Emote NEXT;
    public static Emote TWO_NEXT;
    public static Emote CASTLE;
    public static Emote BACKGROUND;
    public static Emote MUSIC;
    public static Emote MUSIC_BOSS;
    public static Emote CROWN_OFF;
    public static Emote CROWN_ON;
    public static Emote TREASURE_RADAR;
    public static Emote UDP;
}

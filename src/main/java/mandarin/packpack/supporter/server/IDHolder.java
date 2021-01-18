package mandarin.packpack.supporter.server;

public class IDHolder {
    public final String DEV;
    public final String MOD;
    public final String MEMBER;
    public final String PRE_MEMBER;
    public final String BCU_PC_USER;
    public final String BCU_ANDROID;
    public final String MUTED;

    public final String BOT_COMMAND;

    public IDHolder(String d, String m, String me, String pre, String pc, String and, String bot, String mu) {
        this.DEV = d;
        this.MOD = m;
        this.MEMBER = me;
        this.PRE_MEMBER = pre;
        this.BCU_PC_USER = pc;
        this.BCU_ANDROID = and;
        this.BOT_COMMAND = bot;
        this.MUTED = mu;
    }
}

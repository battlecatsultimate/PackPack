package mandarin.packpack;

import mandarin.packpack.supporter.server.slash.SlashBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class SlashCommandUpdater {
    public static void main(String[] args) throws LoginException {
        final String TOKEN = args[0];

        JDA client = JDABuilder.createDefault(TOKEN).build();

        SlashBuilder.build(client);
    }
}

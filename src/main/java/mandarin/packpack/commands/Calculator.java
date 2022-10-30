package mandarin.packpack.commands;

import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Calculator extends ConstraintCommand {

    public Calculator(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] equation = getContent(event).split(" ", 2);

        if(equation.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("calc_eq", lang), getMessage(event), a -> a);

            return;
        }

        double result = Equation.calculate(equation[1].replace(" ", ""), null);

        if(Equation.error.isEmpty()) {
            replyToMessageSafely(ch, LangID.getStringByID("calc_result", lang).replace("_", Equation.df.format(result)), getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, Equation.getErrorMessage(lang), getMessage(event), a -> a);
        }
    }
}

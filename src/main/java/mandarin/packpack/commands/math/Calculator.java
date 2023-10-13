package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Calculator extends ConstraintCommand {

    public Calculator(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
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

        BigDecimal result = Equation.calculate(equation[1].replace(" ", ""), null, false, lang);

        if(Equation.error.isEmpty()) {
            DecimalFormat df = new DecimalFormat("#.########");

            String value = df.format(result);

            if(value.length() > 1500) {
                value = Equation.formatNumber(result);
            }

            replyToMessageSafely(ch, String.format(LangID.getStringByID("calc_result", lang), value), getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, Equation.getErrorMessage(), getMessage(event), a -> a);
        }
    }
}

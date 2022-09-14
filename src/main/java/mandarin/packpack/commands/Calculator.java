package mandarin.packpack.commands;

import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.MessageChannel;
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
            createMessageWithNoPings(ch, LangID.getStringByID("calc_eq", lang));

            return;
        }

        double result = Equation.calculate(equation[1].replace(" ", ""), null);

        if(Equation.error.isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("calc_result", lang).replace("_", Equation.df.format(result)));
        } else {
            createMessageWithNoPings(ch, Equation.getErrorMessage(lang));
        }
    }
}

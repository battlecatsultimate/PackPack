package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import common.util.unit.Unit;

import java.util.ArrayList;
import java.util.Locale;

public class EntityFilter {
    public static ArrayList<Form> findUnitWithName(String name) {
        ArrayList<Form> res = new ArrayList<>();

        for(Unit u : UserProfile.getBCData().units.getList()) {
            if(u == null)
                continue;

            for(Form f : u.forms) {
                for(int i = 0; i < 4; i++) {
                    CommonStatic.getConfig().lang = i;
                    StringBuilder fname = new StringBuilder(Data.trio(u.id.id) + " ");
                    if(MultiLangCont.get(f) != null) {
                        fname.append(MultiLangCont.get(f));
                    }

                    if(fname.toString().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                        res.add(f);
                        break;
                    }
                }
            }
        }

        return res;
    }
}

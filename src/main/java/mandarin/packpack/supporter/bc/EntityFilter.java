package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
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
                    StringBuilder fname = new StringBuilder(Data.trio(u.id.id)+"-"+Data.trio(f.fid)+" "+Data.trio(u.id.id)+" - "+Data.trio(f.fid) + " ");
                    fname.append(Data.trio(u.id.id)).append(Data.trio(f.fid)).append(" ");
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

    public static ArrayList<Enemy> findEnemyWithName(String name) {
        ArrayList<Enemy> res = new ArrayList<>();

        for(Enemy e : UserProfile.getBCData().enemies.getList()) {
            if(e == null)
                continue;

            for(int i = 0; i < 4; i++) {
                CommonStatic.getConfig().lang = i;
                StringBuilder ename = new StringBuilder(Data.trio(e.id.id))
                        .append(" ").append(duo(i)).append(" ");

                if(MultiLangCont.get(e) != null) {
                    ename.append(MultiLangCont.get(e));
                }

                if(ename.toString().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                    res.add(e);
                    break;
                }
            }
        }

        return res;
    }

    private static String duo(int i) {
        if(i < 10)
            return "0"+i;
        else
            return String.valueOf(i);
    }
}

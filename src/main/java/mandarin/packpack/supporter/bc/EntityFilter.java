package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
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
                    StringBuilder fname = new StringBuilder(Data.trio(u.id.id)+"-"+Data.trio(f.fid)+" "+Data.trio(u.id.id)+" - "+Data.trio(f.fid) + " "
                    +u.id.id+"-"+f.fid+" "+Data.trio(u.id.id)+"-"+f.fid+" ");
                    fname.append(Data.trio(u.id.id)).append(Data.trio(f.fid)).append(" ");

                    if(MultiLangCont.get(f) != null) {
                        fname.append(MultiLangCont.get(f));
                    }

                    if(f.name != null) {
                        fname.append(" ").append(f.name);
                    }

                    if(fname.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
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

                if(ename.toString().toLowerCase(Locale.ENGLISH).contains(name.toLowerCase(Locale.ENGLISH))) {
                    res.add(e);
                    break;
                }
            }
        }

        return res;
    }

    public static ArrayList<Stage> findStageWithName(String[] names) {
        ArrayList<Stage> res = new ArrayList<>();

        for(MapColc mc : MapColc.values()) {
            if(mc == null)
                continue;

            if(searchMapColc(names) && names[0] != null && !names[0].isBlank()) {
                for(int i = 0; i < 4; i++) {
                    CommonStatic.getConfig().lang = i;

                    String mcName = MultiLangCont.get(mc);

                    if(mcName == null || mcName.isBlank())
                        continue;

                    if(!mcName.isBlank()) {
                        if(mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH))) {
                            for(StageMap stm : mc.maps.getList()) {
                                if(stm == null)
                                    continue;

                                for(Stage st : stm.list.getList()) {
                                    if(st == null)
                                        continue;

                                    res.add(st);
                                }
                            }

                            break;
                        }
                    }
                }

                continue;
            }

            for(StageMap stm : mc.maps.getList()) {
                if(stm == null)
                    continue;

                if(searchStageMap(names) && names[1] != null && !names[1].isBlank()) {
                    for(int i = 0; i < 4; i++) {
                        CommonStatic.getConfig().lang = i;

                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = MultiLangCont.get(mc);

                            if(mcName != null && !mcName.isBlank()) {
                                s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = MultiLangCont.get(stm);

                            if(stmName == null || stmName.isBlank())
                                continue;

                            if(!stmName.isBlank()) {
                                s1 = stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        if(s0 && s1) {
                            for(Stage st : stm.list.getList()) {
                                if(st == null)
                                    continue;

                                res.add(st);
                            }

                            break;
                        }
                    }

                    continue;
                }

                for(Stage st : stm.list.getList()) {
                    for(int i = 0; i < 4; i++) {
                        CommonStatic.getConfig().lang = i;

                        if(names[2] == null)
                            continue;

                        String stName = MultiLangCont.get(st);

                        if(stName == null || stName.isBlank())
                            continue;

                        boolean s0 = true;

                        if(names[0] != null && !names[0].isBlank()) {
                            String mcName = MultiLangCont.get(mc);

                            if(mcName != null && !mcName.isBlank()) {
                                s0 = mcName.toLowerCase(Locale.ENGLISH).contains(names[0].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        boolean s1 = true;

                        if(names[1] != null && !names[1].isBlank()) {
                            String stmName = MultiLangCont.get(stm);

                            if(stmName != null && !stmName.isBlank()) {
                                s1 = stmName.toLowerCase(Locale.ENGLISH).contains(names[1].toLowerCase(Locale.ENGLISH));
                            }
                        }

                        boolean s2 = false;

                        if(!stName.isBlank()) {
                            s2 = stName.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH));
                        }

                        String id = mc.getSID()+"-"+Data.trio(stm.id.id)+"-"+Data.trio(st.id.id)+" "+mc.getSID()+"-"+stm.id.id+"-"+st.id.id;

                        boolean s3 = id.toLowerCase(Locale.ENGLISH).contains(names[2].toLowerCase(Locale.ENGLISH));

                        if(s0 && s1 && (s2 || s3)) {
                            res.add(st);

                            break;
                        }
                    }
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

    private static boolean searchMapColc(String[] names) {
        return names[1] == null && names[2] == null;
    }

    private static boolean searchStageMap(String[] names) {
        return names[2] == null;
    }
}

package mandarin.packpack.commands;

import common.battle.data.MaskAtk;
import common.battle.data.MaskUnit;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.*;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.calculation.Equation;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1), true);
    }

    @Override
    protected void doThing(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if (ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(ch, "`p!test [Unit ID]`", getMessage(event), a -> a);

            return;
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, "ID must be numeric", getMessage(event), a -> a);

            return;
        }

        Unit u = UserProfile.getBCData().units.get(StaticStore.safeParseInt(contents[1]));
        Form f = u.forms[u.forms.length - 1];

        List<BigDecimal> nodes = new ArrayList<>();

        if (!f.du.isLD() && !f.du.isOmni()) {
            nodes.add(new BigDecimal("-320"));
            nodes.add(BigDecimal.valueOf(f.du.getRange()));
        } else {
            if (DataToString.allRangeSame(f.du)) {
                MaskAtk attack = f.du.getAtkModel(0);

                int shortPoint = attack.getShortPoint();
                int width = attack.getLongPoint() - attack.getShortPoint();

                nodes.add(BigDecimal.valueOf(Math.min(shortPoint, shortPoint + width)));
                nodes.add(BigDecimal.valueOf(Math.max(shortPoint, shortPoint + width)));
            } else {
                for (int i = 0; i < f.du.getAtkCount(); i++) {
                    MaskAtk attack = f.du.getAtkModel(i);

                    int shortPoint = attack.getShortPoint();
                    int width = attack.getLongPoint() - attack.getShortPoint();

                    nodes.add(BigDecimal.valueOf(Math.min(shortPoint, shortPoint + width)));
                    nodes.add(BigDecimal.valueOf(Math.max(shortPoint, shortPoint + width)));
                }
            }
        }

        MaskAtk representativeAttack = f.du.getRepAtk();

        common.util.Data.Proc.VOLC surgeAbility = representativeAttack.getProc().VOLC;
        common.util.Data.Proc.MINIVOLC miniSurgeAbility = representativeAttack.getProc().MINIVOLC;

        boolean surge = surgeAbility.exists() || miniSurgeAbility.exists();

        BigDecimal shortSurgeDistance = BigDecimal.ZERO;
        BigDecimal longSurgeDistance = BigDecimal.ZERO;
        BigDecimal surgeLevel = BigDecimal.ZERO;
        BigDecimal surgeChance = BigDecimal.ZERO;
        BigDecimal surgeMultiplier = BigDecimal.ZERO;

        if (surge) {
            if (surgeAbility.exists()) {
                shortSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(surgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(surgeAbility.time).divide(BigDecimal.valueOf(Data.VOLC_ITV), Equation.context);
                surgeChance = BigDecimal.valueOf(surgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.ONE;
            } else {
                shortSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_0);
                longSurgeDistance = BigDecimal.valueOf(miniSurgeAbility.dis_1);
                surgeLevel = BigDecimal.valueOf(miniSurgeAbility.time).divide(new BigDecimal("20"), Equation.context);
                surgeChance = BigDecimal.valueOf(miniSurgeAbility.prob).divide(new BigDecimal("100"), Equation.context);
                surgeMultiplier = BigDecimal.valueOf(miniSurgeAbility.mult).divide(new BigDecimal("100"), Equation.context);
            }
        }

        BigDecimal chance = BigDecimal.ZERO;

        if (surge) {
            BigDecimal minimumDistance = shortSurgeDistance.min(longSurgeDistance);
            BigDecimal maximumDistance = shortSurgeDistance.max(longSurgeDistance);

            nodes.add(minimumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER)));
            nodes.add(maximumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE)));

            BigDecimal minimumPierce = minimumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));
            BigDecimal maximumInner = maximumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));

            nodes.add(minimumPierce);
            nodes.add(maximumInner);

            if (minimumPierce.subtract(maximumInner).compareTo(BigDecimal.ZERO) > 0) {
                chance = BigDecimal.ONE;
            } else {
                chance = BigDecimal.valueOf(Data.W_VOLC_INNER + Data.W_VOLC_PIERCE)
                        .divide(maximumDistance.subtract(minimumDistance), Equation.context);
            }
        }

        nodes = new ArrayList<>(new HashSet<>(nodes));

        BigDecimal minimumValue = new BigDecimal("-320");
        BigDecimal maximumValue = new BigDecimal("-320");

        for (int i = 0; i < nodes.size(); i++) {
            minimumValue = minimumValue.min(nodes.get(i));
            maximumValue = maximumValue.max(nodes.get(i));
        }

        nodes.add(minimumValue.subtract(new BigDecimal("100")));
        nodes.add(maximumValue.add(new BigDecimal("100")));

        nodes.sort(BigDecimal::compareTo);

        System.out.println(nodes);

        List<BigDecimal[]> coordinates = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            BigDecimal range = nodes.get(i);

            List<Integer> possibleAttack = getAttackIndex(range, f);

            BigDecimal y1 = BigDecimal.ZERO;
            BigDecimal y2 = BigDecimal.ZERO;

            if (surge && inSurgeArea(range, shortSurgeDistance, longSurgeDistance)) {
                BigDecimal multiplier = chance
                        .multiply(surgeChance)
                        .multiply(surgeLevel)
                        .multiply(surgeMultiplier);

                BigDecimal surgeDamage = getTotalSurgeAttack(f.du, f.unit.lv, new Level(50, 0, new int[] { 0, 0, 0, 0, 0 }), TreasureHolder.global, true, false);

                if (inSurgeIntersectionArea(range, shortSurgeDistance, longSurgeDistance)) {
                    y1 = y1.add(multiplier.multiply(surgeDamage));
                    y2 = y2.add(multiplier.multiply(surgeDamage));
                } else {
                    BigDecimal minimumSurgeDistance = shortSurgeDistance.min(longSurgeDistance);
                    BigDecimal maximumSurgeDistance = shortSurgeDistance.max(longSurgeDistance);

                    BigDecimal minimumSurgeRange = minimumSurgeDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));
                    BigDecimal maximumSurgeRange = maximumSurgeDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));

                    BigDecimal minimumPierce = minimumSurgeDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));
                    BigDecimal maximumInner = maximumSurgeDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));

                    BigDecimal value = BigDecimal.ZERO;

                    if (range.compareTo(minimumPierce.min(maximumInner)) <= 0 && range.compareTo(minimumSurgeRange) >= 0) {
                        value = multiplier.multiply(surgeDamage).divide(minimumPierce.min(maximumInner).subtract(minimumSurgeRange), Equation.context).multiply(range.subtract(minimumSurgeRange));
                    } else if (range.compareTo(minimumPierce.max(maximumInner)) >= 0 && range.compareTo(maximumSurgeRange) <= 0) {
                        value = multiplier.multiply(surgeDamage).divide(minimumPierce.max(maximumInner).subtract(maximumSurgeRange), Equation.context).multiply(range.subtract(maximumSurgeRange));
                    }

                    y1 = y1.add(value);
                    y2 = y2.add(value);
                }
            }

            for (int j = 0; j < possibleAttack.size(); j++) {
                int rawIndex = possibleAttack.get(j);

                int index;

                if (rawIndex >= 1000)
                    index = rawIndex - 1000;
                else if (rawIndex <= -1000)
                    index = -rawIndex - 1000;
                else
                    index = rawIndex;

                BigDecimal damage = getAttack(index, f.du, f.unit.lv, new Level(50, 0, new int[] { 0, 0, 0, 0, 0 }), TreasureHolder.global, true, false);

                if (rawIndex >= 1000)
                    y2 = y2.add(damage);
                else if (rawIndex <= -1000)
                    y1 = y1.add(damage);
                else {
                    y1 = y1.add(damage);
                    y2 = y2.add(damage);
                }
            }

            coordinates.add(new BigDecimal[] { range, y1 });

            if (y1.compareTo(y2) != 0) {
                coordinates.add(new BigDecimal[] { range, y2 });
            }
        }

        for (int i = 0; i < coordinates.size(); i++) {
            coordinates.get(i)[1] = coordinates.get(i)[1].divide(BigDecimal.valueOf(f.du.getItv()).divide(new BigDecimal("30"), Equation.context), Equation.context);
        }

        BigDecimal maximumDamage = BigDecimal.ZERO;
        BigDecimal minimumX = new BigDecimal("-320");
        BigDecimal maximumX = new BigDecimal("-320");

        for (int i = 0; i < coordinates.size(); i++) {
            maximumDamage = maximumDamage.max(coordinates.get(i)[1]);

            minimumX = minimumX.min(coordinates.get(i)[0]);
            maximumX = maximumX.max(coordinates.get(i)[0]);
        }

        ImageDrawing.plotDPSGraph(coordinates.toArray(new BigDecimal[0][0]), null, new BigDecimal[] { minimumX, maximumX }, new BigDecimal[] { BigDecimal.ZERO, maximumDamage.multiply(new BigDecimal("1.1")) }, 0);
    }

    @Override
    protected void setOptionalID(GenericMessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }

    private boolean inSurgeArea(BigDecimal range, BigDecimal shortSurgeDistance, BigDecimal longSurgeDistance) {
        BigDecimal minimumDistance = shortSurgeDistance.min(longSurgeDistance).subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));
        BigDecimal maximumDistance = shortSurgeDistance.max(longSurgeDistance).add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));

        return minimumDistance.compareTo(range) <= 0 && range.compareTo(maximumDistance) <= 0;
    }

    private boolean inSurgeIntersectionArea(BigDecimal range, BigDecimal shortSurgeDistance, BigDecimal longSurgeDistance) {
        BigDecimal minimumDistance = shortSurgeDistance.min(longSurgeDistance);
        BigDecimal maximumDistance = shortSurgeDistance.max(longSurgeDistance);

        BigDecimal minimumPierce = minimumDistance.add(BigDecimal.valueOf(Data.W_VOLC_PIERCE));
        BigDecimal maximumInner = maximumDistance.subtract(BigDecimal.valueOf(Data.W_VOLC_INNER));

        return maximumInner.min(minimumPierce).compareTo(range) <= 0 && range.compareTo(maximumInner.max(minimumPierce)) <= 0;
    }

    private List<Integer> getAttackIndex(BigDecimal range, Form f) {
        List<Integer> result = new ArrayList<>();

        if (!f.du.isOmni() && !f.du.isLD()) {
            if (new BigDecimal("-320").compareTo(range) <= 0 && range.compareTo(BigDecimal.valueOf(f.du.getRange())) <= 0) {
                if (range.compareTo(new BigDecimal("-320")) == 0) {
                    result.add(1000);
                } else if (range.compareTo(BigDecimal.valueOf(f.du.getRange())) == 0) {
                    result.add(-1000);
                } else {
                    result.add(0);
                }
            }
        } else {
            if (DataToString.allRangeSame(f.du)) {
                MaskAtk attack = f.du.getAtkModel(0);

                BigDecimal shortPoint = BigDecimal.valueOf(attack.getShortPoint());
                BigDecimal width = BigDecimal.valueOf(attack.getLongPoint() - attack.getShortPoint());

                BigDecimal minimumDistance = shortPoint.min(shortPoint.add(width));
                BigDecimal maximumDistance = shortPoint.max(shortPoint.add(width));

                if (minimumDistance.compareTo(range) <= 0 && range.compareTo(maximumDistance) <= 0) {
                    for (int i = 0; i < f.du.getAtkCount(); i++) {
                        if (range.compareTo(minimumDistance) == 0) {
                            result.add(1000 + i);
                        } else if (range.compareTo(maximumDistance) == 0) {
                            result.add(-1000 - i);
                        } else {
                            result.add(i);
                        }
                    }
                }
            } else {
                for (int i = 0; i < f.du.getAtkCount(); i++) {
                    MaskAtk attack = f.du.getAtkModel(i);

                    BigDecimal shortPoint = BigDecimal.valueOf(attack.getShortPoint());
                    BigDecimal width = BigDecimal.valueOf(attack.getLongPoint() - attack.getShortPoint());

                    BigDecimal minimumDistance = shortPoint.min(shortPoint.add(width));
                    BigDecimal maximumDistance = shortPoint.max(shortPoint.add(width));

                    if (minimumDistance.compareTo(range) <= 0 && range.compareTo(maximumDistance) <= 0) {
                        if (range.compareTo(minimumDistance) == 0) {
                            result.add(1000 + i);
                        } else if (range.compareTo(maximumDistance) == 0) {
                            result.add(-1000 - i);
                        } else {
                            result.add(i);
                        }
                    }
                }
            }
        }

        return result;
    }

    private BigDecimal getTotalSurgeAttack(MaskUnit data, UnitLevel levelCurve, Level lv, TreasureHolder t, boolean talent, boolean treasure) {
        BigDecimal result = BigDecimal.ZERO;

        for (int i = 0; i < data.getAtkCount(); i++) {
            boolean abilityApplied = data.rawAtkData()[i][2] == 1;

            if (abilityApplied) {
                result = result.add(getAttack(i, data, levelCurve, lv, t, talent, treasure));
            }
        }

        return result;
    }

    private BigDecimal getAttack(int index, MaskUnit data, UnitLevel levelCurve, Level lv, TreasureHolder t, boolean talent, boolean treasure) {
        MaskAtk attack = data.getAtkModel(index);

        int result;

        if(data.getPCoin() != null && talent) {
            result = (int) ((int) (Math.round(attack.getAtk() * levelCurve.getMult(lv.getLv() + lv.getPlusLv())) * t.getAtkMultiplier()) * data.getPCoin().getAtkMultiplication(lv.getTalents()));
        } else {
            result = (int) (Math.round(attack.getAtk() * levelCurve.getMult(lv.getLv() + lv.getPlusLv())) * t.getAtkMultiplier());
        }

        if(treasure) {
            List<Trait> traits = data.getTraits();

            if((data.getAbi() & Data.AB_GOOD) > 0) {
                result = (int) (result * t.getStrongAttackMultiplier(traits));
            }

            if((data.getAbi() & Data.AB_MASSIVE) > 0) {
                result = (int) (result * t.getMassiveAttackMultiplier(traits));
            }

            if((data.getAbi() & Data.AB_MASSIVES) > 0) {
                result = (int) (result * t.getInsaneMassiveAttackMultiplier(traits));
            }
        }

        return BigDecimal.valueOf(result);
    }
}
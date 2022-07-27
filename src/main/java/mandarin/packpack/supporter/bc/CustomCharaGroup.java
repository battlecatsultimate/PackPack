package mandarin.packpack.supporter.bc;

import common.pack.Identifier;
import common.util.stage.CharaGroup;
import common.util.unit.Unit;

import java.util.List;

public class CustomCharaGroup extends CharaGroup {
    public final List<Identifier<Unit>> identifiers;

    public CustomCharaGroup(List<Identifier<Unit>> identifiers, int type) {
        this.identifiers = identifiers;
        this.type = type;
    }
}

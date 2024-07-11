package mandarin.packpack.supporter.event;

import common.CommonStatic;

@SuppressWarnings("unused")
public interface Schedule {
    String beautify(CommonStatic.Lang.Locale lang);
    String dataToString(CommonStatic.Lang.Locale lang);
}

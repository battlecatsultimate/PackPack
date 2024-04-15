package mandarin.packpack.supporter.server.holder;

public interface Conflictable {
    boolean isConflicted(Holder holder);

    void onConflict();
}

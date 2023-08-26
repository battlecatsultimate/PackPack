package mandarin.packpack.supporter;

import common.io.Backup;
import common.io.PackLoader;
import common.io.assets.Admin;
import common.pack.Context;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

public class PackContext implements Context {
    private static final String[] ANIMFL = { ".imgcut", ".mamodel", ".maanim" };

    @Override
    public boolean confirmDelete() {
        return true;
    }

    @Override
    public boolean confirmDelete(File f) {
        return true;
    }

    @Override
    public File getAssetFile(String string) {
        return new File("./data/assets/" + string);
    }

    @Override
    public File getAuxFile(String string) {
        return new File(string);
    }

    @Override
    public InputStream getLangFile(String file) {
        try {
            return new FileInputStream("./data/lang/" + file);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to open lang file");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public File getUserFile(String string) {
        return new File("./user/" + string);
    }

    @Override
    public File getWorkspaceFile(String relativePath) {
        return new File("./workspace/" + relativePath);
    }

    @Override
    public File getBackupFile(String string) {
        return null;
    }

    @NotNull
    @Override
    public File getBCUFolder() {
        return new File("./");
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public void initProfile() {
    }

    @Override
    public void noticeErr(Exception e, ErrType t, String str) {
        printErr(t, str);

        StaticStore.logger.uploadErrorLog(e, t.name() + " - " + str);

        e.printStackTrace(t == ErrType.INFO ? System.out : System.err);
    }

    @Override
    public boolean preload(PackLoader.ZipDesc.FileDesc desc) {
        if (toString().endsWith("png"))
            return false;
        for (String str : ANIMFL)
            if (desc.path.endsWith(str))
                return false;
        return true;
    }

    @Override
    public void printErr(ErrType t, String str) {
        (t == ErrType.INFO ? System.out : System.err).println(str);
    }

    @Override
    public void loadProg(String str) {

    }

    @Override
    public boolean restore(Backup b, Consumer<Double> prog) {
        return false;
    }

}
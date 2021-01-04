package mandarin.packpack.supporter;

import common.CommonStatic;
import common.io.PackLoader;
import common.io.assets.Admin;
import common.pack.Context;
import common.util.Data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class PackContext implements Context {

    @Override
    public boolean confirmDelete() {
        System.out.println("skip delete confirmation");
        return true;
    }

    @Override
    public File getAssetFile(String string) {
        return new File("./assets/" + string);
    }

    @Override
    public File getAuxFile(String string) {
        return new File(string);
    }

    @Override
    public InputStream getLangFile(String file) {
        return Data.err(() -> new FileInputStream("./assets/lang/en/" + file));
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
    public void initProfile() {
    }

    @Override
    public void noticeErr(Exception e, ErrType t, String str) {
        printErr(t, str);
        e.printStackTrace(t == ErrType.INFO ? System.out : System.err);
    }

    @Override
    public boolean preload(PackLoader.ZipDesc.FileDesc desc) {
        return Admin.preload(desc);
    }

    @Override
    public void printErr(ErrType t, String str) {
        (t == ErrType.INFO ? System.out : System.err).println(str);
    }

}
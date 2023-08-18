package mandarin.packpack.supporter.bc;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TasteApk {
    public static final List<String> VECTOR = new ArrayList<>();
    public static final List<String> KEY = new ArrayList<>();
    public static final byte[] LIST = getList();

    public static boolean isValidApk(File apk) throws Exception {
        ZipFile zip = new ZipFile(apk);

        boolean hasAsset = false;
        boolean hasPack = false;
        boolean hasList = false;

        Enumeration<? extends ZipEntry> entries = zip.entries();

        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if(entry.getName().contains("assets")) {
                hasAsset = true;

                if(entry.getName().contains(".pack") && !entry.isDirectory())
                    hasPack = true;
                else if(entry.getName().contains(".list") && !entry.isDirectory())
                    hasList = true;
            }
        }

        zip.close();

        return hasAsset && hasPack && hasList;
    }

    public static String tasteApk(File apk, MessageChannel ch, int loc) throws Exception {
        File workspace = new File(apk.getParent(), "workspace");

        if(!workspace.exists() && !workspace.mkdirs()) {
            ch.sendMessage("Failed to create workspace folder, aborted").queue();
            StaticStore.logger.uploadLog("Failed to create folder : "+workspace.getAbsolutePath());

            return "";
        }

        ch.sendMessage("Extracting files from apk...").queue();

        ZipFile zip = new ZipFile(apk);

        Enumeration<? extends ZipEntry> entries = zip.entries();

        StringBuilder result = new StringBuilder("----- EXTRACT -----\n\n");

        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if(!entry.isDirectory() && (entry.getName().endsWith(".pack") || entry.getName().endsWith(".list"))) {
                String realName = extractName(entry.getName());

                result.append("Extracting ").append(realName).append("...\n");

                InputStream ins = zip.getInputStream(entry);

                File target = new File(workspace, realName);

                if(!target.exists() && !target.createNewFile()) {
                    result.append("Failed to create file : ").append(target.getAbsolutePath()).append("\n\n------ ABORTED -----");

                    return result.toString();
                }

                FileOutputStream fos = new FileOutputStream(target);

                byte[] buffer = new byte[65535];
                int len;

                while((len = ins.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                ins.close();
                fos.close();
            }
        }

        List<String> assets = getAssetList(workspace);

        ch.sendMessage("Successfully extracted files, tasting files...").queue();

        result.append("\n----- TASTING -----\n\n");

        for(int i = 0; i < assets.size(); i++) {
            String asset = assets.get(i);

            result.append("Decrypting ").append(asset).append("...\n\n");

            String res = tasteAsset(workspace, asset, loc, result) ? "Successful" : "Failed";

            result.append("\nDecryption ").append(res);

            if(i < assets.size() - 1) {
                result.append("\n\n----- ---------- -----\n\n");
            }
        }

        result.append("\n\n----- SUCCESSFUL -----");

        ch.sendMessage("Tasting file done, cleaning up files...").queue();

        zip.close();

        StaticStore.executorHandler.postDelayed(5000, () -> {
            System.gc();

            File temp = new File(workspace, "temp");

            StaticStore.deleteFile(temp, true);

            for(String asset : assets) {
                StaticStore.deleteFile(new File(workspace, asset+".list"), true);
                StaticStore.deleteFile(new File(workspace, asset+".pack"), true);
            }

            StaticStore.deleteFile(apk, true);
        });

        return result.toString();
    }

    private static List<String> getAssetList(File workspace) {
        List<String> assets = new ArrayList<>();

        File[] files = workspace.listFiles();

        if(files == null)
            return assets;

        Map<String, boolean[]> validation = new HashMap<>();

        for(File f : files) {
            if(f.getName().endsWith(".pack") || f.getName().endsWith(".list")) {
                String name = f.getName().replaceAll("\\.(pack|list)$", "");

                if (validation.containsKey(name)) {
                    boolean[] validates = validation.get(name);

                    if(f.getName().contains(".pack"))
                        validates[0] = true;
                    else
                        validates[1] = true;
                } else {
                    boolean[] validates = new boolean[2];

                    if(f.getName().contains(".pack"))
                        validates[0] = true;
                    else
                        validates[1] = true;

                    validation.put(name, validates);
                }
            }
        }

        for(String key : validation.keySet()) {
            boolean[] validates = validation.get(key);

            if(validates[0] && validates[1])
                assets.add(key);
        }

        assets.sort(String::compareTo);

        return assets;
    }

    private static boolean tasteAsset(File workspace, String asset, int loc, StringBuilder builder) throws Exception {
        File destination = new File(workspace, asset);

        if(!destination.exists() && !destination.mkdirs()) {
            StaticStore.logger.uploadLog("Failed to create folder : "+destination.getAbsolutePath());

            return false;
        }

        File listFile = new File(workspace, asset+".list");
        File packFile = new File(workspace, asset+".pack");

        if(!listFile.exists() || !packFile.exists())
            return false;

        FileInputStream fis = new FileInputStream(listFile);

        File generated = generateList(workspace, fis, asset);

        if(generated != null) {
            RandomAccessFile packAccess = new RandomAccessFile(packFile, "r");

            BufferedReader reader = new BufferedReader(new FileReader(generated, StandardCharsets.UTF_8));

            int num = Integer.parseInt(reader.readLine());

            for(int m = 0; m < num; m++) {
                String[] data = reader.readLine().split(",");

                if(data.length < 3)
                    continue;

                String name = data[0];
                int offset = Integer.parseInt(data[1]);
                int size = Integer.parseInt(data[2]);

                builder.append("Name : ").append(name).append("\tOffset : ").append(offset).append("\tSize : ").append(size).append("\n");

                File f = new File(destination, name);

                if(!f.exists() && !f.createNewFile()) {
                    StaticStore.logger.uploadLog("Failed to create file : "+f.getAbsolutePath());

                    reader.close();
                    packAccess.close();

                    return false;
                }

                if(asset.startsWith("ImageDataLocal")) {
                    packAccess.seek(offset);

                    byte[] input = new byte[size];
                    int len = packAccess.read(input);

                    FileOutputStream fos = new FileOutputStream(f);

                    fos.write(input, 0, len);

                    fos.close();
                } else {
                    Cipher packCipher = getPackCipher(loc);

                    byte[] input = new byte[size];
                    int len = packAccess.read(input);

                    FileOutputStream fos = new FileOutputStream(f);

                    byte[] output = packCipher.update(input, 0, len);

                    if(output != null)
                        fos.write(output);

                    byte[] fin = packCipher.doFinal();

                    if(fin != null)
                        fos.write(fin);

                    fos.close();
                }
            }

            reader.close();
            packAccess.close();

            return true;
        } else {
            return false;
        }
    }

    private static File generateList(File workspace, FileInputStream fis, String name) throws Exception {
        Cipher list = getListCipher();

        File temp = new File(workspace, "temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Failed to create folder : "+temp.getAbsolutePath());

            return null;
        }

        File l = new File(temp, "list_d_"+name+".txt");

        if(!l.exists() && !l.createNewFile()) {
            StaticStore.logger.uploadLog("Failed to create file : "+l.getAbsolutePath());

            return null;
        }

        byte[] input = new byte[65536];
        int i;

        FileOutputStream fos = new FileOutputStream(l);

        while((i = fis.read(input)) != -1) {
            byte[] output = list.update(input, 0, i);

            if(output != null)
                fos.write(output);
        }

        byte[] output = list.doFinal();

        fos.write(output);

        fos.close();

        return l;
    }

    private static Cipher getListCipher() throws Exception {
        SecretKeySpec spec = new SecretKeySpec(LIST, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

        cipher.init(Cipher.DECRYPT_MODE, spec);

        return cipher;
    }

    private static Cipher getPackCipher(int loc) throws Exception {
        SecretKeySpec spec = new SecretKeySpec(stringToByte(KEY.get(loc)), "AES");
        IvParameterSpec iv = new IvParameterSpec(stringToByte(VECTOR.get(loc)));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

        cipher.init(Cipher.DECRYPT_MODE, spec, iv);

        return cipher;
    }

    private static byte[] getList() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            byte[] result = md5.digest("pack".getBytes(StandardCharsets.UTF_8));

            String realResult = hexToString(result).substring(0, 16);

            return realResult.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static String hexToString(byte[] arr) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(0xFF & ((int) arr[i]));

            if(h.length() == 1)
                h = "0" + h;

            builder.append(h);
        }

        return builder.toString();
    }

    private static byte[] stringToByte(String s) {
        int len = s.length();

        byte[] result = new byte[len / 2];

        int i = 0;

        while(i < len) {
            result[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));

            i += 2;
        }

        return result;
    }

    private static String extractName(String entry) {
        String[] contents = entry.split("/");

        return contents[contents.length - 1];
    }
}

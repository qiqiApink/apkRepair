package Utils;

import com.android.apksig.ApkSigner;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ApkUtil {

    public static void unZipApk(File apk, String outDir) throws IOException {
        new File(outDir).mkdirs();

        ZipFile zipApk = new ZipFile(apk);
        for (Enumeration entries = zipApk.entries(); entries.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                new File(outDir, entry.getName()).mkdirs();
            } else {
                int len = 0;
                byte[] buffer = new byte[1024];
                File tmp = new File(outDir, entry.getName());
                tmp.getParentFile().mkdirs();
                InputStream iStream = zipApk.getInputStream(entry);
                OutputStream oStream = new FileOutputStream(tmp);
                while ((len = iStream.read(buffer)) != -1) {
                    oStream.write(buffer, 0, len);
                }
                iStream.close();
                oStream.close();
            }
        }
        zipApk.close();
    }

    public static void zipApk(File dir, String outApk) throws IOException {

        Map<String, List<String>> fileNamesAndDirNames = gatherFilesAndDirs(dir);
        List<String> fileNames = fileNamesAndDirNames.get("fileNames");
        List<String> directoryNames = fileNamesAndDirNames.get("directoryNames");

        File resultApk = new File(outApk);
        resultApk.createNewFile();
        ZipOutputStream zipOStrem = new ZipOutputStream(new FileOutputStream(outApk));

        for (String fileName: fileNames) {
            File file = new File(dir, fileName);
            FileInputStream iStream = new FileInputStream(file);
            ZipEntry entry = new ZipEntry(fileName);
            zipOStrem.putNextEntry(entry);

            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = iStream.read(buffer)) != -1) {
                zipOStrem.write(buffer, 0, len);
            }
            iStream.close();
        }

        for (String dirName: directoryNames) {
            ZipEntry entry = new ZipEntry(dirName + "/");
            zipOStrem.putNextEntry(entry);
        }

        zipOStrem.close();
    }

    public static void signApk(File apk) throws Exception {
        String keyStore = apk.getParent() == null ? ".keystore" : apk.getParent() + File.separator + ".keystore";
        String keyAlias = "cert";

        generateKeyPair(keyStore, keyAlias);

        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(keyStore), "changeIt".toCharArray());

            PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, "changeIt".toCharArray());
            Certificate[] certificateChain = ks.getCertificateChain(keyAlias);
            List<X509Certificate> certs = new ArrayList<>(1);
            certs.add((X509Certificate) certificateChain[0]);

            ApkSigner.SignerConfig signerConfig = (new ApkSigner.SignerConfig.Builder(keyAlias, privateKey, certs)).build();
            List<ApkSigner.SignerConfig> signerConfigs = new ArrayList<>(1);
            signerConfigs.add(signerConfig);

            File tmpOutputApk = File.createTempFile("apksigner", ".apk");
            tmpOutputApk.deleteOnExit();

            ApkSigner.Builder builder = new ApkSigner.Builder(signerConfigs);
            builder = builder.setInputApk(apk).setOutputApk(tmpOutputApk);

            ApkSigner apkSigner = builder.build();
            apkSigner.sign();

            Files.move(tmpOutputApk.toPath(), apk.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception();
        }
    }

    public static void clearSignature(File metaInf) {
        String manifestFile = "MANIFEST.MF";
        String keyFile = "";
        String sfFile = "";

        String[] fileNames = metaInf.list();
        for (String fileName: fileNames) {
            if (fileName.endsWith(".RSA") || fileName.endsWith(".DSA") || fileName.endsWith(".EC")) {
                keyFile = fileName;
            } else if (fileName.endsWith(".SF")) {
                sfFile = fileName;
            }
        }

        (new File(metaInf, manifestFile)).delete();
        (new File(metaInf, keyFile)).delete();
        (new File(metaInf, sfFile)).delete();
    }

    public static void generateKeyPair(String keyStore, String alias) {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            FileInputStream iStream = null;
            char[] password = null;
            if ((new File(keyStore)).exists()) {
                iStream = new FileInputStream(keyStore);
                password = "changeIt".toCharArray();
            }
            else {
                new File(keyStore).createNewFile();
            }
            ks.load(iStream, password);

            if (iStream != null)
                iStream.close();

            if (!ks.isKeyEntry(alias)) {
                CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA256WithRSA");
                keypair.generate(1024);
                PrivateKey privateKey = keypair.getPrivateKey();

                X509Certificate[] chain = new X509Certificate[1];
                chain[0] = keypair.getSelfCertificate(
                        new X500Name("Unknown", "Unknown", "Unknown", "Unknown"), new Date(), 365L * 24L * 60L * 60L);

                ks.setKeyEntry(alias, privateKey, "changeIt".toCharArray(), chain);
            }

            ByteArrayOutputStream bOStream = new ByteArrayOutputStream();
            ks.store(bOStream, "changeIt".toCharArray());
            try (FileOutputStream fOStream = new FileOutputStream(keyStore)) {
                fOStream.write(bOStream.toByteArray());
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<String>> gatherFilesAndDirs(String dir) {
        return gatherFilesAndDirs(new File(dir));
    }

    public static Map<String, List<String>> gatherFilesAndDirs(File dir) {
        Map<String, List<String>> result = new HashMap<>();

        List<String> fileNames = new ArrayList<>();
        List<String> directoryNames = new ArrayList<>();
        List<String> tmpDirNames = new ArrayList<>();
        for (File file: dir.listFiles()) {
            String name = file.getName();
            if (file.isDirectory()) {
                directoryNames.add(name);
                tmpDirNames.add(name);
            }
            else
                fileNames.add(file.getName());
        }
        while (!tmpDirNames.isEmpty()) {
            String tmpDirName = tmpDirNames.get(0);
            File tmpDir = new File(dir, tmpDirName);
            for (File file: tmpDir.listFiles()) {
                String name = tmpDirName + "/" + file.getName();
                if (file.isDirectory()) {
                    directoryNames.add(name);
                    tmpDirNames.add(name);
                }
                else
                    fileNames.add(name);
            }
            tmpDirNames.remove(0);
        }

        result.put("fileNames", fileNames);
        result.put("directoryNames", directoryNames);
        return result;
    }
}

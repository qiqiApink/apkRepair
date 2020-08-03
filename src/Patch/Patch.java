package Patch;

import Locate.ClassLocate;
import Utils.ApkUtil;
import Utils.WalaUtil;
import com.android.tools.r8.code.*;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import de.infsec.tpl.config.LibScoutConfig;
import de.infsec.tpl.hashtree.node.ClassNode;
import de.infsec.tpl.hashtree.node.Node;
import de.infsec.tpl.modules.libmatch.LibraryIdentifier;
import de.infsec.tpl.profile.LibProfile;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.stats.AppStats;
import de.infsec.tpl.utils.Utils;
import de.infsec.tpl.utils.WalaUtils;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.jf.util.ExceptionWithContext;
import sun.tools.asm.Assembler;

import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;

import static Utils.ApkUtil.*;
import static Utils.DexUtil.findDexWithClass;
import static DeCompile.DexToSmali.disassemble;
import static Compile.SmaliToDex.assemble;

class compare implements Comparator<Object> {
    public int compare(Object _a, Object _b) {
        String a = (String)_a;
        String b = (String)_b;
        String nameA = a.split("_")[0] + "::" + a.split("_")[2];
        String nameB = b.split("_")[0] + "::" + b.split("_")[2];
        if(!nameA.equals(nameB))
            return nameA.compareTo(nameB);
        String[] versionA = a.split("_")[3].split("\\.");
        String[] versionB = b.split("_")[3].split("\\.");
        for (int i = 0; i < versionA.length && i < versionB.length; i++) {
            int va = Integer.valueOf(versionA[i]);
            int vb = Integer.valueOf(versionB[i]);
            if (va != vb)
                return va - vb;
        }
        return versionA.length - versionB.length;
    }
}

public class Patch {
    public Map<String, File> patches;
    public AppStats stats;
    public Map<ProfileMatch, File> patchesToRepair;
    public Map<String, Node> classNameMap; // <NameInLib, NameInApk>

    public Patch() {
        //To Do
        patchesToRepair = new HashMap<>();
        classNameMap = new HashMap<>();
        loadPatches();
    }

    public void loadPatches() {
        patches = new TreeMap<String, File>(new compare());
        try {
            File dir = new File("patch");
            File[] fs = dir.listFiles();
            if (fs != null) {
                for (File dexFile: fs) {
                    patches.put(dexFile.getName().substring(0, dexFile.getName().lastIndexOf(".")),
                            new File("patch" + File.separator + dexFile.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(File inputApk, List<LibProfile> profiles) {
        init();
        scanVulnerabilities(inputApk, profiles);
        locateVulnerabilities();
        filterIncompatiblePatches();
        doPatch(inputApk);
    }

    public void scanVulnerabilities(File inputApk, List<LibProfile> profiles) {
        try {
            LibScoutConfig.loadConfig();
            stats = LibraryIdentifier.run(inputApk, profiles, true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void locateVulnerabilities() {
        Map<String, ProfileMatch> pMatches = new HashMap<>();
        for (ProfileMatch pMatch: stats.pMatches) {
            String libName = pMatch.lib.description.name;
            if (pMatch.isMatch())
                pMatches.put(libName, pMatch);
            else if (pMatch.isPartialMatch() && (!pMatches.containsKey(libName) ||
                    pMatches.get(libName).getHighestSimScore().simScore < pMatch.getHighestSimScore().simScore))
                pMatches.put(libName, pMatch);
        }
        for (ProfileMatch pMatch: pMatches.values()) {
            if (!pMatch.isMatch() && !pMatch.isPartialMatch()) {
                continue;
            }

            String libName = pMatch.lib.description.name;
            System.out.println(libName + "@" + pMatch.lib.description.version);
            String target = "";

            String version = pMatch.lib.description.version;
            String[] vs = version.split("\\.");

            lableA:
            for (String lib: patches.keySet()) {
                String[] items = lib.split("_");
                String name = items[0] + "::" + items[2];
                if(!name.equals(libName))
                    continue;
                else if (version.equals(items[3])) {
                    target = lib;
                    System.out.println(lib);
                    break lableA;
                }
                else {
                    String[] ver = items[3].split("\\.");
                    for (int i = 0; i < vs.length && i < ver.length; i++) {
                        int v1 = Integer.valueOf(vs[i]);
                        int v2 = Integer.valueOf(ver[i]);
                        if(v1 == v2)
                            continue;
                        else if (v1 < v2) {
                            target = lib;
                            System.out.println(lib);
                            break lableA;
                        }
                        else
                            break;
                    }
                    if (vs.length < ver.length) {
                        target = lib;
                        System.out.println(lib);
                        break lableA;
                    }
                }
            }

            if (target.length() == 0)
                continue;

            File patchFile = patches.get(target);

            boolean ignore = false;
            for (IClass iClass: WalaUtil.resolveDexFile(patchFile)) {
                String clazzName = WalaUtils.simpleName(iClass);
                if (classNameMap.containsKey(clazzName)) {
                    if (classNameMap.get(clazzName) != null)
                        continue;
                    else {
                        ignore = true;
                        break;
                    }
                }

                Node node = ClassLocate.getMatchedClazzByName(clazzName, pMatch);
                classNameMap.put(clazzName, node);
                if (node == null) {
                    ignore = true;
                    break;
                }
            }

            if (!ignore)
                patchesToRepair.put(pMatch, patchFile);
        }
    }

    public void filterIncompatiblePatches() {
        List<ProfileMatch> removeLibs = new ArrayList<>();
        for (ProfileMatch profileMatch: patchesToRepair.keySet()) {
            if(!checkCompatibility(profileMatch, WalaUtil.resolveDexFile(patchesToRepair.get(profileMatch))))
                removeLibs.add(profileMatch);
        }
        for (ProfileMatch profileMatch: removeLibs)
            patchesToRepair.remove(profileMatch);
    }

    private boolean checkCompatibility(ProfileMatch profileMatch, List<IClass> classesToReplace) {
        for (IClass classToReplace: classesToReplace) {
            if (!checkCompatibility(profileMatch, classToReplace))
                return false;
        }
        return true;
    }

    private boolean checkCompatibility(ProfileMatch profileMatch, IClass classToReplace) {
        String clazzName = WalaUtils.simpleName(classToReplace);
        Node node = classNameMap.get(clazzName);
        if (node == null)
            return true;
        IClass classInApk = null;
        try {
            classInApk = WalaUtils.lookupClass(stats.cha, ((ClassNode) node).clazzName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return ClassCompatibility.checkClassCompatibility(profileMatch, classInApk, classToReplace);
    }

    public void init() {
        patchesToRepair.clear();
        classNameMap.clear();
    }

    public void doPatch(File inputApk) {
        String apkName = inputApk.getName();
        String tmpOut = apkName.substring(0, apkName.lastIndexOf(".")) + "_tmpOut" + File.separator;

        try {
            unZipApk(inputApk, tmpOut);

            List<String> assembleDirs = new ArrayList<>();

            //DeCompile
            for (File patch: patchesToRepair.values()) {
                String tmpReplaceDirName = "tmpReplaceDir";
                disassemble(patch, tmpOut + tmpReplaceDirName);
                List<String> smaliPatches = ApkUtil.gatherFilesAndDirs(tmpOut + tmpReplaceDirName).get("fileNames");

                for (String smaliName: smaliPatches) {
                    String clazzName = smaliName.substring(0, smaliName.lastIndexOf(".")).replace("/", ".");
                    Node node = classNameMap.get(clazzName);
                    String clazzNameInApk = (((ClassNode) node).clazzName).replace(".", "/");
                    File dex = findDexWithClass(new File(tmpOut), clazzNameInApk);
                    String tmpDirName = dex.getName().substring(0, dex.getName().lastIndexOf(".")) + "_tmpDir";
                    if (!(new File(tmpDirName)).exists()) {
                        disassemble(dex, tmpOut + tmpDirName);
                        assembleDirs.add(tmpOut + tmpDirName);
                    }

                    //In dexName
                    File inSmali = new File(tmpOut + tmpReplaceDirName, smaliName);
                    InputStream iStream = new FileInputStream(inSmali);

                    //Out fileName
                    File outSmali = new File(tmpOut + tmpDirName, clazzNameInApk + ".smali");
                    OutputStream oStream = new FileOutputStream(outSmali);

                    System.out.println(inSmali.getName());
                    System.out.println(outSmali.getName());

                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = iStream.read(buffer)) != -1) {
                        oStream.write(buffer, 0, len);
                    }
                    iStream.close();
                    oStream.close();
                }

                FileUtils.deleteDirectory(new File(tmpOut, tmpReplaceDirName));
            }

            //Compile
            boolean flag = false;
            for (String assembleDir: assembleDirs) {
                String dexName = assembleDir.split("_")[0] + ".dex";
                assert new File(assembleDir).exists();
                //delete origin dex
                (new File(tmpOut + dexName)).delete();
                try {
                    assemble(assembleDir, tmpOut + dexName);
                } catch (ExceptionWithContext e) {
                    System.out.println("Assemble Failed!");
                }
                FileUtils.deleteDirectory(new File(tmpOut + assembleDir));
                if (flag)
                    System.exit(0);
            }

            //Repack apk

            String target = "patchedAPK";
            File dir = new File(target);
            if(!dir.exists()) {
                try {
                    dir.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            apkName = target + File.separator + apkName;

            zipApk(new File(tmpOut), apkName);
            FileUtils.deleteDirectory(new File(tmpOut));
            signApk(new File(apkName));

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        List<LibProfile> profiles = new ArrayList<>();
        try {
            File dir = new File("profile");
            File[] fs = dir.listFiles();
            if (fs != null)
                for (File f: fs)
                    profiles.add((LibProfile) Utils.disk2Object(new File("profile" + File.separator + f.getName())));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        /*File time = new File("time.txt");
        try {
            time.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //OutputStream oStream = new FileOutputStream(time, true);

        Patch patch = new Patch();
        List<File> vulApks = new ArrayList<>();
        //vulApks.add(new File("apk/com.verizon.messaging.vzmsgs.apk"));
        //vulApks.add(new File("apk/com.mi.android.globalFileexplorer.apk"));
        vulApks.add(new File(args[0]));
        for (File inputApk: vulApks) {
            //long startTime = System.currentTimeMillis();
            patch.run(inputApk, profiles);
            //long endTime = System.currentTimeMillis();
            //oStream.write((String.valueOf(endTime - startTime) + "\n").getBytes());
        }

    }
}

package DeCompile;

import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class DexToSmali {

    public static void disassemble(File dex, String outDir) throws IOException {
        File out = new File(outDir);
        if (!out.exists() && !out.mkdirs()) {
            System.err.println("Can't create the output directory " + out);
            System.exit(-1);
        }

        BaksmaliOptions options = new BaksmaliOptions();
        DexBackedDexFile dexFile = new DexBackedDexFile(null, Files.readAllBytes(dex.toPath()));

        options.deodex = false;
        options.implicitReferences = false;
        options.parameterRegisters = true;
        options.localsDirective = true;
        options.sequentialLabels = true;
        options.debugInfo = true;
        options.codeOffsets = false;
        options.accessorComments = false;
        options.registerInfo = 0;
        options.inlineResolver = null;

        int jobs = Runtime.getRuntime().availableProcessors();
        if (jobs > 6) {
            jobs = 6;
        }

        Baksmali.disassembleDexFile(dexFile, out, jobs, options);
    }

    public static String clean(String outDir, String originClazzName) throws IOException {
        // originClazzName sample: twitter4j.util.CharacterUtil

        outDir = outDir + "/";
        String[] path = originClazzName.split("\\.");
        String smaliPath = outDir + String.join(File.separator, Arrays.asList(path).subList(0, path.length - 1)) + File.separator;
        String smaliFileName = path[path.length - 1] + ".smali";

        File originSmaliFile = new File(smaliPath + smaliFileName);
        File newSmaliFile = new File(outDir + smaliFileName);
        newSmaliFile.createNewFile();
        originSmaliFile.renameTo(newSmaliFile);

        String dirToDelete = smaliPath;
        for (int i = path.length - 2; i >= 0; i--) {
            File tmp = new File(dirToDelete);
            tmp.delete();
            dirToDelete = dirToDelete.substring(0, dirToDelete.length() - path[i].length() - 2);
        }

        return smaliFileName;
    }
}

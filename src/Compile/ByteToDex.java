package Compile;

import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ByteToDex {
    public static void compileDex(List<File> classFiles, String outDir) throws CompilationFailedException {
        List<Path> classPaths = new ArrayList<>();
        for (File classFile: classFiles)
            classPaths.add(classFile.toPath());

        D8Command.Builder builder = D8Command.builder().addProgramFiles(classPaths).setOutput(new File(outDir).toPath(), OutputMode.DexIndexed);
        D8.run(builder.build());
    }
}

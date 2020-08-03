package Compile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;

public class SourceToBytecode {
    private static JavaCompiler compiler;

    public static byte[] generateBytecode(File sourceFile) {
        compiler = ToolProvider.getSystemJavaCompiler();
        return null;
    }
}

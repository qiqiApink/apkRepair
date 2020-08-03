package Compile;

import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.jf.util.ExceptionWithContext;

import java.io.IOException;

public class SmaliToDex {

    public static void assemble(String dir, String out) throws IOException {
        SmaliOptions options = new SmaliOptions();

        int jobs = Runtime.getRuntime().availableProcessors();
        if (jobs > 6) {
            jobs = 6;
        }

        options.jobs = jobs;
        options.apiLevel = 15;
        options.outputDexFile = out;
        options.allowOdexOpcodes = false;
        options.verboseErrors = false;

        //try {
            Smali.assemble(options, dir);
        //} catch (ExceptionWithContext e) {
            //System.out.println("Repackage Failed!");
        //}
    }
}

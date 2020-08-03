package Utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WalaUtil {
    public static List<IClass> resolveDexFile(File dexFile) {
        List<IClass> result = new ArrayList<>();
        try {
            AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(dexFile.toURI(), null, null, new File("android-sdk/android-28.jar").toURI());
            ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
            for (IClass iClass: cha) {
                if (iClass.getClassLoader().getName().toString().equals("Application")) {
                    result.add(iClass);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean class_equal(IClass class_1, IClass class_2) {
        if (!class_1.equals(class_2))
            return false;
        for (IMethod method_2: class_2.getDeclaredMethods()) {
            IMethod method_1 = class_1.getMethod(method_2.getSelector());
            if (method_1 == null)
                return false;
            if (!method_equal(method_1, method_2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean method_equal(IMethod method_1, IMethod method_2) {
        assert method_1 instanceof DexIMethod;
        assert method_2 instanceof DexIMethod;

        Instruction[] instructions_1 = ((DexIMethod) method_1).getDexInstructions();
        Instruction[] instructions_2 = ((DexIMethod) method_2).getDexInstructions();

        if (instructions_1.length != instructions_2.length) {
            return false;
        }
        for (int i = 0; i < instructions_1.length; i++) {
            Instruction instruction_1 = instructions_1[i];
            Instruction instruction_2 = instructions_2[i];
            if (!instruction_1.getOpcode().name.equals(instruction_2.getOpcode().name)) {
                return false;
            }
            if (instruction_1 instanceof Invoke && !compareInvokeInstruction(instruction_1, instruction_2))
                return false;
        }
        return true;
    }

    private static boolean compareInvokeInstruction(Instruction instruction_1, Instruction instruction_2) {
        assert instruction_1 instanceof Invoke;
        assert instruction_2 instanceof Invoke;
        if (!((Invoke) instruction_1).clazzName.equals(((Invoke) instruction_2).clazzName)) {
            return false;
        }
        if (!((Invoke) instruction_1).methodName.equals(((Invoke) instruction_2).methodName)) {
            return false;
        }

        return true;
    }
}

package Utils;

import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction35c;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction3rc;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class BaksmaliUtil {

    public static DexBackedMethod findMethodBySignature(File dex, String signature) {
        DexBackedMethod targetMethod = null;

        try {
            DexBackedDexFile dexFile = new DexBackedDexFile(null, Files.readAllBytes(dex.toPath()));
            Set<? extends DexBackedClassDef> dexClasses = dexFile.getClasses();

            Map<String, String> tmp = resolveSignature(signature);
            String targetClassName = tmp.get("class");
            String targetMethodName = tmp.get("method");
            String targetParams = tmp.get("params");
            String targetReturnType = tmp.get("returnType");

            DexBackedClassDef targetClass = null;
            for (DexBackedClassDef dexClass: dexClasses) {
                String className = dexClass.getType();
                if (className.equals(targetClassName)){
                    targetClass = dexClass;
                    break;
                }
            }

            for (DexBackedMethod dexMethod: targetClass.getMethods()) {
                if (dexMethod.getName().equals(targetMethodName) && checkParamsAndReturn(dexMethod, targetParams, targetReturnType)){
                    targetMethod = dexMethod;
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return targetMethod;
    }

    public static boolean equals(DexBackedMethod method_1, DexBackedMethod method_2) {
        boolean result = true;

        boolean end_1 = false;
        boolean end_2 = false;

        Iterator it_1 = method_1.getImplementation().getInstructions().iterator();
        Iterator it_2 = method_2.getImplementation().getInstructions().iterator();

        while (result && !end_1 && !end_2) {
            Instruction cur_1 = (Instruction) it_1.next();
            Instruction cur_2 = (Instruction) it_2.next();

            result = cur_1.getOpcode().name.equals(cur_2.getOpcode().name);
            if (result && cur_1.getOpcode().referenceType == 3)
                result = compareInvokeInstruction(cur_1, cur_2);

            end_1 = !(it_1.hasNext());
            end_2 = !(it_2.hasNext());
        }

        return result && (method_1.getAccessFlags() == method_2.getAccessFlags());
    }

    public static Map<String, String> resolveSignature(String signature) {
        String[] tmp = signature.split("\\(");
        String[] tmp_2 = tmp[1].split("\\)");

        String returnType = tmp_2[1];
        String params = tmp_2[0];

        List<String> methodList = Arrays.asList(tmp[0].split("\\."));
        String clazz = "L" + String.join("/",
                methodList.subList(0, methodList.size() - 1)) + ";";
        String method = methodList.get(methodList.size() - 1);

        Map<String, String> result = new HashMap<>();
        result.put("class", clazz);
        result.put("method", method);
        result.put("params", params);
        result.put("returnType", returnType);

        return result;
    }

    public static boolean checkParamsAndReturn(DexBackedMethod method, String targetParams, String targetReturnType) {
        List<String> params = method.getParameterTypes();
        String returnType = method.getReturnType();
        return targetParams.equals(String.join("", params)) && targetReturnType.equals(returnType);
    }

    private static boolean compareInvokeInstruction(Instruction ins_1, Instruction ins_2) {
        DexBackedMethodReference ref_1;
        DexBackedMethodReference ref_2;
        if (ins_1 instanceof DexBackedInstruction35c) {
            ref_1 = (DexBackedMethodReference) ((DexBackedInstruction35c) ins_1).getReference();
            ref_2 = (DexBackedMethodReference) ((DexBackedInstruction35c) ins_2).getReference();
        } else {
            ref_1 = (DexBackedMethodReference) ((DexBackedInstruction3rc) ins_1).getReference();
            ref_2 = (DexBackedMethodReference) ((DexBackedInstruction3rc) ins_2).getReference();
        }

        String callMethod_1 = ref_1.getName();
        String callMethod_2 = ref_2.getName();
        String callClass_1 = ref_1.getDefiningClass();
        String callClass_2 = ref_2.getDefiningClass();

        return callMethod_1.equals(callMethod_2) && callClass_1.equals(callClass_2);
    }
}

package Patch;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.utils.WalaUtils;

import java.io.File;

public class ClassCompatibility {
    /*public static boolean findCallInApk(File apk, IMethod method, String excludedPackageName) {
        //TO DO Get IClassHierarchy
        IClassHierarchy cha = null;
        return findCallInApk(cha, method, excludedPackageName);
    }*/

    /*public static boolean findCallInApk(IClassHierarchy cha, IMethod method, String excludedPackageName) {
        for (IClass iClass: cha) {
            if (!WalaUtils.isAppClass(iClass))
                continue;
            for (IMethod iMethod: iClass.getDeclaredMethods()) {
                if (WalaUtils.simpleName(iMethod.getDeclaringClass()).startsWith(excludedPackageName))
                    continue;
                if (iMethod.isAbstract() || iMethod.isNative())
                    continue;
                try {
                    for (CallSiteReference callSiteReference: CodeScanner.getCallSites(iMethod)) {
                        MethodReference callMethodReference = callSiteReference.getDeclaredTarget();
                        if (callMethodReference.equals(method.getReference()))
                            return true;
                    }
                } catch (InvalidClassFileException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }*/

    public static boolean findCallInApk(ProfileMatch profileMatch, IMethod method) {
        for (String methodSig: profileMatch.usedLibMethods) {
            if (method.getSignature().equals(methodSig))
                return true;
        }
        return false;
    }

    /*public static boolean checkClassCompatibility(File apkFile, File dexFile) {
        //TO DO Get ClassHierarchy
        IClassHierarchy cha = null;

        //TO DO Get IClass
        IClass classInApk = null;

        //TO DO Get IClass
        IClass classToReplace = null;

        //TO DO Get Package Name
        String packageName = null;

        return checkClassCompatibility(cha, classInApk, classToReplace, packageName);
    }*/

    public static boolean checkClassCompatibility(ProfileMatch profileMatch, IClass classInApk, IClass classToReplace) {
        ClassDiff classDiff = new ClassDiff(classInApk, classToReplace);
        for (IMethod method: classDiff.removedMethods) {
            if (findCallInApk(profileMatch, method))
                return false;
        }
        return true;
    }

    /*public static boolean checkClassCompatibility(IClassHierarchy cha, IClass classInApk, IClass classToReplace, String excludedPackageName) {
        ClassDiff classDiff = new ClassDiff(classInApk, classToReplace);
        for (IMethod method: classDiff.removedMethods) {
            if (findCallInApk(cha, method, excludedPackageName))
                return false;
        }
        return true;
    }*/
}

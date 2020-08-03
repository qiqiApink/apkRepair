package Utils;


import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import de.infsec.tpl.hashtree.HashTree;
import de.infsec.tpl.hashtree.node.*;
import de.infsec.tpl.pkg.PackageTree;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.profile.ProfileMatch.HTreeMatch;
import de.infsec.tpl.stats.AppStats;
import de.infsec.tpl.utils.WalaUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class LibScoutUtil {

    public static String getClazzName(String methodName) {
        if (methodName == null)
            return null;

        String method = methodName.split("\\(")[0];
        List<String> list = Arrays.asList(method.split("\\."));
        return String.join(".", list.subList(0, list.size() - 1));
    }


    public static Node getLibMethodNode(String methodName, HashTree hashTree) {
        String clazzName = getClazzName(methodName);
        Node libClazzNode = getLibClazzNode(clazzName, hashTree);
        if (libClazzNode == null)
            return null;

        String nodeName = "MNode(" + methodName + ")";
        for (Node method: libClazzNode.childs)
            if (method.toString().equals(nodeName))
                return method;

        return null;
    }


    public static Node getLibClazzNode(String clazzName, HashTree hashTree) {
        if (hashTree == null)
            return null;

        String nodeName = "CNode(" + clazzName + ")";
        List<Node> allClazz = gatherClazz(hashTree);

        for (Node clazz: allClazz)
            if (clazz.toString().equals(nodeName))
                return clazz;

        return null;
    }


    public static Node getApkClazzNode(Node node, HTreeMatch hMatch) {
        if (node == null || hMatch == null)
            return null;


        List<Node> clazzCandidate = gatherClazz(hMatch.matchingNodes);

        for (Node n: clazzCandidate)
            if (n.equals(node))
                return n;  // TODO multiple nodes with same hash?

        return null;
    }


    public static Node getApkMethodNode(Node node, HTreeMatch hMatch) {
        if (node == null || hMatch == null)
            return null;

        List<Node> methodCandidate = hMatch.matchingNodes.stream()
                .map(PackageNode::getMethodNodes)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (Node n: methodCandidate)
            if (n.toString().equals(node.toString()))   // To Do now only match by method name
                return n;

        return null;
    }

    public static List<Node> gatherClazz(HashTree hashTree) {
        return gatherClazz(hashTree.getPackageNodes());
    }

    public static List<Node> gatherClazz(Collection<PackageNode> packageNodes) {
        return packageNodes.stream()
                .map(PackageNode::getClassNodes)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static boolean isCalledInApk(IClassHierarchy cha, IMethod method, String excludedPackageName) {
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
    }

    public static boolean isCompatible(IClassHierarchy cha, IMethod method, ProfileMatch pMatch) {
        String excludedPackageName = pMatch.getMatchedPackageTree().getRootPackage();
        return !isCalledInApk(cha, method, excludedPackageName);
    }

    public static List isCompatible(File inputApk, List<ProfileMatch> pMatches) {
        //TODO Get IClassHierarchy
        IClassHierarchy cha;
        for (ProfileMatch match: pMatches) {

        }
        return pMatches;
    }
}
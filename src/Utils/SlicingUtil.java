package Utils;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;
import org.jf.dexlib2.dexbacked.DexBackedMethod;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SlicingUtil {

    public static void slice(File dexFile, DexBackedMethod method) {
        try {
            URI androidURI = new File("android-sdk", "android-28.jar").toURI();
            AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(dexFile.toURI(), null, null, androidURI);
            ClassHierarchy cha = ClassHierarchyFactory.make(scope);

            Set<LocatorFlags> flags = HashSetFactory.make();
            flags.add(LocatorFlags.INCLUDE_CALLBACKS);
            flags.add(LocatorFlags.EP_HEURISTIC);
            flags.add(LocatorFlags.CB_HEURISTIC);
            AndroidEntryPointLocator entryPointLocator = new AndroidEntryPointLocator(flags);
            List<? extends Entrypoint> entryPoints = entryPointLocator.getEntryPoints(cha);
            assert !entryPoints.isEmpty();

            AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
            SSAPropagationCallGraphBuilder callGraphBuilder = Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(new DexIRFactory()), cha, scope);
            CallGraph callGraph = callGraphBuilder.makeCallGraph(options);
            //PointerAnalysis pointerAnalysis = callGraphBuilder.getPointerAnalysis();

        } catch (IOException | ClassHierarchyException | CancelException e) {
            e.printStackTrace();
        }

    }

    private static CGNode findMethod(CallGraph callGraph, DexBackedMethod method) {
        Iterator<? extends CGNode> it = callGraph.getSuccNodes(callGraph.getFakeRootNode());
        while (it.hasNext()) {
            CGNode cgNode = it.next();
            IMethod iMethod = cgNode.getMethod();
            Atom methodName = Atom.findOrCreateUnicodeAtom(method.getName());

            Descriptor methodDescriptor = Descriptor.findOrCreateUTF8("");
            if (iMethod.getName().equals(methodName) && iMethod.getDescriptor().equals(methodDescriptor))
                return cgNode;
        }
        return null;
    }
}

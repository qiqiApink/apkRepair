package Locate;

import Utils.LibScoutUtil;

import de.infsec.tpl.hashtree.HashTree;
import de.infsec.tpl.hashtree.node.Node;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.profile.ProfileMatch.HTreeMatch;

public class MethodLocate {

    public static Node getMatchedMethodByName(String methodName, ProfileMatch pMatch) {
        HTreeMatch hMatch = pMatch.getHighestSimScore();
        HashTree libHTree = pMatch.lib.hashTrees.get(0);

        Node libMethodNode = LibScoutUtil.getLibMethodNode(methodName, libHTree);
        Node matchedApkNode = LibScoutUtil.getApkMethodNode(libMethodNode, hMatch);

        return matchedApkNode;
    }
}

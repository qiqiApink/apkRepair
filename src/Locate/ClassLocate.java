package Locate;

import Utils.LibScoutUtil;

import de.infsec.tpl.hashtree.HashTree;
import de.infsec.tpl.hashtree.node.Node;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.profile.ProfileMatch.HTreeMatch;

public class ClassLocate {

    public static Node getMatchedClazzByName(String clazzName, ProfileMatch pMatch) {
        HTreeMatch hMatch = pMatch.getHighestSimScore();
        HashTree libHTree = pMatch.lib.hashTrees.get(0);

        Node libNode = LibScoutUtil.getLibClazzNode(clazzName, libHTree);
        Node matchedApkNode = LibScoutUtil.getApkClazzNode(libNode, hMatch);

        return matchedApkNode;
    }
}
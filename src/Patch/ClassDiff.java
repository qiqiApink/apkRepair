package Patch;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;

import java.util.*;

public class ClassDiff {
    public Set<IField> removedFields;
    public Set<IField> addedFields;
    public Set<IMethod> removedMethods;
    public Set<IMethod> addedMethods;

    public ClassDiff(IClass class_1, IClass class_2) {
        addedMethods = new HashSet<>();
        removedMethods = new HashSet<>();
        addedFields = new HashSet<>();
        removedFields = new HashSet<>();

        Set<IMethod> methods_1 = getPublicMethods(class_1);
        Set<IMethod> methods_2 = getPublicMethods(class_2);
        Set<IField> fields_1 = getPublicFields(class_1);
        Set<IField> fields_2 = getPublicFields(class_2);

        addedMethods.addAll(methods_2);
        addedMethods.removeAll(methods_1);
        removedMethods.addAll(methods_1);
        removedMethods.removeAll(methods_2);

        addedFields.addAll(fields_2);
        addedFields.removeAll(fields_1);
        removedFields.addAll(fields_1);
        removedFields.removeAll(fields_2);
    }

    private static Set<IMethod> getPublicMethods(IClass clazz) {
        Set<IMethod> result = new HashSet<>();
        for (IMethod method: clazz.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic())
                continue;
            if (method.isPublic())
                result.add(method);
        }
        return result;
    }

    private static Set<IField> getPublicFields(IClass clazz) {
        Set<IField> result = new HashSet<>();
        for (IField field: clazz.getDeclaredInstanceFields()) {
            if (field.isPublic())
                result.add(field);
        }
        for (IField field: clazz.getDeclaredStaticFields()) {
            if (field.isPublic())
                result.add(field);
        }
        return result;
    }
}

package Dex;

import java.util.Map;

import static Utils.DexUtil.*;

public class ClassDefItem {
    public ClassItem[] classItems;

    public class ClassItem {
        public String clazz;
        public int accessFlags;
        public String superClass;
        public String sourceFile;
        public String[] interfaces = null;
        public String[] annotations = null;
        public ClassData classData;
    }

    public class ClassData {
        public int staticFieldsSize;
        public int instanceFieldsSize;
        public int directMethodsSize;
        public int virtualMethodsSize;
        public EncodedField[] staticFields = null;
        public EncodedField[] instanceFields = null;
        public EncodedMethod[] directMethods = null;
        public EncodedMethod[] virtualMethods = null;
    }

    public class EncodedMethod {
        public String method;
        public int accessFlags;
        public int registersSize;
        public int insSize;
        public int outsSize;
        public int triesSize;
        public int insnsSize;
        public byte[] insns = null;
    }

    public class EncodedField {
        String field;
        int accessFlags;
    }

    public ClassDefItem(byte[] dexFile, int offset, int size, StringIdItem stringIdItem, TypeIdItem typeIdItem, FieldIdItem fieldIdItem, MethodIdItem methodIdItem) {
        classItems = new ClassItem[size];
        for (int i = 0; i < size; i++) {
            classItems[i] = readClassItem(dexFile, offset + 32 * i, stringIdItem, typeIdItem, fieldIdItem, methodIdItem);
        }
    }

    private ClassItem readClassItem(byte[] dexFile, int offset, StringIdItem stringIdItem, TypeIdItem typeIdItem, FieldIdItem fieldIdItem, MethodIdItem methodIdItem) {
        ClassItem result = new ClassItem();

        int class_idx = readInt(dexFile, offset);
        result.clazz = typeIdItem.findById(class_idx);
        offset += 4;

        result.accessFlags = readInt(dexFile, offset);
        offset += 4;

        int supperclass_idx = readInt(dexFile, offset);
        result.superClass = typeIdItem.findById(supperclass_idx);
        offset += 4;

        int interfaces_off = readInt(dexFile, offset);
        int[] interfaces_id = readIntList(dexFile, interfaces_off);
        if (interfaces_id != null) {
            result.interfaces = new String[interfaces_id.length];
            for (int j = 0; j < interfaces_id.length; j++)
                result.interfaces[j] = typeIdItem.findById(interfaces_id[j]);
        }
        offset += 4;

        int sourcefile_idx = readInt(dexFile, offset);
        result.sourceFile = stringIdItem.findById(sourcefile_idx);
        offset += 4;

        int annotations_off = readInt(dexFile, offset);
        // To Do
        offset += 4;

        int class_data_off = readInt(dexFile, offset);
        result.classData = readClassData(dexFile, class_data_off, fieldIdItem, methodIdItem);
        offset += 4;

        int static_values_off = readInt(dexFile, offset);
        //To Do

        return result;
    }

    private ClassData readClassData(byte[] dexFile, int class_data_off, FieldIdItem fieldIdItem, MethodIdItem methodIdItem) {
        if (class_data_off == 0)
            return null;

        ClassData result = new ClassData();
        int curOffset = class_data_off;

        Map<String, Integer> static_fields_leb128 = readLeb128(dexFile, curOffset);
        result.staticFieldsSize = static_fields_leb128.get("Value");
        result.staticFields = new EncodedField[result.staticFieldsSize];
        curOffset += static_fields_leb128.get("Length");

        Map<String, Integer> instance_fields_leb128 = readLeb128(dexFile, curOffset);
        result.instanceFieldsSize = instance_fields_leb128.get("Value");
        result.instanceFields = new EncodedField[result.instanceFieldsSize];
        curOffset += instance_fields_leb128.get("Length");

        Map<String, Integer> direct_methods_leb128 = readLeb128(dexFile, curOffset);
        result.directMethodsSize = direct_methods_leb128.get("Value");
        result.directMethods = new EncodedMethod[result.directMethodsSize];
        curOffset += direct_methods_leb128.get("Length");

        Map<String, Integer> virtual_methods_leb128 = readLeb128(dexFile, curOffset);
        result.virtualMethodsSize = virtual_methods_leb128.get("Value");
        result.virtualMethods = new EncodedMethod[result.virtualMethodsSize];
        curOffset += virtual_methods_leb128.get("Length");

        curOffset = readFields(dexFile, curOffset, result.staticFields, fieldIdItem);
        curOffset = readFields(dexFile, curOffset, result.instanceFields, fieldIdItem);
        curOffset = readMethods(dexFile, curOffset, result.directMethods, methodIdItem);
        curOffset = readMethods(dexFile, curOffset, result.virtualMethods, methodIdItem);

        return result;
    }

    private int readMethods(byte[] dexFile, int offset, EncodedMethod[] result, MethodIdItem methodIdItem) {
        if (result == null)
            return offset;

        int method_idx = 0;
        for (int i = 0; i < result.length; i++) {
            EncodedMethod tmp = new EncodedMethod();

            Map<String, Integer> method_idx_leb128 = readLeb128(dexFile, offset);
            int method_idx_diff = method_idx_leb128.get("Value");
            method_idx += method_idx_diff;
            tmp.method = methodIdItem.findById(method_idx);
            offset += method_idx_leb128.get("Length");

            Map<String, Integer> access_flags_leb128 = readLeb128(dexFile, offset);
            tmp.accessFlags = access_flags_leb128.get("Value");
            offset += access_flags_leb128.get("Length");

            Map<String, Integer> code_off_leb128 = readLeb128(dexFile, offset);
            int code_off = code_off_leb128.get("Value");
            offset += code_off_leb128.get("Length");

            if (code_off != 0) {
                tmp.registersSize = readShort(dexFile, code_off);
                code_off += 2;

                tmp.insSize = readShort(dexFile, code_off);
                code_off += 2;

                tmp.outsSize = readShort(dexFile, code_off);
                code_off+= 2;

                tmp.triesSize = readShort(dexFile, code_off);
                code_off += 2;

                int debug_info_off = readInt(dexFile, code_off);
                code_off += 4;

                tmp.insnsSize = readInt(dexFile, code_off);
                code_off += 4;

                tmp.insns = readBytes(dexFile, code_off, tmp.insnsSize * 2);
            }

            result[i] = tmp;
        }

        return offset;
    }

    private int readFields(byte[] data, int offset, EncodedField[] result, FieldIdItem fieldIdItem) {
        if (result == null)
            return offset;

        int field_idx = 0;
        for (int i = 0; i < result.length; i++) {
            EncodedField tmp = new EncodedField();

            Map<String, Integer> field_idx_leb128 = readLeb128(data, offset);
            int field_idx_diff = field_idx_leb128.get("Value");
            field_idx += field_idx_diff;
            offset += field_idx_leb128.get("Length");
            tmp.field = fieldIdItem.findById(field_idx);

            Map<String, Integer> access_flags_leb128 = readLeb128(data, offset);
            tmp.accessFlags = access_flags_leb128.get("Value");
            offset += access_flags_leb128.get("Length");
        }

        return offset;
    }
}

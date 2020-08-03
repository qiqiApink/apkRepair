package Dex;

import static Utils.DexUtil.readInt;
import static Utils.DexUtil.readShort;

public class FieldIdItem {
    public FieldItem[] fieldData;

    public class FieldItem {
        String clazz;
        String name;
        String return_type;
    }
    public FieldIdItem(byte[] dex, int offset, int size, StringIdItem stringIdItem, TypeIdItem typeIdItem) {
        fieldData = new FieldItem[size];
        for (int i = 0; i < size; i++) {
            FieldItem tmp = new FieldItem();
            int class_idx = readShort(dex, offset + i * 8);
            tmp.clazz = typeIdItem.findById(class_idx);

            int return_type_idx = readShort(dex, offset + i * 8 + 2);
            tmp.return_type = typeIdItem.findById(return_type_idx);

            int name_idx = readInt(dex, offset + i * 8 + 4);
            tmp.name = stringIdItem.findById(name_idx);

            fieldData[i] = tmp;
        }
    }

    public String findById(int field_id) {
        String result = "";
        FieldItem tmp = fieldData[field_id];
        result += tmp.clazz + "->";
        result += tmp.name + ":";
        result += tmp.return_type;
        return result;
    }
}

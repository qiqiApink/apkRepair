package Dex;

import static Utils.DexUtil.readInt;

public class TypeIdItem {
    public String[] typeData;

    public TypeIdItem(byte[] dexFile, int offset, int size, StringIdItem stringIdItem) {
        typeData = new String[size];
        for (int i = 0; i < size; i++) {
            int stringId = readInt(dexFile, offset + 4 * i);
            typeData[i] = stringIdItem.findById(stringId);
        }
    }

    public String findById(int typeId) {
        return typeData[typeId];
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < typeData.length; i++) {
            result += "type_id_item[" + i + "]: " + typeData[i] + "\n";
        }
        return result;
    }
}

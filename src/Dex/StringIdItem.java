package Dex;

import java.util.Map;

import static Utils.DexUtil.*;

public class StringIdItem {
    public String[] stringData;

    public StringIdItem(byte[] dexFile, int offset, int size) {
        stringData = new String[size];
        for (int i = 0; i < size; i++) {
            int stringDataOff = readInt(dexFile, 4 * i + offset);
            Map<String, Integer> leb128 = readLeb128(dexFile, stringDataOff);
            int utf16_size = leb128.get("Value");
            int stringOff = stringDataOff + leb128.get("Length");
            stringData[i] = readString(dexFile, stringOff, utf16_size);;
        }
    }

    public String findById(int id) {
        if (id == -1)
            return null;
        return stringData[id];
    }

    @Override
    public String toString() {
        String result = "";

        for (int i = 0; i < stringData.length; i++) {
            result += "string_id_item[" + i + "]: \"" + stringData[i] + "\"\n";
        }

        return result;
    }
}

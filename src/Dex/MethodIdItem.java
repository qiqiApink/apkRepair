package Dex;

import static Utils.DexUtil.readInt;
import static Utils.DexUtil.readShort;

public class MethodIdItem {
    public MethodItem[] methodData;

    public class MethodItem {
        public String clazz;
        public String proto;
        public String name;

        public MethodItem(String clazz, String proto, String name) {
            this.clazz = clazz;
            this.name = name;
            this.proto = proto;
        }
    }

    public MethodIdItem(byte[] dexFile, int offset, int size, StringIdItem stringIdItem, TypeIdItem typeIdItem, ProtoIdItem protoIdItem) {
        methodData = new MethodItem[size];
        for (int i = 0; i < size; i++) {
            int clazzId = readShort(dexFile, offset + 8 * i);
            String clazz = typeIdItem.findById(clazzId);

            int protoId = readShort(dexFile, offset + 8 * i + 2);
            String proto = protoIdItem.findById(protoId);

            int nameId = readInt(dexFile, offset + 8 * i + 4);
            String name = stringIdItem.findById(nameId);

            methodData[i] = new MethodItem(clazz, proto, name);
        }
    }

    public String findById(int method_id) {
        String result = "";

        MethodItem tmp = methodData[method_id];
        result += tmp.clazz + "->";
        result += tmp.name;
        result += tmp.proto;

        return result;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < methodData.length; i++) {
            MethodItem tmp = methodData[i];
            result += "method_id_item[" + i + "]\n";
            result += "  class: " + tmp.clazz + "\n";
            result += "  proto: " + tmp.proto + "\n";
            result += "  name: " + tmp.name + "\n";
        }
        return result;
    }
}

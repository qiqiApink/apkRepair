package Dex;

import static Utils.DexUtil.*;

public class ProtoIdItem {
    public ProtoItem[] protoData;

    public class ProtoItem {
        public String shorty;
        public String return_type;
        public String[] parameters = null;

        public ProtoItem(ProtoItem item) {
            shorty = item.shorty;
            return_type = item.return_type;
            if (item.parameters != null)
                parameters = item.parameters.clone();
        }

        public ProtoItem(String shorty, String return_type, String[] parameters) {
            this.shorty = shorty;
            this.return_type = return_type;
            if (parameters != null)
                this.parameters = parameters.clone();
        }
    }
    public ProtoIdItem(byte[] dexFile, int offset, int size, StringIdItem stringIdItem, TypeIdItem typeIdItem) {
        protoData = new ProtoItem[size];
        for (int i = 0; i < size; i++) {
            int shorty_idx = readInt(dexFile, offset + 12 * i);
            String shorty = stringIdItem.findById(shorty_idx);

            int return_type_idx = readInt(dexFile, offset + 12 * i + 4);
            String return_type = typeIdItem.findById(return_type_idx);

            int parameters_off = readInt(dexFile, offset + 12 * i + 8);
            int[] parameters_id = readIntList(dexFile, parameters_off);
            String[] parameters = null;
            if (parameters_id != null) {
                parameters = new String[parameters_id.length];
                for (int j = 0; j < parameters_id.length; j++)
                    parameters[j] = typeIdItem.findById(parameters_id[j]);
            }

            protoData[i] = new ProtoItem(shorty, return_type, parameters);
        }
    }

    public String findById(int protoId) {
        String result = "(";
        ProtoItem tmp = protoData[protoId];
        if (tmp.parameters != null)
            for (String param: tmp.parameters) {
                result += param;
            }
        result += ")" + tmp.return_type;
        return result;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < protoData.length; i++) {
            ProtoItem tmp = protoData[i];
            result += "proto_id_item[" + i + "]: \n";
            result += "  shorty: " + tmp.shorty + "\n";
            result += "  return_type: " + tmp.return_type + "\n";
            result += "  parameters: ";
            if (tmp.parameters != null) {
                for (String param: tmp.parameters) {
                    result += param;
                }
                result += "\n";
            }
            else
                result += "NULL\n";
        }
        return result;
    }
}

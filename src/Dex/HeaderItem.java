package Dex;

import java.util.Arrays;

import static Utils.DexUtil.readBytes;
import static Utils.DexUtil.readInt;

public class HeaderItem {
    public int MAGIC_LENGTH = 8;
    public int SIG_LENGTH = 20;
    public static int HEADER_SIZE = 112;
    public static int ENDIAN_TAG = 0x12345678;

    public byte[] magic = new byte[MAGIC_LENGTH];
    public int checksum;
    public byte[] signature = new byte[SIG_LENGTH];
    public int file_size;
    public int header_size;
    public int endian_tag;
    public int link_size;
    public int link_offset;
    public int map_off;
    public int string_ids_size;
    public int string_ids_off;
    public int type_ids_size;
    public int type_ids_off;
    public int proto_ids_size;
    public int proto_ids_off;
    public int field_ids_size;
    public int field_ids_off;
    public int method_ids_size;
    public int method_ids_off;
    public int class_defs_size;
    public int class_defs_off;
    public int data_size;
    public int data_off;

    public HeaderItem(byte[] dexFile) {
        int cur = 0;

        magic = readBytes(dexFile, cur, MAGIC_LENGTH);
        cur += MAGIC_LENGTH;

        checksum = readInt(dexFile, cur);
        cur += 4;

        signature = readBytes(dexFile, cur, SIG_LENGTH);
        cur += SIG_LENGTH;

        file_size = readInt(dexFile, cur);
        cur += 4;

        header_size = readInt(dexFile, cur);
        assert header_size == HEADER_SIZE;
        cur += 4;

        endian_tag = readInt(dexFile, cur);
        assert endian_tag == ENDIAN_TAG;
        cur += 4;

        link_size = readInt(dexFile, cur);
        cur += 4;

        link_offset = readInt(dexFile, cur);
        cur += 4;

        map_off = readInt(dexFile, cur);
        cur += 4;

        string_ids_size = readInt(dexFile, cur);
        cur += 4;

        string_ids_off = readInt(dexFile, cur);
        cur += 4;

        type_ids_size = readInt(dexFile, cur);
        cur += 4;

        type_ids_off = readInt(dexFile, cur);
        cur += 4;

        proto_ids_size = readInt(dexFile, cur);
        cur += 4;

        proto_ids_off = readInt(dexFile, cur);
        cur += 4;

        field_ids_size = readInt(dexFile, cur);
        cur += 4;

        field_ids_off = readInt(dexFile, cur);
        cur += 4;

        method_ids_size = readInt(dexFile, cur);
        cur += 4;

        method_ids_off = readInt(dexFile, cur);
        cur += 4;

        class_defs_size = readInt(dexFile, cur);
        cur += 4;

        class_defs_off = readInt(dexFile, cur);
        cur += 4;

        data_size = readInt(dexFile, cur);
        cur += 4;

        data_off = readInt(dexFile, cur);
        cur += 4;

        assert cur == HEADER_SIZE;
    }

    @Override
    public String toString() {
        String result = "";
        result += "magic: " + Arrays.toString(magic) + "\n";
        result += "checksum: " + checksum + "\n";
        result += "signature: " + Arrays.toString(signature) + "\n";
        result += "file_size: " + file_size + "\n";
        result += "header_size: " + header_size + "\n";
        result += "endian_tag: "+ endian_tag + "\n";
        result += "link_size: " + link_size + "\n";
        result += "link_offset: " + link_offset + "\n";
        result += "map_off: "+ map_off + "\n";
        result += "string_ids_size: " + string_ids_size + "\n";
        result += "string_ids_off: " + string_ids_off + "\n";
        result += "type_ids_size: " + type_ids_size + "\n";
        result += "type_ids_off: " + type_ids_off + "\n";
        result += "proto_ids_size: " + proto_ids_size + "\n";
        result += "proto_ids_off: " + proto_ids_off + "\n";
        result += "field_ids_size: " + field_ids_size + "\n";
        result += "field_ids_off: " + field_ids_off + "\n";
        result += "method_ids_size: " + method_ids_size + "\n";
        result += "method_ids_off: " + method_ids_off + "\n";
        result += "class_defs_size: " + class_defs_size + "\n";
        result += "class_defs_off: " + class_defs_off + "\n";
        result += "data_size: " + data_size + "\n";
        result += "data_off: " + data_off + "\n";
        return result;
    }
}

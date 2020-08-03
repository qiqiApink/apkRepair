package Utils;

import Dex.*;
import org.jsoup.Connection;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class DexUtil {

    public static byte[] readBytes(byte[] data, int start, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = data[i + start];
        }
        return result;
    }

    public static int readInt(byte[] data, int start) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (data[start + i] & 0xff) << (8 * i);
        }
        return result;
    }

    public static Map<String, Integer> readLeb128(byte[] data, int start) {

        Map<String, Integer> result = new HashMap<String, Integer>();
        int value = 0;
        int length = 0;

        boolean end = false;
        int cur = start;

        while (!end) {
            value += ((data[cur] & 0x7F) << (length * 7));
            end = (data[cur] >= 0);
            length++;
            cur++;
        }

        result.put("Value", value);
        result.put("Length", length);

        return result;
    }

    public static int readShort(byte[] data, int start) {
        int result = data[start] & 0xff;
        result += (data[start + 1] & 0xff) << 8;
        return result;
    }

    public static String readString(byte[] data, int start, int length) {
        String result = "";
        byte[] tmp = readBytes(data, start, length);

        for (byte b: tmp) {
            char c = (char) (b & 0xff);
            result += c;
        }
        return result;
    }

    public static int[] readIntList(byte[] data, int start) {
        int[] result = null;
        if (start != 0) {
            int uint_size = readInt(data, start);
            result = new int[uint_size];
            for (int j = 0; j < uint_size; j++) {
                result[j] = readShort(data, start + 4);
            }
        }
        return result;
    }

    public static boolean classInDex(byte[] dexFile, String className) {
        boolean result = false;

        HeaderItem header = new HeaderItem(dexFile);
        StringIdItem stringIdItem = new StringIdItem(dexFile, header.string_ids_off, header.string_ids_size);
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, header.type_ids_off, header.type_ids_size, stringIdItem);
        ProtoIdItem protoIdItem = new ProtoIdItem(dexFile, header.proto_ids_off, header.proto_ids_size, stringIdItem, typeIdItem);
        FieldIdItem fieldIdItem = new FieldIdItem(dexFile, header.field_ids_off, header.field_ids_size, stringIdItem, typeIdItem);
        MethodIdItem methodIdItem = new MethodIdItem(dexFile, header.method_ids_off, header.method_ids_size, stringIdItem, typeIdItem, protoIdItem);
        ClassDefItem classDefItem = new ClassDefItem(dexFile, header.class_defs_off, header.class_defs_size, stringIdItem, typeIdItem, fieldIdItem, methodIdItem);
        for (ClassDefItem.ClassItem classItem: classDefItem.classItems) {
            if (classItem.clazz.contains(className))
                return true;
        }

        return false;
    }

    public static File findDexWithClass(File apk, String className) {
        File[] files = apk.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".dex"))
                    return true;
                return false;
            }
        });

        File result = null;
        try {
            for (File file: files) {
                if (classInDex(Files.readAllBytes(file.toPath()), className)) {
                    result = file;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}

package askledger.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * 간단한 JSON 읽기/쓰기 도우미.
 * DataStorage(데이터 파일)와 LLMClient(LLM 응답)에서 사용.
 */
public class Json {
    /** 저장할 때 문자열에 따옴표 붙이기 */
    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        String r = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t");
        return "\"" + r + "\"";
    }

    /** "key": "값" 에서 문자열 값을 꺼낸다 (없으면 null) */
    public static String getString(String json, String key) {
        int i = findValue(json, key);
        if (i < 0 || json.charAt(i) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        readString(json, i, sb);
        return sb.toString();
    }

    /** 숫자 값 꺼내기 */
    public static Long getLong(String json, String key) {
        int i = findValue(json, key);
        if (i < 0) {
            return null;
        }
        String number = "";
        while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '-')) {
            number += json.charAt(i);
            i++;
        }
        if (number.isEmpty()) {
            return null;
        }
        return Long.parseLong(number);
    }

    /** ["a", "b"] 같은 목록 꺼내기 */
    public static List<String> getList(String json, String key) {
        List<String> list = new ArrayList<>();
        int i = findValue(json, key);
        if (i < 0 || json.charAt(i) != '[') {
            return list;
        }
        i++;
        while (i < json.length() && json.charAt(i) != ']') {
            if (json.charAt(i) == '"') {
                StringBuilder sb = new StringBuilder();
                i = readString(json, i, sb);
                list.add(sb.toString());
            } else {
                i++;
            }
        }
        return list;
    }

    // "key": 다음에 오는 값의 시작 위치를 찾는다 (없으면 -1)
    private static int findValue(String json, String key) {
        String k = "\"" + key + "\":";
        int pos = json.indexOf(k);
        if (pos < 0) {
            return -1;
        }
        int i = pos + k.length();
        while (i < json.length() && json.charAt(i) == ' ') {
            i++;
        }
        if (i >= json.length()) {
            return -1;
        }
        return i;
    }

    // i 위치의 따옴표 문자열을 읽어서 sb에 넣고, 닫는 따옴표 다음 위치를 돌려준다
    private static int readString(String json, int i, StringBuilder sb) {
        i++;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                return i + 1;
            }
            if (c == '\\' && i + 1 < json.length()) {
                i++;
                char next = json.charAt(i);
                if (next == 'n') {
                    sb.append('\n');
                } else if (next == 't') {
                    sb.append('\t');
                } else {
                    sb.append(next);
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return i;
    }
}

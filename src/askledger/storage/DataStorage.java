package askledger.storage;

import askledger.model.Record;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 데이터 파일(JSON) 읽기/저장 */
public class DataStorage {
    /** 데이터 파일이 없을 때 쓰는 기본 카테고리 */
    public static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "Food", "Cafe", "Transport", "Shopping", "Culture", "Education", "Housing",
            "Health", "Subscription", "Salary", "Allowance", "Scholarship", "Other");

    private String fileName;
    private LedgerData data;

    public DataStorage(String fileName) {
        this.fileName = fileName;
    }

    /** 데이터 파일을 읽는다. 파일이 없으면 기본 카테고리만 있는 빈 데이터를 돌려준다 */
    public LedgerData loadAll() throws IOException {
        List<Record> records = new ArrayList<>();
        Map<String, Long> budgets = new HashMap<>();
        List<String> categories = new ArrayList<>(DEFAULT_CATEGORIES);

        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String section = "";
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("\"categories\"")) {
                    List<String> list = Json.getList(line, "categories");
                    if (!list.isEmpty()) {
                        categories.clear();
                        categories.addAll(list);
                    }
                } else if (line.startsWith("\"budgets\"")) {
                    section = "budgets";
                } else if (line.startsWith("\"records\"")) {
                    section = "records";
                } else if (line.startsWith("{\"")) {
                    if (section.equals("budgets")) {
                        budgets.put(Json.getString(line, "category"), Json.getLong(line, "amount"));
                    } else if (section.equals("records")) {
                        records.add(readRecord(line));
                    }
                }
            }
        }
        data = new LedgerData(records, budgets, categories);
        return data;
    }

    // 기록 한 줄을 Record 객체로 바꾼다
    private Record readRecord(String line) throws IOException {
        Long id = Json.getLong(line, "id");
        Long amount = Json.getLong(line, "amount");
        String date = Json.getString(line, "date");
        String type = Json.getString(line, "type");
        if (id == null || amount == null || date == null || type == null) {
            throw new IOException("Wrong record line: " + line);
        }
        return new Record(id.intValue(), LocalDate.parse(date), Record.Type.valueOf(type), amount,
                Json.getString(line, "category"), Json.getString(line, "memo"));
    }

    /** 전체 데이터를 파일에 저장 */
    public boolean saveAll() {
        if (data == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("\"categories\": [");
        List<String> categories = data.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Json.quote(categories.get(i)));
        }
        sb.append("],\n");

        sb.append("\"budgets\": [\n");
        int n = 0;
        for (String category : data.getBudgets().keySet()) {
            sb.append("{\"category\": ").append(Json.quote(category));
            sb.append(", \"amount\": ").append(data.getBudgets().get(category)).append("}");
            n++;
            if (n < data.getBudgets().size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("],\n");

        sb.append("\"records\": [\n");
        List<Record> records = data.getRecords();
        for (int i = 0; i < records.size(); i++) {
            Record r = records.get(i);
            sb.append("{\"id\": ").append(r.getId());
            sb.append(", \"date\": ").append(Json.quote(r.getDate().toString()));
            sb.append(", \"type\": ").append(Json.quote(r.getType().name()));
            sb.append(", \"amount\": ").append(r.getAmount());
            sb.append(", \"category\": ").append(Json.quote(r.getCategory()));
            sb.append(", \"memo\": ").append(Json.quote(r.getMemo())).append("}");
            if (i < records.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n}\n");

        try {
            Path path = Paths.get(fileName);
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

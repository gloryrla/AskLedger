package askledger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the local JSON data file.
 */
public class DataStorage {
    /** Categories used when there is no data file. */
    public static final List<String> DEFAULT_CATEGORIES = List.of(
            "Food", "Cafe", "Transport", "Shopping", "Culture", "Education", "Housing",
            "Health", "Subscription", "Salary", "Allowance", "Scholarship", "Other");

    private final Path file;
    private LedgerData data;

    /**
     * @param file path of the data file
     */
    public DataStorage(Path file) { this.file = file; }

    /**
     * Reads the data file. If there is no file, returns empty data with the default categories.
     * @return loaded data
     * @throws IOException if the file exists but cannot be read
     */
    @SuppressWarnings("unchecked")
    public LedgerData loadAll() throws IOException {
        List<Record> records = new ArrayList<>();
        Map<String, Long> budgets = new LinkedHashMap<>();
        List<String> categories = new ArrayList<>(DEFAULT_CATEGORIES);
        if (Files.exists(file)) {
            Map<String, Object> root = (Map<String, Object>) Json.parse(Files.readString(file, StandardCharsets.UTF_8));
            for (Object o : (List<Object>) root.getOrDefault("records", List.of())) {
                Map<String, Object> r = (Map<String, Object>) o;
                records.add(new Record(((Long) r.get("id")).intValue(), LocalDate.parse((String) r.get("date")),
                        Record.Type.valueOf((String) r.get("type")), (Long) r.get("amount"),
                        (String) r.get("category"), (String) r.get("memo")));
            }
            ((Map<String, Object>) root.getOrDefault("budgets", Map.of()))
                    .forEach((k, v) -> budgets.put(k, (Long) v));
            List<Object> cats = (List<Object>) root.get("categories");
            if (cats != null && !cats.isEmpty()) {
                categories.clear();
                cats.forEach(c -> categories.add((String) c));
            }
        }
        data = new LedgerData(records, budgets, categories);
        return data;
    }

    /**
     * Copies the current file as a backup ({@code .bak}), then writes all records and budgets.
     * The new content is written to a temporary file first, so a crash never leaves a half-written file.
     * @return true if the file is written
     */
    public boolean saveAll() {
        if (data == null) return false;
        try {
            List<Object> recs = new ArrayList<>();
            for (Record r : data.records()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("date", r.getDate().toString());
                m.put("type", r.getType().name());
                m.put("amount", r.getAmount());
                m.put("category", r.getCategory());
                m.put("memo", r.getMemo());
                recs.add(m);
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("records", recs);
            root.put("budgets", data.budgets());
            root.put("categories", data.categories());
            Path abs = file.toAbsolutePath();
            if (abs.getParent() != null) Files.createDirectories(abs.getParent());
            if (Files.exists(abs)) Files.copy(abs, abs.resolveSibling(abs.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            Path tmp = abs.resolveSibling(abs.getFileName() + ".tmp");
            Files.writeString(tmp, Json.write(root), StandardCharsets.UTF_8);
            Files.move(tmp, abs, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

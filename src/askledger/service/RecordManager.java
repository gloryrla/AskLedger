package askledger.service;

import askledger.model.Record;
import askledger.model.SearchQuery;
import askledger.storage.DataStorage;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 전체 기록 목록을 관리하는 클래스 */
public class RecordManager {
    private DataStorage storage;
    private List<Record> records = new ArrayList<>();

    public RecordManager(DataStorage storage) {
        this.storage = storage;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    /** 새 id를 붙여서 추가하고 저장 (id는 지금 가장 큰 id + 1) */
    public boolean addRecord(Record record) {
        int maxId = 0;
        for (Record r : records) {
            if (r.getId() > maxId) {
                maxId = r.getId();
            }
        }
        record.setId(maxId + 1);
        records.add(record);
        return storage.saveAll();
    }

    /** 기록 수정 */
    public boolean updateRecord(int id, Record record) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == id) {
                record.setId(id);
                records.set(i, record);
                return storage.saveAll();
            }
        }
        return false;
    }

    /** 기록 삭제 */
    public boolean deleteRecord(int id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == id) {
                records.remove(i);
                return storage.saveAll();
            }
        }
        return false;
    }

    /** 조건에 모두 맞는 기록을 날짜순으로 돌려준다 */
    public List<Record> findRecords(SearchQuery query) {
        List<Record> result = new ArrayList<>();
        for (Record r : records) {
            if (matches(r, query)) {
                result.add(r);
            }
        }
        Collections.sort(result);
        return result;
    }

    /** 해당 달의 기록을 날짜순으로 돌려준다 */
    public List<Record> getRecordsByMonth(YearMonth month) {
        List<Record> result = new ArrayList<>();
        for (Record r : records) {
            if (r.getDate().getYear() == month.getYear() && r.getDate().getMonthValue() == month.getMonthValue()) {
                result.add(r);
            }
        }
        Collections.sort(result);
        return result;
    }

    // 조건이 null이면 그 조건은 검사 안 함
    private boolean matches(Record r, SearchQuery q) {
        if (q.getStartDate() != null && r.getDate().isBefore(q.getStartDate())) {
            return false;
        }
        if (q.getEndDate() != null && r.getDate().isAfter(q.getEndDate())) {
            return false;
        }
        if (q.getType() != null && r.getType() != q.getType()) {
            return false;
        }
        if (q.getCategories() != null && !q.getCategories().isEmpty() && !q.getCategories().contains(r.getCategory())) {
            return false;
        }
        if (q.getMinAmount() != null && r.getAmount() < q.getMinAmount()) {
            return false;
        }
        if (q.getMaxAmount() != null && r.getAmount() > q.getMaxAmount()) {
            return false;
        }
        if (q.getKeywords() != null && !q.getKeywords().isEmpty()) {
            String text = (r.getMemo() + " " + r.getCategory()).toLowerCase();
            boolean found = false;
            // 키워드 중 하나라도 메모나 카테고리에 있으면 통과
            for (String keyword : q.getKeywords()) {
                if (text.contains(keyword.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}


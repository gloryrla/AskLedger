package askledger.model;

import java.util.List;

/** 검색 결과 (기록 목록, 개수, 합계) */
public class SearchResult {
    private SearchQuery query;
    private List<Record> records;
    private int count;
    private long total;
    private boolean usedRuleParser;   // LLM 대신 RuleParser를 썼는지

    public SearchResult(List<Record> records, int count, long total) {
        this.records = records;
        this.count = count;
        this.total = total;
    }

    public SearchQuery getQuery() {
        return query;
    }

    public void setQuery(SearchQuery query) {
        this.query = query;
    }

    public List<Record> getRecords() {
        return records;
    }

    public int getCount() {
        return count;
    }

    public long getTotal() {
        return total;
    }

    public boolean isUsedRuleParser() {
        return usedRuleParser;
    }

    public void setUsedRuleParser(boolean usedRuleParser) {
        this.usedRuleParser = usedRuleParser;
    }
}

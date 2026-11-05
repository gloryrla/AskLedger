package askledger.service;

import askledger.model.Record;
import askledger.model.SearchQuery;
import askledger.model.SearchResult;

import java.util.List;

/**
 * 질문 검색을 처리하는 클래스: LLM을 먼저 쓰고, 실패하면 RuleParser를 쓴다.
 * 개수와 합계는 LLM이 아니라 여기서 직접 계산한다.
 */
public class SearchService {
    private LLMClient llmClient;
    private RuleParser ruleParser;
    private RecordManager recordManager;
    private List<String> categories;

    public SearchService(LLMClient llmClient, RuleParser ruleParser, RecordManager recordManager, List<String> categories) {
        this.llmClient = llmClient;
        this.ruleParser = ruleParser;
        this.recordManager = recordManager;
        this.categories = categories;
    }

    /** 질문으로 검색. LLM 결과가 없거나 이상하면 RuleParser 사용 */
    public SearchResult search(String question) {
        SearchQuery query = llmClient.requestQuery(question);
        boolean usedRuleParser = false;
        if (query == null || !validate(query)) {
            query = ruleParser.parse(question);
            usedRuleParser = true;
        }
        List<Record> records = recordManager.findRecords(query);
        long total = 0;
        for (Record r : records) {
            total += r.getAmount();
        }
        SearchResult result = new SearchResult(records, records.size(), total);
        result.setQuery(query);
        result.setUsedRuleParser(usedRuleParser);
        return result;
    }

    /** 사용자가 고친 조건으로 다시 검색 (LLM 안 씀). 조건이 틀리면 null */
    public SearchResult apply(SearchQuery query) {
        if (!validate(query)) {
            return null;
        }
        List<Record> records = recordManager.findRecords(query);
        long total = 0;
        for (Record r : records) {
            total += r.getAmount();
        }
        SearchResult result = new SearchResult(records, records.size(), total);
        result.setQuery(query);
        return result;
    }

    /** 검색 조건이 이상한지 검사 (날짜 순서, 음수 금액, 없는 카테고리) */
    public boolean validate(SearchQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            return false;
        }
        if (query.getMinAmount() != null && query.getMinAmount() < 0) {
            return false;
        }
        if (query.getMaxAmount() != null && query.getMaxAmount() < 0) {
            return false;
        }
        if (query.getCategories() != null) {
            for (String c : query.getCategories()) {
                if (!categories.contains(c)) {
                    return false;
                }
            }
        }
        return true;
    }
}

package com.ved.BackendAnalyzer.rules;

import java.util.ArrayList;
import java.util.List;

import com.ved.BackendAnalyzer.model.Issue;
import com.ved.BackendAnalyzer.model.MethodCallInfo;

public class UnpaginatedFindAllRule {

    public List<Issue> check(List<MethodCallInfo> calls) {
        List<Issue> issues = new ArrayList<>();

        for (MethodCallInfo call : calls) {
            if (call.getMethodName().equals("findAll")) {
                String location = call.getCallerClass();
                if (call.getLineNumber() > 0) {
                    location += " (line " + call.getLineNumber() + ")";
                }
                issues.add(new Issue(
                        "UNPAGINATED_FIND_ALL",
                        Issue.Severity.MEDIUM,
                        "Possible unpaginated findAll() usage. Consider using Pageable.",
                        location
                ));
            }
        }
        return issues;
    }
}

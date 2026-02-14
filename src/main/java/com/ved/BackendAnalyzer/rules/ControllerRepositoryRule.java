package com.ved.BackendAnalyzer.rules;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ved.BackendAnalyzer.model.ClassInfo;
import com.ved.BackendAnalyzer.model.MethodCallInfo;

public class ControllerRepositoryRule {

    public void check(List<ClassInfo> classes, List<MethodCallInfo> calls) {

        // 1. find controller class names
        Set<String> controllers = classes.stream()
                .filter(c -> c.getType() == ClassInfo.Type.CONTROLLER)
                .map(c -> c.getClassName())
                .collect(Collectors.toSet());

        // 2. find repository class names
        Set<String> repositories = classes.stream()
                .filter(c -> c.getType() == ClassInfo.Type.REPOSITORY)
                .map(c -> c.getClassName())
                .collect(Collectors.toSet());

        // 3. detect violations
        for (MethodCallInfo call : calls) {
            String caller = call.getCallerClass();
            String calledObject = call.getCalledObject();

            if (controllers.contains(caller)) {
                for (String repo : repositories) {
                    if (calledObject.toLowerCase().contains(repo.replace(".java","").toLowerCase())) {
                        System.out.println(
                                "[HIGH] Controller " + caller +
                                " directly calls Repository (" + calledObject + ")"
                        );
                    }
                }
            }
        }
    }
}

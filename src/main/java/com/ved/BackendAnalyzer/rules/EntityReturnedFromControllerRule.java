package com.ved.BackendAnalyzer.rules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ved.BackendAnalyzer.model.ClassInfo;

public class EntityReturnedFromControllerRule {

    public void check(List<ClassInfo> classes) {

        // collect entity class names
        Set<String> entities = classes.stream()
                .filter(c -> c.getType() == ClassInfo.Type.ENTITY)
                .map(c -> c.getClassName())
                .collect(Collectors.toSet());

        // check controllers returning entities (heuristic)
        classes.stream()
                .filter(c -> c.getType() == ClassInfo.Type.CONTROLLER)
                .forEach(controller -> {
                    for (String entity : entities) {
                        if (controller.getClassName().toLowerCase()
                                .contains(entity.replace(".java","").toLowerCase())) {

                            System.out.println(
                                "[MEDIUM] Controller " + controller.getClassName() +
                                " may be exposing Entity directly. Consider using DTOs."
                            );
                        }
                    }
                });
    }
}

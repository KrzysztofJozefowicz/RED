/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rf.ide.core.project.RobotProjectConfig.VariableMapping;
import org.rf.ide.core.testdata.model.table.variables.names.VariableNamesSupport;

class VariableMappingsResolver {

    private static final Pattern SCALAR_PATTERN = Pattern.compile("\\$\\{[^\\$\\{}]+}");

    static Map<String, String> resolve(final List<VariableMapping> variableMappings) {
        final Map<String, String> resolvedVariables = new HashMap<>();
        for (final VariableMapping mapping : variableMappings) {
            if (isScalarDefinition(mapping.getName())) {
                final String unifiedName = VariableNamesSupport.extractUnifiedVariableName(mapping.getName());
                final String resolvedValue = replaceKnownScalarVariables(mapping.getValue(), resolvedVariables);
                resolvedVariables.put(unifiedName, resolvedValue);
            }
        }
        return resolvedVariables;
    }

    private static boolean isScalarDefinition(final String name) {
        return Pattern.matches("^" + SCALAR_PATTERN.pattern() + "$", name);
    }

    private static String replaceKnownScalarVariables(final String value, final Map<String, String> resolvedVariables) {
        final StringBuffer result = new StringBuffer();
        final Matcher matcher = SCALAR_PATTERN.matcher(value);
        while (matcher.find()) {
            final String matchedUnifiedName = VariableNamesSupport.extractUnifiedVariableName(matcher.group());
            final String replacement = resolvedVariables.getOrDefault(matchedUnifiedName, matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}

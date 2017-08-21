/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.agent.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.rf.ide.core.execution.debug.KeywordCallType;
import org.rf.ide.core.execution.debug.RunningKeyword;

public final class KeywordStartedEvent {

    private final String name;

    private final String keywordType;

    private final String libraryName;

    private final List<Map<Variable, VariableTypedValue>> variables;

    public static KeywordStartedEvent fromPre(final Map<String, Object> eventMap) {
        return fromEventArguments((List<?>) eventMap.get("pre_start_keyword"));
    }

    public static KeywordStartedEvent from(final Map<String, Object> eventMap) {
        return fromEventArguments((List<?>) eventMap.get("start_keyword"));
    }

    private static KeywordStartedEvent fromEventArguments(final List<?> arguments) {
        final Map<?, ?> attributes = (Map<?, ?>) arguments.get(1);
        final String keywordName = (String) attributes.get("kwname");
        final String keywordType = (String) attributes.get("type");
        final String libraryName = (String) attributes.get("libname");
        final List<Map<Variable, VariableTypedValue>> variables = Events
                .extractVariableScopes((List<?>) attributes.get("vars_scopes"));

        return new KeywordStartedEvent(keywordName, keywordType, libraryName, variables);
    }

    public KeywordStartedEvent(final String name, final String keywordType, final String libraryName,
            final List<Map<Variable, VariableTypedValue>> variables) {
        this.name = name;
        this.keywordType = keywordType;
        this.libraryName = libraryName;
        this.variables = variables;
    }

    public String getName() {
        return name;
    }

    public String getKeywordType() {
        return keywordType;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public RunningKeyword getRunningKeyword() {
        return new RunningKeyword(libraryName, name, KeywordCallType.from(keywordType));
    }

    public List<Map<Variable, VariableTypedValue>> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == KeywordStartedEvent.class) {
            final KeywordStartedEvent that = (KeywordStartedEvent) obj;
            return this.name.equals(that.name) && this.keywordType.equals(that.keywordType)
                    && this.libraryName.equals(that.libraryName) && this.variables.equals(that.variables);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keywordType, libraryName, variables);
    }
}
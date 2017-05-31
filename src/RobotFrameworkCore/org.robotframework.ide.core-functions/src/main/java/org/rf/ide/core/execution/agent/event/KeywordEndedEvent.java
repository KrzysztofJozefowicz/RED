/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.agent.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KeywordEndedEvent {

    private final String name;

    private final String keywordType;

    public static KeywordEndedEvent from(final Map<String, Object> eventMap) {
        final List<?> arguments = (List<?>) eventMap.get("end_keyword");
        final String name = (String) arguments.get(0);
        final Map<?, ?> attributes = (Map<?, ?>) arguments.get(1);
        final String keywordType = (String) attributes.get("type");

        return new KeywordEndedEvent(name, keywordType);
    }

    public KeywordEndedEvent(final String name, final String keywordType) {
        this.name = name;
        this.keywordType = keywordType;
    }

    public String getName() {
        return name;
    }

    public String getKeywordType() {
        return keywordType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == KeywordEndedEvent.class) {
            final KeywordEndedEvent that = (KeywordEndedEvent) obj;
            return this.name.equals(that.name) && this.keywordType.equals(that.keywordType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keywordType);
    }
}
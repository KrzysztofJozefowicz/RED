/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.server.response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.ImmutableMap;

public final class EvaluateCondition implements ServerResponseOnShouldContinue {

    private final List<String> conditionWithArguments;

    public EvaluateCondition(final List<String> conditionWithArguments) {
        this.conditionWithArguments = conditionWithArguments;
    }

    @Override
    public String toMessage() {
        try {
            final Map<String, Object> value = ImmutableMap.of("evaluate_condition", conditionWithArguments);

            return new ObjectMapper().writeValueAsString(value);
        } catch (final IOException e) {
            throw new ResponseException("Unable to serialize breakpoint condition response arguments to json", e);
        }
    }
}

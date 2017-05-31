/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.agent.event;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.rf.ide.core.executor.RedURI;

class Events {

    static URI toFileUri(final String source) {
        if (source == null) {
            return null;
        }
        try {
            final String escaped = RedURI.URI_SPECIAL_CHARS_ESCAPER.escape(source);
            return new URI("file://" + (escaped.startsWith("/") ? "" : "/") + escaped.replaceAll("\\\\", "/"));
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    static List<String> ensureListOfStrings(final List<?> list) {
        return list.stream().map(String.class::cast).collect(Collectors.toList());
    }

    static Map<String, Object> ensureOrderedMapOfStringsToObjects(final Map<?, ?> map) {
        final LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        map.entrySet().stream().forEach(e -> result.put((String) e.getKey(), e.getValue()));
        return result;
    }
}

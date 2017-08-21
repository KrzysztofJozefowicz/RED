/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.server.response;

@FunctionalInterface
public interface ServerResponseOnShouldContinue extends ServerResponse {
    // marker interface in order to structure possible responses to ShouldContinueEvent
}

/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.server.response;


public class ContinueExecution implements ServerResponse {

    @Override
    public String toMessage() {
        return "continue";
    }

}

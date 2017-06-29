/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model.cmd;

import java.util.List;

import org.robotframework.ide.eclipse.main.plugin.model.RobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

public class MoveKeywordCallUpCommand extends EditorCommand {

    private final RobotKeywordCall keywordCall;
    private boolean wasMoved = false;

    public MoveKeywordCallUpCommand(final RobotKeywordCall keywordCall) {
        this.keywordCall = keywordCall;
    }

    @Override
    public void execute() throws CommandExecutionException {

        final RobotCodeHoldingElement<?> parent = (RobotCodeHoldingElement<?>) keywordCall.getParent();
        final int index = keywordCall.getIndex();

        if (index == 0) {
            // no place to move it further up
            return;
        } else {
            wasMoved = true;
            parent.moveChildUp(keywordCall);

            eventBroker.send(RobotModelEvents.ROBOT_KEYWORD_CALL_MOVED, parent);
        }
    }

    @Override
    public List<EditorCommand> getUndoCommands() {
        return newUndoCommands(wasMoved ? new MoveKeywordCallDownCommand(keywordCall) : new EmptyCommand());
    }
}

/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model.cmd;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

public class ConvertSettingToComment extends EditorCommand {

    private final RobotKeywordCall settingCall;

    private RobotKeywordCall commentCall;

    private final String newName;

    public ConvertSettingToComment(final IEventBroker eventBroker, final RobotKeywordCall settingCall,
            final String name) {
        this.eventBroker = eventBroker;
        this.settingCall = settingCall;
        this.newName = name;
        this.commentCall = null;
    }

    @Override
    public void execute() throws CommandExecutionException {

        final List<RobotToken> tokens = settingCall.getLinkedElement()
                .getElementTokens()
                .stream()
                .map(RobotToken::copy)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return;
        }

        final RobotExecutableRow<?> newLinked = new RobotExecutableRow<>();
        newLinked.getAction().setType(settingCall.getParent() instanceof RobotCase
                ? RobotTokenType.TEST_CASE_ACTION_NAME : RobotTokenType.KEYWORD_ACTION_NAME);

        final RobotToken first = tokens.get(0);
        first.setType(RobotTokenType.START_HASH_COMMENT);
        first.setText(newName);
        newLinked.addCommentPart(first);

        for (int i = 1; i < tokens.size(); i++) {
            final RobotToken token = tokens.get(i);
            token.setType(RobotTokenType.COMMENT_CONTINUE);
            newLinked.addCommentPart(token);
        }

        final RobotCodeHoldingElement<?> parent = (RobotCodeHoldingElement<?>) settingCall.getParent();

        parent.removeUnitSettings(settingCall);

        final int index = settingCall.getIndex();
        parent.removeChild(settingCall);

        commentCall = new RobotKeywordCall(parent, newLinked);
        parent.insertKeywordCall(index, commentCall);

        eventBroker.send(RobotModelEvents.ROBOT_KEYWORD_CALL_COMMENT_CHANGE, parent);
    }

    @Override
    public List<EditorCommand> getUndoCommands() {
        return newArrayList(new ReplaceRobotKeywordCallCommand(eventBroker, commentCall, settingCall));
    }

}

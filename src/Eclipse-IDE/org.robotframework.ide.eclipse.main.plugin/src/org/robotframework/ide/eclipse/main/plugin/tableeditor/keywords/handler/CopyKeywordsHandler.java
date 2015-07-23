package org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords.handler;

import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.robotframework.ide.eclipse.main.plugin.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.RobotKeywordDefinition;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.dnd.KeywordCallsTransfer;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.dnd.KeywordDefinitionsTransfer;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords.handler.CopyKeywordsHandler.E4CopyKeywordsHandler;
import org.robotframework.red.viewers.Selections;

public class CopyKeywordsHandler extends DIHandler<E4CopyKeywordsHandler> {

    public CopyKeywordsHandler() {
        super(E4CopyKeywordsHandler.class);
    }

    public static class E4CopyKeywordsHandler {

        @Execute
        public Object copyKeywords(@Named(Selections.SELECTION) final IStructuredSelection selection,
                final Clipboard clipboard) {

            final List<RobotKeywordDefinition> defs = Selections.getElements(selection, RobotKeywordDefinition.class);
            final List<RobotKeywordCall> calls = Selections.getElements(selection, RobotKeywordCall.class);
            if (!defs.isEmpty()) {
                clipboard.setContents(
                        new RobotKeywordDefinition[][] { defs.toArray(new RobotKeywordDefinition[defs.size()]) },
                        new Transfer[] { KeywordDefinitionsTransfer.getInstance() });
            } else if (!calls.isEmpty()) {
                clipboard.setContents(new RobotKeywordCall[][] { calls.toArray(new RobotKeywordCall[calls.size()]) },
                        new Transfer[] { KeywordCallsTransfer.getInstance() });
            }
            return null;
        }
    }
}

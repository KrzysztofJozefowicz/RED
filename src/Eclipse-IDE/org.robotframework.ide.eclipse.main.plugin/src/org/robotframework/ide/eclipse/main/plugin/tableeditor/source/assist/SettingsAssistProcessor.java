/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.robotframework.ide.eclipse.main.plugin.assist.AssistProposal;
import org.robotframework.ide.eclipse.main.plugin.assist.RedSettingProposals;
import org.robotframework.ide.eclipse.main.plugin.assist.RedSettingProposals.SettingTarget;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.DocumentUtilities;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourcePartitionScanner;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.RedCompletionProposalAdapter.DocumentModification;


/**
 * @author Michal Anglart
 *
 */
public class SettingsAssistProcessor extends RedContentAssistProcessor {

    public SettingsAssistProcessor(final SuiteSourceAssistantContext assist) {
        super(assist);
    }

    @Override
    protected String getProposalsTitle() {
        return "Test Case/Keyword settings";
    }

    @Override
    protected List<String> getApplicableContentTypes() {
        return newArrayList(SuiteSourcePartitionScanner.TEST_CASES_SECTION,
                SuiteSourcePartitionScanner.KEYWORDS_SECTION);
    }

    @Override
    protected boolean shouldShowProposals(final IDocument document, final int offset, final String lineContent)
            throws BadLocationException {
        return isInApplicableContentType(document, offset)
                && DocumentUtilities.getNumberOfCellSeparators(lineContent, assist.isTsvFile()) == 1;
    }

    @Override
    protected List<? extends ICompletionProposal> computeProposals(final IDocument document, final int offset,
            final int cellLength, final String prefix, final boolean atTheEndOfLine) throws BadLocationException {

        final String additionalContent = atTheEndOfLine ? assist.getSeparatorToFollow() : "";

        final List<? extends AssistProposal> settingsProposals = new RedSettingProposals(getTarget(document, offset))
                .getSettingsProposals(prefix);

        final List<ICompletionProposal> proposals = newArrayList();
        for (final AssistProposal settingProposal : settingsProposals) {
            final DocumentModification modification = new DocumentModification(additionalContent,
                    new Position(offset - prefix.length(), cellLength));

            proposals.add(new RedCompletionProposalAdapter(settingProposal, modification));
        }
        return proposals;
    }

    private SettingTarget getTarget(final IDocument document, final int offset) throws BadLocationException {
        final String contentType = getVirtualContentType(document, offset);
        if (SuiteSourcePartitionScanner.TEST_CASES_SECTION.equals(contentType)) {
            return SettingTarget.TEST_CASE;
        } else if (SuiteSourcePartitionScanner.KEYWORDS_SECTION.equals(contentType)) {
            return SettingTarget.KEYWORD;
        }
        throw new IllegalStateException("Should never be called");
    }
}

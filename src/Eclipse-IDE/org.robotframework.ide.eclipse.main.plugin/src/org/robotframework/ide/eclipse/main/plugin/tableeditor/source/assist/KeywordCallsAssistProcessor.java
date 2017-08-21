/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;
import org.robotframework.ide.eclipse.main.plugin.assist.AssistProposal;
import org.robotframework.ide.eclipse.main.plugin.assist.RedKeywordProposal;
import org.robotframework.ide.eclipse.main.plugin.assist.RedKeywordProposals;
import org.robotframework.ide.eclipse.main.plugin.model.locators.KeywordEntity;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.DocumentUtilities;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourcePartitionScanner;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.RedCompletionProposalAdapter.DocumentationModification;
import org.robotframework.red.jface.text.link.RedEditorLinkedModeUI;
import org.robotframework.red.swt.SwtThread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

/**
 * @author Michal Anglart
 */
public class KeywordCallsAssistProcessor extends RedContentAssistProcessor {

    public KeywordCallsAssistProcessor(final SuiteSourceAssistantContext assist) {
        super(assist);
    }

    @Override
    protected String getProposalsTitle() {
        return "Keywords";
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
                && DocumentUtilities.getNumberOfCellSeparators(lineContent, assist.isTsvFile()) > 0;
    }

    @Override
    protected List<? extends ICompletionProposal> computeProposals(final IDocument document, final int offset,
            final int cellLength, final String prefix, final boolean atTheEndOfLine) throws BadLocationException {

        final List<RedKeywordProposal> kwProposals = new RedKeywordProposals(assist.getModel())
                .getKeywordProposals(prefix);

        final String separator = assist.getSeparatorToFollow();
        final List<ICompletionProposal> proposals = newArrayList();

        final String lineContent = DocumentUtilities.lineContentBeforeCurrentPosition(document, offset);
        for (final AssistProposal kwProposal : kwProposals) {
            final List<String> args = atTheEndOfLine ? getArguments(kwProposal, lineContent) : new ArrayList<String>();
            final String contentSuffix = args.isEmpty() ? "" : (separator + Joiner.on(separator).join(args));

            final Position positionToReplace = new Position(offset - prefix.length(), cellLength);
            final Collection<Runnable> operations = atTheEndOfLine ? createOperationsToPerformAfterAccepting(viewer,
                    (KeywordEntity) kwProposal, positionToReplace.getOffset(), lineContent) : new ArrayList<Runnable>();
            final DocumentationModification modification = new DocumentationModification(contentSuffix,
                    positionToReplace, operations);
            final IContextInformation contextInfo = new ContextInformation(null,
                    ((KeywordEntity) kwProposal).getArgumentsDescriptor().getDescription());
            proposals.add(new RedCompletionProposalAdapter(kwProposal, modification, contextInfo));
        }
        return proposals;
    }

    protected List<String> getArguments(final AssistProposal proposal, final String lineContent) {
        return proposal.getArguments();
    }

    private Collection<Runnable> createOperationsToPerformAfterAccepting(final ITextViewer viewer,
            final KeywordEntity entity, final int startOffset, final String lineContent) {
        final Runnable operation = new Runnable() {

            @Override
            public void run() {
                final Collection<IRegion> regionsToLinkedEdit = calculateRegionsForLinkedMode(entity, startOffset,
                        lineContent);
                if (!regionsToLinkedEdit.isEmpty()) {
                    SwtThread.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            RedEditorLinkedModeUI.enableLinkedMode(viewer, regionsToLinkedEdit);
                        }
                    });
                }
            }
        };
        return newArrayList(operation);
    }

    @VisibleForTesting
    Collection<IRegion> calculateRegionsForLinkedMode(final KeywordEntity entity, final int startOffset,
            final String lineContent) {
        final AssistProposal proposal = (AssistProposal) entity;
        final String keywordName = entity.getNameFromDefinition();

        if (EmbeddedKeywordNamesSupport.hasEmbeddedArguments(keywordName)) {
            return calculateRegionsForLinkedModeOfEmbeddedKeyword(startOffset, proposal.getContent());
        } else {
            final int separatorLength = assist.getSeparatorToFollow().length();
            return calculateRegionsForLinkedModeOfRegularKeyword(startOffset, proposal.getContent(),
                    getArguments(proposal, lineContent), separatorLength);
        }
    }

    private Collection<IRegion> calculateRegionsForLinkedModeOfEmbeddedKeyword(final int startOffset,
            final String wholeContent) {
        final Collection<IRegion> regions = new ArrayList<>();
        final Matcher matcher = Pattern.compile("\\$\\{[^\\}]+\\}").matcher(wholeContent);
        while (matcher.find()) {
            regions.add(new Region(startOffset + matcher.start(), matcher.end() - matcher.start()));
        }
        return regions;
    }

    private Collection<IRegion> calculateRegionsForLinkedModeOfRegularKeyword(final int startOffset,
            final String wholeContent, final List<String> arguments, final int separatorLength) {
        final Collection<IRegion> regions = new ArrayList<>();
        int offset = startOffset + wholeContent.length();
        if (!arguments.isEmpty()) {
            offset += separatorLength;
        }
        for (final String requiredArg : arguments) {
            regions.add(new Region(offset, requiredArg.length()));
            offset += requiredArg.length() + separatorLength;
        }
        return regions;
    }
}

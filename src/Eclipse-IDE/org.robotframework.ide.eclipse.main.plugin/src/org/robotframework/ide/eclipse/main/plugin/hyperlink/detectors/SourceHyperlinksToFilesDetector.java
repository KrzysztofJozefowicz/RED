/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.hyperlink.detectors;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.DocumentUtilities;

/**
 * @author Michal Anglart
 */
public class SourceHyperlinksToFilesDetector extends HyperlinksToFilesDetector implements IHyperlinkDetector {

    private final RobotSuiteFile suiteFile;

    public SourceHyperlinksToFilesDetector(final RobotSuiteFile suiteFile) {
        this.suiteFile = suiteFile;
    }

    @Override
    public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
            final boolean canShowMultipleHyperlinks) {
        try {
            final IDocument document = textViewer.getDocument();
            final Optional<IRegion> hyperlinkRegion = DocumentUtilities.findCellRegion(document, suiteFile.isTsvFile(),
                    region.getOffset());
            if (!hyperlinkRegion.isPresent()) {
                return null;
            }
            final String lineContent = DocumentUtilities.lineContentBeforeCurrentPosition(document,
                    hyperlinkRegion.get().getOffset());
            if (!isApplicable(lineContent)) {
                return null;
            }
            final IRegion fromRegion = hyperlinkRegion.get();
            final String pathAsString = document.get(fromRegion.getOffset(), fromRegion.getLength());
            final boolean isLibraryImport = lineContent.trim().toLowerCase().startsWith("library");

            final List<IHyperlink> hyperlinks = detectHyperlinks(suiteFile, fromRegion, pathAsString, isLibraryImport);
            return hyperlinks.isEmpty() ? null : hyperlinks.toArray(new IHyperlink[0]);
        } catch (final BadLocationException e) {
            return null;
        }
    }

    private boolean isApplicable(final String lineContent) {
        return lineContent.trim().toLowerCase().startsWith("resource")
                || lineContent.trim().toLowerCase().startsWith("variables")
                || lineContent.trim().toLowerCase().startsWith("library");
    }

    @Override
    protected Function<IFile, Void> performAfterOpening() {
        return new Function<IFile, Void>() {

            @Override
            public Void apply(final IFile file) {
                final IEditorPart activeEditor = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage()
                        .getActiveEditor();
                if (activeEditor instanceof RobotFormEditor
                        && activeEditor.getEditorInput().equals(new FileEditorInput(file))) {
                    final RobotFormEditor suiteEditor = (RobotFormEditor) activeEditor;
                    suiteEditor.activateSourcePage();
                }
                return null;
            }
        };
    }
}

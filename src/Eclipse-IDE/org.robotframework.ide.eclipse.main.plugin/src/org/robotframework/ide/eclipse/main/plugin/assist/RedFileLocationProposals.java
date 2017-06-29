/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.rf.ide.core.executor.RedSystemProperties;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting.SettingsGroup;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;

import com.google.common.base.Supplier;

public abstract class RedFileLocationProposals {

    private final RobotSuiteFile suiteFile;

    private final Supplier<List<IFile>> filesSupplier;

    private final ProposalMatcher matcher;

    protected RedFileLocationProposals(final RobotSuiteFile suiteFile, final Supplier<List<IFile>> filesSupplier,
            final ProposalMatcher matcher) {
        this.suiteFile = suiteFile;
        this.filesSupplier = filesSupplier;
        this.matcher = matcher;
    }

    public static RedFileLocationProposals create(final SettingsGroup importType, final RobotSuiteFile suiteFile) {
        return create(importType, suiteFile, ProposalMatchers.pathsMatcher());
    }

    public static RedFileLocationProposals create(final SettingsGroup importType, final RobotSuiteFile suiteFile,
            final ProposalMatcher matcher) {
        switch (importType) {
            case RESOURCES:
                return new RedResourceFileLocationsProposals(suiteFile, matcher);
            case VARIABLES:
                return new RedPythonFileLocationsProposals(suiteFile, matcher);
            case LIBRARIES:
                return new RedPythonFileLocationsProposals(suiteFile, matcher);
            default:
                throw new IllegalStateException("Unknown value: " + importType);
        }
    }

    public List<? extends AssistProposal> getFilesLocationsProposals(final String userContent) {
        final String projectFolderName = suiteFile.getProject().getProject().getFullPath().segment(0);
        return getFilesLocationsProposals(userContent, ImportedFiles.createComparator(projectFolderName));
    }

    public List<? extends AssistProposal> getFilesLocationsProposals(final String userContent,
            final Comparator<IFile> comparator) {

        final List<RedFileLocationProposal> proposals = new ArrayList<>();

        final List<IFile> files = filesSupplier.get();
        Collections.sort(files, comparator);

        for (final IFile varFile : files) {

            final IFile fromFile = suiteFile.getFile();
            final String content;
            if (RedSystemProperties.isWindowsPlatform()
                    && !fromFile.getLocation().getDevice().equals(varFile.getLocation().getDevice())) {
                content = varFile.getLocation().toString();
            } else {
                content = createCurrentFileRelativePath(fromFile, varFile);
            }
            final Optional<ProposalMatch> match = matcher.matches(userContent, content);

            if (match.isPresent()) {
                proposals.add(AssistProposals.createFileLocationProposal(content, varFile, match.get()));
            }
        }
        return proposals;
    }

    private static String createCurrentFileRelativePath(final IFile from, final IFile to) {
        return to.getLocation().makeRelativeTo(from.getLocation()).removeFirstSegments(1).toString();
    }

    private static class RedPythonFileLocationsProposals extends RedFileLocationProposals {

        private RedPythonFileLocationsProposals(final RobotSuiteFile suiteFile, final ProposalMatcher matcher) {
            super(suiteFile, new Supplier<List<IFile>>() {
                @Override
                public List<IFile> get() {
                    return ImportedFiles.getPythonFiles();
                }
            }, matcher);
        }
    }

    private static class RedResourceFileLocationsProposals extends RedFileLocationProposals {

        private RedResourceFileLocationsProposals(final RobotSuiteFile suiteFile, final ProposalMatcher matcher) {
            super(suiteFile, new Supplier<List<IFile>>() {

                @Override
                public List<IFile> get() {
                    return ImportedFiles.getResourceFiles(suiteFile.getFile());
                }
            }, matcher);
        }
    }
}

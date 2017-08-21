/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import com.google.common.collect.Range;

/**
 * @author Michal Anglart
 */
public class DocumentUtilities {

    /**
     * Returns region around offset which constitutes a single robot variable.
     *
     * @param document
     *            Document in which variable should be find
     * @param offset
     *            Current offset at which search should start
     * @return The region describing location of variable or absent if offset lies outside variable
     * @throws BadLocationException
     */
    public static Optional<IRegion> findVariable(final IDocument document, final boolean isTsv, final int offset)
            throws BadLocationException {
        final Optional<IRegion> cellRegion = findCellRegion(document, isTsv, offset);
        if (cellRegion.isPresent()) {
            final String cellContent = document.get(cellRegion.get().getOffset(), cellRegion.get().getLength());

            final int projectedOffset = offset - cellRegion.get().getOffset();

            return findVariable(cellContent, projectedOffset)
                    .map(reg -> new Region(reg.getOffset() + cellRegion.get().getOffset(), reg.getLength()));
        }
        return Optional.empty();
    }

    public static Optional<IRegion> findVariable(final String cellContent, final int offset) {
        final Stack<Integer> positions = new Stack<>();

        int stackLevel = -1;
        int lastIndex = 0;
        for (int i = 0; i < cellContent.length(); i++) {
            if (varStartDetected(cellContent, i)) {
                positions.push(i);
            }
            if (i == offset) {
                stackLevel = positions.size() - 1;
            }
            if (varEndDetected(cellContent, i) && !positions.isEmpty()) {
                lastIndex = positions.pop();
                if (stackLevel == positions.size() && i >= offset) {
                    return Optional.of(new Region(lastIndex, i - lastIndex + 1));
                }
            }
        }
        return Optional.empty();
    }

    public static String getFirstTokenInLine(final IDocument document, final boolean isTSV, final int offset)
            throws BadLocationException {
        int begin = offset;

        while (Character.isWhitespace(document.get(begin, 1).charAt(0)) && document.get(begin, 1).charAt(0) != '\n') {
            begin++;
        }

        int end = begin;
        if (!isTSV) {
            while (!(Character.isWhitespace(document.get(end, 1).charAt(0))
                    && Character.isWhitespace(document.get(end + 1, 1).charAt(0))
                    || document.get(end, 1).charAt(0) == '\t')) {
                end++;
            }
        } else {
            while (!(document.get(end, 1).charAt(0) == '\t' || document.get(end, 1).charAt(0) == '\n')) {
                end++;
            }
        }

        return document.get(begin, end - begin);
    }

    private static boolean varStartDetected(final String cellContent, final int i) {
        if (i + 1 < cellContent.length()) {
            return newHashSet("${", "@{", "%{", "&{").contains(cellContent.substring(i, i + 2));
        }
        return false;

    }

    private static boolean varEndDetected(final String cellContent, final int i) {
        return i < cellContent.length() && cellContent.charAt(i) == '}';
    }

    public static Optional<IRegion> findLiveVariable(final IDocument document, final boolean isTsv, final int offset)
            throws BadLocationException {
        final Optional<IRegion> cellRegion = findLiveCellRegion(document, isTsv, offset);
        if (cellRegion.isPresent()) {

            final String cellContent = document.get(cellRegion.get().getOffset(), cellRegion.get().getLength());
            return findLiveVariable(cellContent, offset - cellRegion.get().getOffset())
                    .map(reg -> new Region(reg.getOffset() + cellRegion.get().getOffset(), reg.getLength()));
        }
        return Optional.empty();
    }

    public static Optional<IRegion> findLiveVariable(final String cellContent, final int offset) {
        final Matcher matcher = Pattern.compile("[@$&%][^@$%&]*").matcher(cellContent);
        while (matcher.find()) {
            final int start = matcher.start();
            final int closingBracketIndex = cellContent.indexOf('}', start + 1);
            final int end = closingBracketIndex == -1 ? matcher.end() : Math.min(matcher.end(), closingBracketIndex);
            if (Range.closed(start, end).contains(offset)) {
                return Optional.of(new Region(start, end - start));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns region around offset which constitutes a cell in robot file table. The region
     * is surrounded with file begin or cells separator on the left and by the file end or another
     * cells separator on right.
     * Cell separator is at least 2 spaces, tabulator or newline character
     *
     * @param document
     *            Document in which cell should be find
     * @param offset
     *            Current offset at which search should start
     * @return The region describing whole cell or absent if offset is inside cell separator. If
     *         returned region
     *         is present then it is always true that:
     *         region.getOffset() <= offset <= region.getOffset() + region.getLength()
     * @throws BadLocationException
     */
    public static Optional<IRegion> findCellRegion(final IDocument document, final boolean isTsv, final int offset)
            throws BadLocationException {
        final String prev = offset > 0 ? document.get(offset - 1, 1) : "";
        final String next = offset < document.getLength() ? document.get(offset, 1) : "";
        if (prev.equals("\n") && next.equals("\n")) {
            return Optional.of(new Region(offset, 0));
        }
        if (isInsideSeparator(prev, next, isTsv)) {
            return Optional.empty();
        }

        final int beginOffset = offset - calculateCellRegionBegin(document, isTsv, offset);
        final int endOffset = offset + calculateCellRegionEnd(document, isTsv, offset);
        return Optional.of(new Region(beginOffset, endOffset - beginOffset));
    }

    /**
     * Returns region around offset which consitutues a cell during live editing. This is very
     * similar to {@link #findCellRegion(IDocument, int)} method with a single exception that
     * there can be a single space just before offset prefixed with whole cell content.
     *
     * @param document
     * @param offset
     * @return
     * @throws BadLocationException
     */
    public static Optional<IRegion> findLiveCellRegion(final IDocument document, final boolean isTsv, final int offset)
            throws BadLocationException {
        final Optional<IRegion> firstCandidate = findCellRegion(document, isTsv, offset);
        if (!firstCandidate.isPresent() && (offset > 0 ? document.get(offset - 1, 1) : "").equals("\t")) {
            return firstCandidate;
        }

        if (!firstCandidate.isPresent() && offset > 0 && document.getChar(offset - 1) == '\n') {
            return Optional.of(new Region(offset, 0));
        }
        final Optional<IRegion> region = firstCandidate.isPresent() ? firstCandidate
                : findCellRegion(document, isTsv, offset - 1);
        if (region.isPresent()) {
            final int length = Math.max(offset - region.get().getOffset(), region.get().getLength());
            return Optional.of(new Region(region.get().getOffset(), length));
        }
        return region;
    }

    private static boolean isInsideSeparator(final String prev, final String next, final Boolean isTsv) {
        if (isTsv) {
            return !prev.isEmpty() && (prev.charAt(0) == '\t' || prev.charAt(0) == '\n' || prev.charAt(0) == '\r')
                    && !next.isEmpty() && (next.charAt(0) == '\t' || next.charAt(0) == '\n' || prev.charAt(0) == '\r');
        } else {
            return !prev.isEmpty() && (Character.isWhitespace(prev.charAt(0)) || prev.charAt(0) == '|')
                    && !next.isEmpty() && (Character.isWhitespace(next.charAt(0)) || next.charAt(0) == '|');
        }
    }

    private static int calculateCellRegionBegin(final IDocument document, final boolean isTsv, final int caretOffset)
            throws BadLocationException {
        int j = 1;
        while (true) {
            if (caretOffset - j < 0) {
                break;
            }
            final char prev = document.get(caretOffset - j, 1).charAt(0);
            if (prev == '\t' || prev == '\r' || prev == '\n') {
                break;
            }

            if (caretOffset - j - 1 < 0) {
                if (prev != ' ') {
                    j++;
                }
                break;
            }
            if (prev == ' ' && !isTsv) {
                final char lookBack = document.get(caretOffset - j - 1, 1).charAt(0);
                if (Character.isWhitespace(lookBack) || lookBack == '|') {
                    break;
                }
            }
            j++;
        }
        return j - 1;
    }

    private static int calculateCellRegionEnd(final IDocument document, final boolean isTsv, final int caretOffset)
            throws BadLocationException {
        int i = 0;
        while (true) {
            if (caretOffset + i >= document.getLength()) {
                break;
            }
            final char next = document.get(caretOffset + i, 1).charAt(0);
            if (next == '\t' || next == '\r' || next == '\n') {
                break;
            }
            if (caretOffset + i + 1 >= document.getLength()) {
                if (next != ' ') {
                    i++;
                }
                break;
            }
            if (next == ' ' && !isTsv) {
                final char lookAhead = document.get(caretOffset + i + 1, 1).charAt(0);
                if (Character.isWhitespace(lookAhead) || lookAhead == '|') {
                    break;
                }
            }
            i++;
        }
        return i;
    }

    public static String lineContentBeforeCurrentPosition(final IDocument document, final int offset) {
        try {
            final IRegion lineInfo = document.getLineInformationOfOffset(offset);
            return document.get(lineInfo.getOffset(), offset - lineInfo.getOffset());
        } catch (final BadLocationException e) {
            throw new IllegalStateException("Unable to get line content at offset " + offset, e);
        }
    }

    public static boolean isInLastCellOfLine(final IDocument document, final int offset, final boolean isTsv) {
        try {
            final IRegion lineInfo = document.getLineInformationOfOffset(offset);
            return offset + calculateCellRegionEnd(document, isTsv, offset) == lineInfo.getOffset()
                    + lineInfo.getLength();
        } catch (final BadLocationException e) {
            throw new IllegalStateException("Unable to get line content at offset " + offset, e);
        }
    }

    public static String getPrefix(final IDocument document, final Optional<IRegion> region, final int offset)
            throws BadLocationException {
        if (!region.isPresent()) {
            return "";
        }
        return document.get(region.get().getOffset(), offset - region.get().getOffset());
    }

    public static int getNumberOfCellSeparators(final String lineContentBefore, final boolean isTsv) {
        return isTsv ? getNumberOfCellSeparatorsInTsv(lineContentBefore)
                : getNumberOfCellsSeparators(lineContentBefore);
    }

    private static int getNumberOfCellsSeparators(final String lineContentBefore) {
        if (lineContentBefore.isEmpty()) {
            return 0;
        }
        final String withoutTabs = lineContentBefore.replaceAll("\t", "  ")
                .replaceAll(" \\| ", "   ")
                .replaceFirst("^\\| ", "  ");

        int spacesRegions = 0;
        int currentNumberOfSpaces = 0;
        for (int i = 0; i < withoutTabs.length(); i++) {
            if (withoutTabs.charAt(i) == ' ') {
                currentNumberOfSpaces++;
            } else if (currentNumberOfSpaces == 1) {
                currentNumberOfSpaces = 0;
            } else if (currentNumberOfSpaces > 1) {
                spacesRegions++;
                currentNumberOfSpaces = 0;
            }
        }
        // maybe spaces were suffix of line content
        if (currentNumberOfSpaces > 1) {
            spacesRegions++;
        }

        return spacesRegions;
    }

    private static int getNumberOfCellSeparatorsInTsv(final String lineContentBefore) {
        int separators = 0;
        for (final char ch : lineContentBefore.toCharArray()) {
            if (ch == '\t') {
                separators++;
            }
        }
        return separators;
    }

    public static String getDelimiter(final IDocument document) {
        try {
            final String delimiter = document.getLineDelimiter(0);
            if (delimiter != null) {
                return delimiter;
            }
        } catch (final BadLocationException e) {
            // ok just get it from preferences
        }

        final IScopeContext[] context = new IScopeContext[] { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE,
                DefaultScope.INSTANCE };
        final String delimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME,
                Platform.PREF_LINE_SEPARATOR, null, context);
        return delimiter != null ? delimiter : System.lineSeparator();
    }

    public static int getLine(final IDocument document, final int offset) {
        try {
            return document.getLineOfOffset(offset);
        } catch (final BadLocationException e) {
            return -1;
        }
    }

    public static Optional<IRegion> getSnippet(final IDocument document, final int offset,
            final int noOfLinesBeforeAndAfter) {
        if (noOfLinesBeforeAndAfter < 0) {
            return Optional.empty();
        }
        try {
            final int line = document.getLineOfOffset(offset);
            final int firstLine = Math.max(0, line - noOfLinesBeforeAndAfter);
            final int lastLine = Math.min(document.getNumberOfLines() - 1, line + noOfLinesBeforeAndAfter);

            final IRegion firstLineRegion = document.getLineInformation(firstLine);
            final IRegion lastLineRegion = document.getLineInformation(lastLine);

            return Optional.of(new Region(firstLineRegion.getOffset(),
                    lastLineRegion.getOffset() + lastLineRegion.getLength() - firstLineRegion.getOffset()));

        } catch (final BadLocationException e) {
            return Optional.empty();
        }
    }
}

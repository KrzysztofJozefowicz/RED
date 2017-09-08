/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.IExecutableStepsHolder;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.TableHeader;
import org.rf.ide.core.testdata.text.read.EndOfLineBuilder.EndOfLineTypes;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.read.separators.Separator;
import org.rf.ide.core.testdata.text.write.DumpLineUpdater;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.EmptyLineDumper;
import org.rf.ide.core.testdata.text.write.SectionBuilder.Section;
import org.rf.ide.core.testdata.text.write.SectionBuilder.SectionType;

public abstract class AExecutableTableDumper implements ISectionTableDumper {

    private final DumperHelper aDumpHelper;

    private final DumpLineUpdater lineUpdater;

    private final List<IExecutableSectionElementDumper> dumpers;

    public AExecutableTableDumper(final DumperHelper aDumpHelper, final List<IExecutableSectionElementDumper> dumpers) {
        this.aDumpHelper = aDumpHelper;
        this.lineUpdater = new DumpLineUpdater(aDumpHelper);
        this.dumpers = dumpers;
    }

    protected DumperHelper getDumperHelper() {
        return this.aDumpHelper;
    }

    protected EmptyLineDumper getEmptyDumperHelper() {
        return getDumperHelper().getEmptyLineDumper();
    }

    protected DumpLineUpdater getLineDumperHelper() {
        return this.lineUpdater;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void dump(final RobotFile model, final List<Section> sections, final int sectionWithHeaderPos,
            final TableHeader<? extends ARobotSectionTable> th, final List<AModelElement<ARobotSectionTable>> sorted,
            final List<RobotLine> lines) {
        getDumperHelper().getHeaderDumpHelper().dumpHeader(model, th, lines);

        getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(th, null, model, lines);

        if (!sorted.isEmpty()) {
            final List<Section> execUnits = SectionType.filterByType(sections, sectionWithHeaderPos, getSectionType());
            final int lastIndexToDump = getDumperHelper().getLastSortedToDump(model, execUnits,
                    new ArrayList<>(sorted));

            AModelElement<?> last = null;
            for (int execUnitIndex = 0; execUnitIndex <= lastIndexToDump; execUnitIndex++) {
                addLineSeparatorIfIsRequired(model, lines);

                final AModelElement<ARobotSectionTable> execUnit = sorted.get(execUnitIndex);
                if (execUnitIndex > 0) {
                    getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(sorted.get(execUnitIndex - 1),
                            execUnit, model, lines);
                }

                @SuppressWarnings("rawtypes")
                final IExecutableStepsHolder execHolder = (IExecutableStepsHolder) execUnit;

                final RobotToken elemDeclaration = execHolder.getHolder().getDeclaration();
                final FilePosition filePosition = elemDeclaration.getFilePosition();
                int fileOffset = -1;
                if (filePosition != null && !filePosition.isNotSet()) {
                    fileOffset = filePosition.getOffset();
                }

                final RobotLine currentLine = getLineForToken(model, fileOffset);

                addSeparatorInTheBeginning(model, lines, elemDeclaration, currentLine);

                if (!elemDeclaration.isDirty() && currentLine != null) {
                    getLineDumperHelper().updateLine(model, lines, elemDeclaration);
                    addSuffixAfterTokenDeclaration(model, lines, elemDeclaration, currentLine);
                } else {
                    getLineDumperHelper().updateLine(model, lines, elemDeclaration);
                }

                final List<AModelElement<? extends IExecutableStepsHolder<?>>> sortedUnits = execHolder
                        .getElements();
                final int sortedUnitsSize = sortedUnits.size();
                for (int sortedUnitId = 0; sortedUnitId < sortedUnitsSize; sortedUnitId++) {
                    final AModelElement<? extends IExecutableStepsHolder<?>> execElement = sortedUnits
                            .get(sortedUnitId);

                    addLineSeparatorIfIsRequiredAfterExecElement(model, lines, execHolder, execElement);

                    IExecutableSectionElementDumper elemDumper = null;
                    for (final IExecutableSectionElementDumper dumper : dumpers) {
                        if (dumper.isServedType(execElement)) {
                            elemDumper = dumper;
                            break;
                        }
                    }

                    elemDumper.dump(model, sections, sectionWithHeaderPos, th, sortedUnits, execElement, lines);

                    last = execElement;
                }

                if (sortedUnits.isEmpty()) {
                    last = null;
                }
                getEmptyDumperHelper().dumpEmptyLines(model, lines, execUnit, execUnitIndex == lastIndexToDump);
            }

            if (last != null) {
                getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(last, null, model, lines);
            }

            if (lastIndexToDump == sorted.size() - 1) {
                sorted.clear();
            } else {
                for (int elemIndex = 0; elemIndex <= lastIndexToDump; elemIndex++) {
                    sorted.remove(0);
                }
            }
        }
    }

    private void addLineSeparatorIfIsRequiredAfterExecElement(final RobotFile model, final List<RobotLine> lines,
            final IExecutableStepsHolder<AModelElement<? extends ARobotSectionTable>> execHolder,
            final AModelElement<? extends IExecutableStepsHolder<?>> execElement) {
        if (!lines.isEmpty()) {
            final RobotLine lastLine = lines.get(lines.size() - 1);
            final IRobotLineElement endOfLine = lastLine.getEndOfLine();
            if ((endOfLine == null || endOfLine.getFilePosition().isNotSet()
                    || endOfLine.getTypes().contains(EndOfLineTypes.NON)
                    || endOfLine.getTypes().contains(EndOfLineTypes.EOF)) && !lastLine.getLineElements().isEmpty()) {
                final boolean shouldSeparateLine = shouldSeparateLine(execHolder, execElement);

                if (shouldSeparateLine) {
                    final IRobotLineElement lineSeparator = getDumperHelper().getLineSeparator(model);
                    getLineDumperHelper().updateLine(model, lines, lineSeparator);
                }
            }
        }
    }

    private void addSuffixAfterTokenDeclaration(final RobotFile model, final List<RobotLine> lines,
            final RobotToken elemDeclaration, final RobotLine currentLine) {
        final List<IRobotLineElement> lineElements = currentLine.getLineElements();
        final int tokenPosIndex = lineElements.indexOf(elemDeclaration);
        if (lineElements.size() - 1 > tokenPosIndex + 1) {
            for (int index = tokenPosIndex + 1; index < lineElements.size(); index++) {
                final IRobotLineElement nextElem = lineElements.get(index);
                final List<IRobotTokenType> types = nextElem.getTypes();
                if (types.contains(RobotTokenType.PRETTY_ALIGN_SPACE)
                        || types.contains(RobotTokenType.ASSIGNMENT)) {
                    getLineDumperHelper().updateLine(model, lines, nextElem);
                } else {
                    break;
                }
            }
        }
    }

    private void addSeparatorInTheBeginning(final RobotFile model, final List<RobotLine> lines,
            final RobotToken elemDeclaration, final RobotLine currentLine) {
        if (currentLine != null) {
            getDumperHelper().getSeparatorDumpHelper().dumpSeparatorsBeforeToken(model, currentLine,
                    elemDeclaration, lines);
        } else if (getDumperHelper().isSeparatorForExecutableUnitName(
                getDumperHelper().getSeparator(model, lines, elemDeclaration, elemDeclaration))) {
            if (!getDumperHelper().wasSeparatorBefore(lines)) {
                final Separator sep = getDumperHelper().getSeparator(model, lines, elemDeclaration, elemDeclaration);
                if (sep.getText().equals(" | ")) {
                    sep.setText("| ");
                    sep.setRaw("| ");
                }
                getDumperHelper().getDumpLineUpdater().updateLine(model, lines, sep);
            }
        }
    }

    private RobotLine getLineForToken(final RobotFile model, final int fileOffset) {
        RobotLine currentLine = null;
        if (fileOffset >= 0) {
            final Optional<Integer> lineIndex = model.getRobotLineIndexBy(fileOffset);
            if (lineIndex.isPresent()) {
                currentLine = model.getFileContent().get(lineIndex.get());
            }
        }
        return currentLine;
    }

    private void addLineSeparatorIfIsRequired(final RobotFile model, final List<RobotLine> lines) {
        if (!lines.isEmpty()) {
            final RobotLine lastLine = lines.get(lines.size() - 1);
            final IRobotLineElement endOfLine = lastLine.getEndOfLine();
            if ((endOfLine == null || endOfLine.getFilePosition().isNotSet()
                    || endOfLine.getTypes().contains(EndOfLineTypes.NON)
                    || endOfLine.getTypes().contains(EndOfLineTypes.EOF))
                    && !lastLine.getLineElements().isEmpty()) {
                final IRobotLineElement lineSeparator = getDumperHelper().getLineSeparator(model);
                getLineDumperHelper().updateLine(model, lines, lineSeparator);
            }
        }
    }

    private boolean shouldSeparateLine(
            final IExecutableStepsHolder<AModelElement<? extends ARobotSectionTable>> execUnit,
            final AModelElement<? extends IExecutableStepsHolder<?>> execElement) {
        boolean shouldSeparateLine = true;

        final IRobotLineElement execUnitDec = execUnit.getHolder().getDeclaration();
        if (execUnitDec.getStartOffset() >= 0) {
            final RobotFile model = execUnit.getHolder().getParent().getParent();
            final Optional<Integer> robotLineIndexBy = model.getRobotLineIndexBy(execUnitDec.getStartOffset());
            if (robotLineIndexBy.isPresent()) {
                final RobotLine lastLine = model.getFileContent().get(robotLineIndexBy.get());
                final Optional<Integer> execUnitPos = lastLine.getElementPositionInLine(execUnitDec);

                final IRobotLineElement execElemDec = execElement.getDeclaration();
                final Optional<Integer> execElemPos = lastLine.getElementPositionInLine(execElemDec);
                if (execUnitPos.isPresent() && execElemPos.isPresent()) {
                    shouldSeparateLine = false;
                }
            }
        }

        return shouldSeparateLine;
    }

    public abstract List<AModelElement<? extends IExecutableStepsHolder<?>>> getSortedUnits(
            final IExecutableStepsHolder<?> execHolder);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void revertExecutableRowToCorrectPlace(
            final List<AModelElement<? extends IExecutableStepsHolder<?>>> sortedUnits,
            final IExecutableStepsHolder<?> execHolder) {
        if (execHolder.getExecutionContext().isEmpty()) {
            return;
        }
        final int size = sortedUnits.size();
        int indexInExec = 0;
        for (int i = 0; i < size; i++) {
            final AModelElement<? extends IExecutableStepsHolder<?>> elem = sortedUnits.get(i);
            if (elem instanceof RobotExecutableRow) {
                sortedUnits.set(i, (RobotExecutableRow) execHolder.getExecutionContext().get(indexInExec++));
            }
        }
    }
}

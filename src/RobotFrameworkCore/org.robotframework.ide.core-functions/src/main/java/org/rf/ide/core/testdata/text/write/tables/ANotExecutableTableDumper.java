/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables;

import java.util.ArrayList;
import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.TableHeader;
import org.rf.ide.core.testdata.text.read.EndOfLineBuilder.EndOfLineTypes;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.EmptyLineDumper;
import org.rf.ide.core.testdata.text.write.SectionBuilder.Section;
import org.rf.ide.core.testdata.text.write.SectionBuilder.SectionType;

public abstract class ANotExecutableTableDumper implements ISectionTableDumper {

    private final DumperHelper aDumpHelper;

    private final List<ISectionElementDumper> dumpers;

    private final boolean shouldDumpHashCommentAfterHeader;

    public ANotExecutableTableDumper(final DumperHelper aDumpHelper, final List<ISectionElementDumper> dumpers,
            final boolean shouldDumpHashCommentAfterHeader) {
        this.aDumpHelper = aDumpHelper;
        this.dumpers = dumpers;
        this.shouldDumpHashCommentAfterHeader = shouldDumpHashCommentAfterHeader;
    }

    protected DumperHelper getDumperHelper() {
        return this.aDumpHelper;
    }

    protected EmptyLineDumper getEmptyDumperHelper() {
        return getDumperHelper().getEmptyLineDumper();
    }

    @Override
    public void dump(final RobotFile model, final List<Section> sections, final int sectionWithHeaderPos,
            final TableHeader<? extends ARobotSectionTable> th, final List<AModelElement<ARobotSectionTable>> sorted,
            final List<RobotLine> lines) {
        getDumperHelper().getHeaderDumpHelper().dumpHeader(model, th, lines);

        if (this.shouldDumpHashCommentAfterHeader) {
            getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(th, null, model, lines);
        }

        if (!sorted.isEmpty()) {
            final List<Section> settingSections = SectionType.filterByType(sections, sectionWithHeaderPos,
                    getSectionType());
            final int lastIndexToDump = getDumperHelper().getLastSortedToDump(model, settingSections,
                    new ArrayList<AModelElement<ARobotSectionTable>>(sorted));

            for (int settingIndex = 0; settingIndex <= lastIndexToDump; settingIndex++) {
                addLineSeparatorIfIsRequired(model, lines);

                final AModelElement<ARobotSectionTable> setting = sorted.get(settingIndex);

                ISectionElementDumper elemDumper = null;
                for (final ISectionElementDumper dumper : dumpers) {
                    if (dumper.isServedType(setting)) {
                        elemDumper = dumper;
                        break;
                    }
                }

                elemDumper.dump(model, settingSections, sectionWithHeaderPos, th, sorted, setting, lines);

                if (settingIndex < lastIndexToDump) {
                    getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(setting,
                            sorted.get(settingIndex + 1), model, lines);
                } else {
                    getDumperHelper().getHashCommentDumper().dumpHashCommentsIfTheyExists(setting, null, model, lines);
                }

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

    private void addLineSeparatorIfIsRequired(final RobotFile model, final List<RobotLine> lines) {
        if (!lines.isEmpty()) {
            final RobotLine lastLine = lines.get(lines.size() - 1);
            final IRobotLineElement endOfLine = lastLine.getEndOfLine();
            if ((endOfLine == null || endOfLine.getFilePosition().isNotSet()
                    || endOfLine.getTypes().contains(EndOfLineTypes.NON)
                    || endOfLine.getTypes().contains(EndOfLineTypes.EOF))
                    && !lastLine.getLineElements().isEmpty()) {
                final IRobotLineElement lineSeparator = getDumperHelper().getLineSeparator(model);
                getDumperHelper().getDumpLineUpdater().updateLine(model, lines, lineSeparator);
            }
        }
    }

}

/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.postfixes;

import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.TestCaseTable;
import org.rf.ide.core.testdata.model.table.exec.ExecutableUnitsFixer;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;

/**
 * @author wypych
 */
public class FixerForForContinueForItemIssue implements IPostProcessFixAction {

    private final ExecutableUnitsFixer execUnitFixer;

    public FixerForForContinueForItemIssue() {
        this.execUnitFixer = new ExecutableUnitsFixer();
    }

    /**
     * fix issue with test cases like below:
     * 
     * <pre>
    T1
    :FOR  ${x}  IN  1
    ...  2  3  4
    \    ...  ${x}
    
    T2
    :FOR  ${x}  IN  1
    ...  2  3  4
    \    Log
    \    ...  ${x}
     * </pre>
     * 
     * @param parsingOutput
     */
    @Override
    public void applyFix(final RobotFileOutput parsingOutput) {
        final RobotFile fileModel = parsingOutput.getFileModel();
        final TestCaseTable testCaseTable = fileModel.getTestCaseTable();
        final KeywordTable keywordTable = fileModel.getKeywordTable();

        if (testCaseTable.isPresent()) {
            final List<TestCase> testCases = testCaseTable.getTestCases();
            for (final TestCase execUnit : testCases) {
                final List<AModelElement<TestCase>> fixed = execUnitFixer.applyFix(execUnit);
                execUnit.removeAllElements();
                for (final AModelElement<TestCase> el : fixed) {
                    execUnit.addElement(el);
                }
            }
        }

        if (keywordTable.isPresent()) {
            final List<UserKeyword> keywords = keywordTable.getKeywords();
            for (final UserKeyword execUnit : keywords) {
                final List<AModelElement<UserKeyword>> fixed = execUnitFixer.applyFix(execUnit);
                execUnit.removeAllElements();
                for (final AModelElement<UserKeyword> el : fixed) {
                    execUnit.addElement(el);
                }
            }
        }
    }
}

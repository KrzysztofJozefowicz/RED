/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.presenter.update;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ICommentHolder;
import org.rf.ide.core.testdata.model.presenter.CommentServiceHandler;
import org.rf.ide.core.testdata.model.presenter.CommentServiceHandler.ETokenSeparator;
import org.rf.ide.core.testdata.model.presenter.update.settings.DefaultTagsModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.DocumentationModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.ForceTagsModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.LibraryImportModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.MetadataModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.ResourceImportModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.SuiteSetupModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.SuiteTeardownModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.TestSetupModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.TestTeardownModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.TestTemplateModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.TestTimeoutModelOperation;
import org.rf.ide.core.testdata.model.presenter.update.settings.VariablesImportModelOperation;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class SettingTableModelUpdater {

    private static final List<ISettingTableElementOperation> ELEMENT_OPERATIONS = newArrayList(
            new SuiteSetupModelOperation(), new SuiteTeardownModelOperation(), new TestSetupModelOperation(),
            new TestTeardownModelOperation(), new TestTemplateModelOperation(), new TestTimeoutModelOperation(),
            new ForceTagsModelOperation(), new DefaultTagsModelOperation(), new DocumentationModelOperation(),
            new LibraryImportModelOperation(), new ResourceImportModelOperation(), new VariablesImportModelOperation(),
            new MetadataModelOperation());

    public void update(final AModelElement<?> modelElement, final int index, final String value) {

        if (modelElement != null) {
            final ISettingTableElementOperation operationHandler = getOperationHandler(modelElement);
            if (operationHandler != null) {
                operationHandler.update(modelElement, index, value);
            }
        }
    }

    public void updateComment(final AModelElement<?> modelElement, final String value) {
        if (modelElement != null) {
            CommentServiceHandler.update((ICommentHolder) modelElement, ETokenSeparator.PIPE_WRAPPED_WITH_SPACE, value);
        }
    }

    public AModelElement<?> create(final ARobotSectionTable sectionTable, final int tableIndex,
            final String settingName, final String comment, final List<String> args) {

        if (sectionTable != null && sectionTable instanceof SettingTable) {
            final ISettingTableElementOperation operationHandler = getOperationHandler(settingName);
            if (operationHandler != null) {
                return operationHandler.create((SettingTable) sectionTable, tableIndex, args, comment);
            }
        }
        return null;
    }

    public void insert(final ARobotSectionTable sectionTable, final int index, final AModelElement<?> modelElement) {
        if (sectionTable != null && sectionTable instanceof SettingTable) {
            final ISettingTableElementOperation operationHandler = getOperationHandler(modelElement);
            if (operationHandler != null) {
                operationHandler.insert((SettingTable) sectionTable, index, modelElement);
            }
        }
    }

    public void remove(final ARobotSectionTable sectionTable, final AModelElement<?> modelElement) {

        if (sectionTable != null && sectionTable instanceof SettingTable) {
            final ISettingTableElementOperation operationHandler = getOperationHandler(modelElement);
            if (operationHandler != null) {
                operationHandler.remove((SettingTable) sectionTable, modelElement);
            }
        }
    }

    private ISettingTableElementOperation getOperationHandler(final String settingName) {
        return getOperationHandler(RobotTokenType.findTypeOfDeclarationForSettingTable(settingName));
    }

    private ISettingTableElementOperation getOperationHandler(final AModelElement<?> elem) {
        for (final ISettingTableElementOperation operation : ELEMENT_OPERATIONS) {
            if (operation.isApplicable(elem.getModelType())) {
                return operation;
            }
        }
        return null;
    }

    private ISettingTableElementOperation getOperationHandler(final IRobotTokenType type) {
        for (final ISettingTableElementOperation operation : ELEMENT_OPERATIONS) {
            if (operation.isApplicable(type)) {
                return operation;
            }
        }
        return null;
    }

}

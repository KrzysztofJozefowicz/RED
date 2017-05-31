/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.settings;

import static com.google.common.collect.Lists.newArrayList;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rf.ide.core.testdata.model.ModelType;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSettingsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class GeneralSettingsModel {

    private static final String DEFAULT_TAGS = "Default Tags";
    private static final String FORCE_TAGS = "Force Tags";
    private static final String TEST_TIMEOUT = "Test Timeout";
    private static final String TEST_TEMPLATE = "Test Template";
    private static final String TEST_TEARDOWN = "Test Teardown";
    private static final String TEST_SETUP = "Test Setup";
    private static final String SUITE_TEARDOWN = "Suite Teardown";
    private static final String SUITE_SETUP = "Suite Setup";
    
    static Map<String, ModelType> labelsToTypes() {
        Map<String, ModelType> returned = new HashMap<>();
        returned.put(DEFAULT_TAGS, ModelType.DEFAULT_TAGS_SETTING);
        returned.put(FORCE_TAGS, ModelType.FORCE_TAGS_SETTING);
        returned.put(TEST_TIMEOUT, ModelType.SUITE_TEST_TIMEOUT);
        returned.put(TEST_TEMPLATE, ModelType.SUITE_TEST_TEMPLATE);
        returned.put(TEST_TEARDOWN, ModelType.SUITE_TEST_TEARDOWN);
        returned.put(TEST_SETUP, ModelType.SUITE_TEST_SETUP);
        returned.put(SUITE_TEARDOWN, ModelType.SUITE_TEARDOWN);
        returned.put(SUITE_SETUP, ModelType.SUITE_SETUP);
        return returned;
    }


    public static List<RobotElement> findGeneralSettingsList(final RobotSettingsSection section) {
        if (section == null) {
            return newArrayList();
        }
        return newArrayList(
                Iterables.filter(newArrayList(fillSettingsMapping(section).values()),
                Predicates.notNull()));
    }

    public static Map<String, RobotElement> fillSettingsMapping(final RobotSettingsSection section) {
        final Map<String, RobotElement> initialMapping = AccessibleSettings.forFile(section.getSuiteFile())
                .createInitialMapping();

        final Map<String, ModelType> labels = labelsToTypes();
        if (section != null) {
            for (RobotKeywordCall setting : section.getChildren()) {
                for (String label : initialMapping.keySet()) {
                    if (labels.get(label) == setting.getLinkedElement().getModelType()) {
                         initialMapping.put(label, setting);
                         break;
                   }
                }
             }
        }
        return initialMapping;
    }

    static enum AccessibleSettings {
        OF_INIT_FILE {
            @Override
            Map<String, RobotElement> createInitialMapping() {
                final Map<String, RobotElement> settings = new LinkedHashMap<>();
                settings.put(SUITE_SETUP, null);
                settings.put(SUITE_TEARDOWN, null);
                settings.put(TEST_SETUP, null);
                settings.put(TEST_TEARDOWN, null);
                // there are no templates in __init__ files
                settings.put(TEST_TIMEOUT, null);
                settings.put(FORCE_TAGS, null);
                // there are no default tags in __init__ files
                return settings;
            }
        },
        OF_SUITE_FILE {
            @Override
            Map<String, RobotElement> createInitialMapping() {
                final Map<String, RobotElement> settings = new LinkedHashMap<>();
                settings.put(SUITE_SETUP, null);
                settings.put(SUITE_TEARDOWN, null);
                settings.put(TEST_SETUP, null);
                settings.put(TEST_TEARDOWN, null);
                settings.put(TEST_TEMPLATE, null);
                settings.put(TEST_TIMEOUT, null);
                settings.put(FORCE_TAGS, null);
                settings.put(DEFAULT_TAGS, null);
                return settings;
            }
        },
        OF_RESOURCE_FILE {
            @Override
            Map<String, RobotElement> createInitialMapping() {
                return new HashMap<>();
            }
        };

        abstract Map<String, RobotElement> createInitialMapping();

        static AccessibleSettings forFile(final RobotSuiteFile suiteModel) {
            if (suiteModel.isInitializationFile()) {
                return OF_INIT_FILE;
            } else if (suiteModel.isSuiteFile()) {
                return OF_SUITE_FILE;
            } else {
                return OF_RESOURCE_FILE;
            }
        }
    }
}

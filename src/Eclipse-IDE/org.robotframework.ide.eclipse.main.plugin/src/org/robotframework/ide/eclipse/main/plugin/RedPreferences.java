/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin;

import static com.google.common.collect.Sets.newHashSet;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement.ElementOpenMode;
import org.robotframework.ide.eclipse.main.plugin.preferences.SyntaxHighlightingCategory;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ProblemCategory;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ProblemCategory.Severity;
import org.robotframework.red.graphics.ColorsManager;

public class RedPreferences {

    private final IPreferenceStore store;

    protected RedPreferences(final IPreferenceStore store) {
        this.store = store;
    }

    public static final String OTHER_RUNTIMES = "otherRuntimes";
    public static final String OTHER_RUNTIMES_EXECS = "red.otherRuntimesExecs";
    public static final String ACTIVE_RUNTIME = "activeRuntime";
    public static final String ACTIVE_RUNTIME_EXEC = "red.activeRuntimeExec";

    public static final String FILE_ELEMENTS_OPEN_MODE = "red.editor.general.fileElementOpenMode";
    public static final String SEPARATOR_MODE = "separatorMode";
    public static final String SEPARATOR_TO_USE = "separatorToUse";
    public static final String MINIMAL_NUMBER_OF_ARGUMENT_COLUMNS = "minimalArgsColumns";
    public static final String BEHAVIOR_ON_CELL_COMMIT = "cellCommitBehavior";
    public static final String CELL_WRAPPING = "red.editor.tables.cellWrapping";

    public static final String FOLDABLE_SECTIONS = "foldableSections";
    public static final String FOLDABLE_CASES = "foldableCases";
    public static final String FOLDABLE_KEYWORDS = "foldableKeywords";
    public static final String FOLDABLE_DOCUMENTATION = "foldableDocumentation";
    public static final String FOLDING_LINE_LIMIT = "foldingLineLimit";

    public static final String ASSISTANT_AUTO_ACTIVATION_ENABLED = "assistantAutoActivationEnabled";
    public static final String ASSISTANT_AUTO_ACTIVATION_DELAY = "assistantAutoActivationDelay";
    public static final String ASSISTANT_AUTO_ACTIVATION_CHARS = "assistantAutoActivationChars";
    public static final String ASSISTANT_KEYWORD_PREFIX_AUTO_ADDITION_ENABLED = "assistantKeywordPrefixAutoAdditionEnabled";

    public static final String PROJECT_MODULES_RECURSIVE_ADDITION_ON_VIRTUALENV_ENABLED = "projectModulesRecursiveAdditionOnVirtualenvEnabled";

    public static final String SYNTAX_COLORING_PREFIX = "syntaxColoring.";

    public static final String LAUNCH_USE_ARGUMENT_FILE = "red.launch.useArgumentFile";

    public static final String LAUNCH_ADDITIONAL_INTERPRETER_ARGUMENTS = "red.launch.additionalInterpreterArguments";

    public static final String LAUNCH_ADDITIONAL_ROBOT_ARGUMENTS = "red.launch.additionalRobotArguments";

    public static final String LAUNCH_AGENT_CONNECTION_HOST = "red.launch.agentConnectionHost";

    public static final String LAUNCH_AGENT_CONNECTION_PORT = "red.launch.agentConnectionPort";

    public static final String LAUNCH_AGENT_CONNECTION_TIMEOUT = "red.launch.agentConnectionTimeout";

    public static final String LAUNCH_EXECUTABLE_FILE_PATH = "red.launch.executableFilePath";

    public static final String LAUNCH_ADDITIONAL_EXECUTABLE_FILE_ARGUMENTS = "red.launch.additionalExecutableFileArguments";

    public static final String LAUNCH_USE_SINGLE_COMMAND_LINE_ARGUMENT = "red.launch.useSingleCommandLineArgument";

    public static final String LAUNCH_USE_SINGLE_FILE_DATA_SOURCE = "red.launch.useSingleFileDataSource";


    public String getActiveRuntime() {
        return store.getString(ACTIVE_RUNTIME);
    }

    public String getActiveRuntimeExec() {
        return getStringPreferenceOrEmptyIfOnlyDependentDefined(ACTIVE_RUNTIME, ACTIVE_RUNTIME_EXEC);
    }

    public String getAllRuntimes() {
        return store.getString(OTHER_RUNTIMES);
    }

    public String getAllRuntimesExecs() {
        return getStringPreferenceOrEmptyIfOnlyDependentDefined(OTHER_RUNTIMES, OTHER_RUNTIMES_EXECS);
    }

    private String getStringPreferenceOrEmptyIfOnlyDependentDefined(final String dependentPreference,
            final String preference) {
        final IEclipsePreferences[] nodes = ((ScopedPreferenceStore) store).getPreferenceNodes(false);
        for (final IEclipsePreferences prefNode : nodes) {
            try {
                final Set<String> keys = newHashSet(prefNode.keys());
                if (keys.contains(dependentPreference) && !keys.contains(preference)) {
                    return "";
                }
            } catch (final BackingStoreException e) {
                // ok we'll return from store
            }
        }
        return store.getString(preference);
    }

    public ElementOpenMode getElementOpenMode() {
        return ElementOpenMode.valueOf(store.getString(FILE_ELEMENTS_OPEN_MODE));
    }

    public SeparatorsMode getSeparatorsMode() {
        return SeparatorsMode.valueOf(store.getString(SEPARATOR_MODE));
    }

    public String getSeparatorToUse(final boolean isTsvFile) {
        final SeparatorsMode mode = getSeparatorsMode();
        switch (mode) {
            case ALWAYS_TABS:
                return "\t";
            case ALWAYS_USER_DEFINED_SEPARATOR:
                return store.getString(SEPARATOR_TO_USE).replaceAll("t", "\t").replaceAll("s", " ");
            case FILE_TYPE_DEPENDENT:
                if (isTsvFile) {
                    return "\t";
                } else {
                    return store.getString(SEPARATOR_TO_USE).replaceAll("t", "\t").replaceAll("s", " ");
                }
            default:
                throw new IllegalStateException("Unrecognized separators mode: " + mode.toString());
        }
    }

    public int getMinimalNumberOfArgumentColumns() {
        return store.getInt(MINIMAL_NUMBER_OF_ARGUMENT_COLUMNS);
    }

    public CellCommitBehavior getCellCommitBehavior() {
        return CellCommitBehavior.valueOf(store.getString(BEHAVIOR_ON_CELL_COMMIT));
    }

    public CellWrappingStrategy getCellWrappingStrategy() {
        return CellWrappingStrategy.valueOf(store.getString(CELL_WRAPPING));
    }

    public boolean isAssistantAutoActivationEnabled() {
        return store.getBoolean(ASSISTANT_AUTO_ACTIVATION_ENABLED);
    }

    public int getAssistantAutoActivationDelay() {
        return store.getInt(ASSISTANT_AUTO_ACTIVATION_DELAY);
    }

    public char[] getAssistantAutoActivationChars() {
        return store.getString(ASSISTANT_AUTO_ACTIVATION_CHARS).toCharArray();
    }

    public boolean isAssistantKeywordPrefixAutoAdditionEnabled() {
        return store.getBoolean(ASSISTANT_KEYWORD_PREFIX_AUTO_ADDITION_ENABLED);
    }

    public boolean isProjectModulesRecursiveAdditionOnVirtualenvEnabled() {
        return store.getBoolean(PROJECT_MODULES_RECURSIVE_ADDITION_ON_VIRTUALENV_ENABLED);
    }

    public EnumSet<FoldableElements> getFoldableElements() {
        final EnumSet<FoldableElements> elements = EnumSet.noneOf(FoldableElements.class);
        if(store.getBoolean(FOLDABLE_SECTIONS)) {
            elements.add(FoldableElements.SECTIONS);
        }
        if (store.getBoolean(FOLDABLE_CASES)) {
            elements.add(FoldableElements.CASES);
        }
        if (store.getBoolean(FOLDABLE_KEYWORDS)) {
            elements.add(FoldableElements.KEYWORDS);
        }
        if (store.getBoolean(FOLDABLE_DOCUMENTATION)) {
            elements.add(FoldableElements.DOCUMENTATION);
        }
        return elements;
    }

    public int getFoldingLineLimit() {
        return store.getInt(FOLDING_LINE_LIMIT);
    }

    public boolean shouldLaunchUsingArgumentsFile() {
        return store.getBoolean(LAUNCH_USE_ARGUMENT_FILE);
    }

    public String getLaunchAdditionalInterpreterArguments() {
        return store.getString(LAUNCH_ADDITIONAL_INTERPRETER_ARGUMENTS);
    }

    public String getLaunchAdditionalRobotArguments() {
        return store.getString(LAUNCH_ADDITIONAL_ROBOT_ARGUMENTS);
    }

    public String getLaunchAgentConnectionHost() {
        return store.getString(LAUNCH_AGENT_CONNECTION_HOST);
    }

    public String getLaunchAgentConnectionPort() {
        return store.getString(LAUNCH_AGENT_CONNECTION_PORT);
    }

    public String getLaunchAgentConnectionTimeout() {
        return store.getString(LAUNCH_AGENT_CONNECTION_TIMEOUT);
    }

    public String getLaunchExecutableFilePath() {
        return store.getString(LAUNCH_EXECUTABLE_FILE_PATH);
    }

    public String getLaunchAdditionalExecutableFileArguments() {
        return store.getString(LAUNCH_ADDITIONAL_EXECUTABLE_FILE_ARGUMENTS);
    }

    public boolean shouldUseSingleCommandLineArgument() {
        return store.getBoolean(LAUNCH_USE_SINGLE_COMMAND_LINE_ARGUMENT);
    }

    public boolean shouldUseSingleFileDataSource() {
        return store.getBoolean(LAUNCH_USE_SINGLE_FILE_DATA_SOURCE);
    }

    public ColoringPreference getSyntaxColoring(final SyntaxHighlightingCategory category) {
        final int fontStyle = store.getInt(SYNTAX_COLORING_PREFIX + category.getId() + ".fontStyle");
        final int red = store.getInt(SYNTAX_COLORING_PREFIX + category.getId() + ".color.r");
        final int green = store.getInt(SYNTAX_COLORING_PREFIX + category.getId() + ".color.g");
        final int blue = store.getInt(SYNTAX_COLORING_PREFIX + category.getId() + ".color.b");
        return new ColoringPreference(new RGB(red, green, blue), fontStyle);
    }

    public Severity getProblemCategorySeverity(final ProblemCategory category) {
        return Severity.valueOf(store.getString(category.getId()));
    }

    public static class ColoringPreference {

        private final RGB color;

        private final int fontStyle;

        public ColoringPreference(final RGB color, final int fontStyle) {
            this.color = color;
            this.fontStyle = fontStyle;
        }

        public Color getColor() {
            return ColorsManager.getColor(color);
        }

        public int getFontStyle() {
            return fontStyle;
        }

        public RGB getRgb() {
            return color;
        }
    }

    public enum SeparatorsMode {
        ALWAYS_TABS,
        ALWAYS_USER_DEFINED_SEPARATOR,
        FILE_TYPE_DEPENDENT
    }

    public enum CellCommitBehavior {
        STAY_IN_SAME_CELL,
        MOVE_TO_ADJACENT_CELL
    }

    public enum CellWrappingStrategy {
        WRAP,
        SINGLE_LINE_CUT
    }

    public enum FoldableElements {
        SECTIONS,
        KEYWORDS,
        CASES,
        DOCUMENTATION
    }
}

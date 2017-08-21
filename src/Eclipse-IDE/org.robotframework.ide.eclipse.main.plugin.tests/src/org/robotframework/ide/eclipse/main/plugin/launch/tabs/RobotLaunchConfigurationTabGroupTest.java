/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.tabs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.junit.Test;

public class RobotLaunchConfigurationTabGroupTest {

    @Test
    public void fourTabsAreCreated_forRobotLaunchConfigInRunMode() {
        final RobotLaunchConfigurationTabGroup group = new RobotLaunchConfigurationTabGroup();
        group.createTabs(mock(ILaunchConfigurationDialog.class), ILaunchManager.RUN_MODE);

        final ILaunchConfigurationTab[] tabs = group.getTabs();
        assertThat(tabs.length).isEqualTo(5);
        assertThat(tabs[0]).isInstanceOf(LaunchConfigurationRobotTab.class);
        assertThat(tabs[1]).isInstanceOf(LaunchConfigurationListenerTab.class);
        assertThat(tabs[2]).isInstanceOf(LaunchConfigurationExecutorTab.class);
        assertThat(tabs[3]).isInstanceOf(EnvironmentTab.class);
        assertThat(tabs[4]).isInstanceOf(CommonTab.class);
    }

    @Test
    public void fourTabsAreCreated_forRobotLaunchConfigInDebugMode() {
        final RobotLaunchConfigurationTabGroup group = new RobotLaunchConfigurationTabGroup();
        group.createTabs(mock(ILaunchConfigurationDialog.class), ILaunchManager.DEBUG_MODE);

        final ILaunchConfigurationTab[] tabs = group.getTabs();
        assertThat(tabs.length).isEqualTo(5);
        assertThat(tabs[0]).isInstanceOf(LaunchConfigurationRobotTab.class);
        assertThat(tabs[1]).isInstanceOf(LaunchConfigurationListenerTab.class);
        assertThat(tabs[2]).isInstanceOf(LaunchConfigurationExecutorTab.class);
        assertThat(tabs[3]).isInstanceOf(EnvironmentTab.class);
        assertThat(tabs[4]).isInstanceOf(CommonTab.class);
    }

}

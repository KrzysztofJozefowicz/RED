/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.libraries;

import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URI;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.rf.ide.core.executor.EnvironmentSearchPaths;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfig;
import org.robotframework.red.junit.ProjectProvider;

@RunWith(MockitoJUnitRunner.class)
public class PythonLibStructureBuilderTest {

    @ClassRule
    public static ProjectProvider projectProvider = new ProjectProvider(PythonLibStructureBuilderTest.class);

    @Mock
    private RobotRuntimeEnvironment environment;

    private RobotProjectConfig config;

    private URI moduleLocation;

    @Before
    public void before() throws Exception {
        config = new RobotProjectConfig();
        moduleLocation = projectProvider.createFile("module.py").getLocationURI();
    }

    @Test
    public void testGettingPythonClassesFromModule() throws Exception {
        final PythonLibStructureBuilder builder = new PythonLibStructureBuilder(environment, config,
                projectProvider.getProject());

        builder.provideEntriesFromFile(moduleLocation);

        verify(environment).getClassesFromModule(new File(moduleLocation), null, new EnvironmentSearchPaths());
    }

    @Test
    public void testGettingPythonClassesFromModuleWithSpecifiedName() throws Exception {
        final PythonLibStructureBuilder builder = new PythonLibStructureBuilder(environment, config,
                projectProvider.getProject());

        builder.provideEntriesFromFile(moduleLocation, "module_name");

        verify(environment).getClassesFromModule(new File(moduleLocation), "module_name", new EnvironmentSearchPaths());
    }

    @Test
    public void testGettingPythonClassesFromModuleWithAdditionalSearchPaths() throws Exception {
        config.addPythonPath(SearchPath.create("path1"));
        config.addPythonPath(SearchPath.create("path2"));
        config.addClassPath(SearchPath.create("path3"));

        final PythonLibStructureBuilder builder = new PythonLibStructureBuilder(environment, config,
                projectProvider.getProject());

        builder.provideEntriesFromFile(moduleLocation);

        verify(environment).getClassesFromModule(new File(moduleLocation), null,
                new RedEclipseProjectConfig(config).createEnvironmentSearchPaths(projectProvider.getProject()));
    }

}

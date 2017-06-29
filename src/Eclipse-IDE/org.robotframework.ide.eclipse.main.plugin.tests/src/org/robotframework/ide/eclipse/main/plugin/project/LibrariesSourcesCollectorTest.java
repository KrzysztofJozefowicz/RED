/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.rf.ide.core.executor.EnvironmentSearchPaths;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.red.junit.ProjectProvider;

public class LibrariesSourcesCollectorTest {

    @Rule
    public ProjectProvider projectProvider = new ProjectProvider(LibrariesSourcesCollectorTest.class);

    private RobotProject project;

    @Before
    public void before() throws Exception {
        final RobotModel model = new RobotModel();
        project = model.createRobotProject(projectProvider.getProject());
    }

    @Test
    public void defaultPathsAreCollectedForProjectWithoutLibs() throws Exception {
        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".");
        assertThat(searchPaths.getPythonPaths()).isEmpty();
    }

    @Test
    public void pathsToAllFoldersWithPythonFilesAreCollected() throws Exception {
        projectProvider.createDir("a");
        projectProvider.createDir("a/b");
        projectProvider.createDir("a/b/c");
        projectProvider.createFile("lib.py");
        projectProvider.createFile("a/libA.py");
        projectProvider.createFile("a/b/libB.py");
        projectProvider.createFile("a/b/c/libC.py");

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".");
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getProject().getLocation().toOSString(),
                projectProvider.getDir("a").getLocation().toOSString(),
                projectProvider.getDir("a/b").getLocation().toOSString(),
                projectProvider.getDir("a/b/c").getLocation().toOSString());
    }

    @Test
    public void pathToFolderIsCollectedOnlyOnce() throws Exception {
        projectProvider.createFile("lib1.py");
        projectProvider.createFile("lib2.py");

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".");
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getProject().getLocation().toOSString());
    }

    @Test
    public void pathToFoldersWithPythonFilesAndPathsToJarFilesAreCollected() throws Exception {
        projectProvider.createDir("a");
        projectProvider.createFile("lib1.py");
        projectProvider.createFile("lib2.jar");
        projectProvider.createFile("a/lib3.jar");
        projectProvider.createFile("a/lib4.py");

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".",
                projectProvider.getFile("lib2.jar").getLocation().toOSString(),
                projectProvider.getFile("a/lib3.jar").getLocation().toOSString());
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getProject().getLocation().toOSString(),
                projectProvider.getDir("a").getLocation().toOSString());
    }

    @Test
    public void pathsToFoldersWithoutPythonFilesAreNotCollected() throws Exception {
        projectProvider.createDir("a");
        projectProvider.createDir("a/b");
        projectProvider.createFile("lib.js");
        projectProvider.createFile("a/lib.py");
        projectProvider.createFile("a/b/lib");

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".");
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getDir("a").getLocation().toOSString());
    }

    @Test
    public void collectedPathsAreExtendedWithPathsFromProjectConfiguration() throws Exception {
        projectProvider.createDir("python_path");
        projectProvider.createDir("java_path");
        projectProvider.createFile("lib.py");
        projectProvider.createFile("lib.jar");

        final RobotProjectConfig config = new RobotProjectConfig();
        config.addPythonPath(SearchPath.create(projectProvider.getDir("python_path").getLocation().toOSString()));
        config.addClassPath(SearchPath.create(projectProvider.getDir("java_path").getLocation().toOSString()));
        projectProvider.configure(config);

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".",
                projectProvider.getFile("lib.jar").getLocation().toOSString(),
                projectProvider.getDir("java_path").getLocation().toOSString());
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getProject().getLocation().toOSString(),
                projectProvider.getDir("python_path").getLocation().toOSString());
    }

    @Test
    public void pathsFoldersWithPythonFilesAndPathsToJarFilesAreNotCollected_whenMaxDepthExceeded() throws Exception {
        projectProvider.createDir("a");
        projectProvider.createDir("a/b");
        projectProvider.createDir("a/b/c");
        projectProvider.createFile("lib.py");
        projectProvider.createFile("lib.jar");
        projectProvider.createFile("a/libA.py");
        projectProvider.createFile("a/libA.jar");
        projectProvider.createFile("a/b/libB.py");
        projectProvider.createFile("a/b/libB.jar");
        projectProvider.createFile("a/b/c/libC.py");
        projectProvider.createFile("a/b/c/libC.jar");

        final LibrariesSourcesCollector collector = new LibrariesSourcesCollector();
        collector.collectPythonAndJavaLibrariesSources(project, 1);

        final EnvironmentSearchPaths searchPaths = collector.getEnvironmentSearchPaths();
        assertThat(searchPaths.getClassPaths()).containsOnly(".",
                projectProvider.getFile("lib.jar").getLocation().toOSString(),
                projectProvider.getFile("a/libA.jar").getLocation().toOSString());
        assertThat(searchPaths.getPythonPaths()).containsOnly(projectProvider.getProject().getLocation().toOSString(),
                projectProvider.getDir("a").getLocation().toOSString());
    }
}

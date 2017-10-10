package org.robotframework.ide.eclipse.main.plugin.refactoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ltk.core.refactoring.Change;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.RedProjectConfigEventData;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.red.junit.ProjectProvider;

public class LibraryAddChangeTest {

    @ClassRule
    public static ProjectProvider projectProvider = new ProjectProvider(LibraryAddChangeTest.class);

    @BeforeClass
    public static void beforeSuite() throws Exception {
        projectProvider.configure();
    }

    @Test
    public void checkChangeName() {
        final RobotProjectConfig config = new RobotProjectConfig();
        final ReferencedLibrary libraryToAdd = ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b");

        final LibraryAddChange change = new LibraryAddChange(projectProvider.getFile(new Path("red.xml")), config,
                libraryToAdd);

        assertThat(change.getName()).isEqualTo("The library 'c' (a/b) will be added");
        assertThat(change.getModifiedElement()).isSameAs(libraryToAdd);
    }

    @Test
    public void libraryIsAdded_whenChangeIsPerfomed() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final ReferencedLibrary libraryToAdd = ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b");

        final IEventBroker eventBroker = mock(IEventBroker.class);
        final LibraryAddChange change = new LibraryAddChange(projectProvider.getFile(new Path("red.xml")), config,
                libraryToAdd, eventBroker);

        change.initializeValidationData(null);
        assertThat(change.isValid(null).isOK()).isTrue();
        final Change undoOperation = change.perform(null);

        assertThat(undoOperation).isInstanceOf(LibraryRemoveChange.class);
        assertThat(config.getLibraries()).contains(ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b"));
        verify(eventBroker, times(1)).send(eq(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED),
                any(RedProjectConfigEventData.class));

        undoOperation.perform(null);
        assertThat(config.getLibraries()).isEmpty();
    }
}

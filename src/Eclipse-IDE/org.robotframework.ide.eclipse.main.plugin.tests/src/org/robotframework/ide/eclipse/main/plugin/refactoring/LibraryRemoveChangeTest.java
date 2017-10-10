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

public class LibraryRemoveChangeTest {

    @ClassRule
    public static ProjectProvider projectProvider = new ProjectProvider(LibraryAddChangeTest.class);

    @BeforeClass
    public static void beforeSuite() throws Exception {
        projectProvider.configure();
    }

    @Test
    public void checkChangeName() {
        final RobotProjectConfig config = new RobotProjectConfig();
        final ReferencedLibrary libraryToRemove = ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b");
        config.addReferencedLibrary(libraryToRemove);

        final LibraryRemoveChange change = new LibraryRemoveChange(projectProvider.getFile(new Path("red.xml")), config,
                libraryToRemove);

        assertThat(change.getName()).isEqualTo("The library 'c' (a/b) will be removed");
        assertThat(change.getModifiedElement()).isSameAs(libraryToRemove);
    }

    @Test
    public void libraryIsRemoved_whenChangeIsPerfomed() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final ReferencedLibrary libraryToRemove = ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b");
        config.addReferencedLibrary(libraryToRemove);

        final IEventBroker eventBroker = mock(IEventBroker.class);
        final LibraryRemoveChange change = new LibraryRemoveChange(projectProvider.getFile(new Path("red.xml")), config,
                libraryToRemove, eventBroker);

        change.initializeValidationData(null);
        assertThat(change.isValid(null).isOK()).isTrue();
        final Change undoOperation = change.perform(null);

        assertThat(undoOperation).isInstanceOf(LibraryAddChange.class);
        assertThat(config.getLibraries()).isEmpty();
        verify(eventBroker, times(1)).send(eq(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED),
                any(RedProjectConfigEventData.class));

        undoOperation.perform(null);
        assertThat(config.getLibraries()).contains(ReferencedLibrary.create(LibraryType.PYTHON, "c", "a/b"));
    }
}

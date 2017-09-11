/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.local;

import java.util.Map;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.rf.ide.core.execution.debug.UserProcessController;
import org.robotframework.ide.eclipse.main.plugin.launch.IRobotProcess;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotConsoleFacade;

public class LocalProcess extends RuntimeProcess implements IRobotProcess {

    private Runnable onDisconnectHook;

    private UserProcessController userProcessController;

    private boolean isDisconnected;

    private boolean isConnectedToTests;

    private boolean isSuspended;

    public LocalProcess(final ILaunch launch, final Process process, final String name,
            final Map<String, String> attributes) {
        super(launch, process, name, attributes);
    }

    @Override
    public void setUserProcessController(final UserProcessController controller) {
        this.userProcessController = controller;
    }

    @Override
    public UserProcessController getUserProcessController() {
        return userProcessController;
    }

    @Override
    public RobotConsoleFacade provideConsoleFacade(final String consoleDescription) {
        return RobotConsoleFacade.provide(getLaunch().getLaunchConfiguration(), consoleDescription);
    }

    @Override
    public void onTerminate(final Runnable operation) {
        this.onDisconnectHook = operation;
    }

    @Override
    public void setConnectedToTests(final boolean isConnected) {
        this.isConnectedToTests = isConnected;
    }

    @Override
    public void terminate() throws DebugException {
        if (isConnectedToTests) {
            userProcessController.terminate(() -> {});
        }
        super.terminate();
    }

    @Override
    public void terminated() {
        isConnectedToTests = false;
        isDisconnected = true;
        isSuspended = false;

        if (onDisconnectHook != null) {
            onDisconnectHook.run();
        }
        super.terminated();
    }

    @Override
    public boolean canDisconnect() {
        return isConnectedToTests && !isDisconnected;
    }

    @Override
    public boolean isDisconnected() {
        return isDisconnected;
    }

    @Override
    public void disconnect() {
        userProcessController.disconnect(this::disconnected);
        fireEvent(DebugEvent.CHANGE);
    }

    private void disconnected() {
        isConnectedToTests = false;
        isDisconnected = true;
        isSuspended = false;

        if (onDisconnectHook != null) {
            onDisconnectHook.run();
        }
        fireEvent(DebugEvent.CHANGE);
    }

    @Override
    public boolean canResume() {
        return isConnectedToTests && isSuspended;
    }

    @Override
    public void resume() {
        userProcessController.resume(this::resumed);
        fireEvent(DebugEvent.CHANGE);
    }

    @Override
    public void resumed() {
        isSuspended = false;
        fireEvent(DebugEvent.RESUME);
    }

    @Override
    public boolean canSuspend() {
        return isConnectedToTests && !isSuspended;
    }

    @Override
    public boolean isSuspended() {
        return isSuspended;
    }

    @Override
    public void suspend() {
        userProcessController.pause(this::suspended);
        fireEvent(DebugEvent.CHANGE);
    }

    @Override
    public void suspended() {
        isSuspended = true;
        fireEvent(DebugEvent.SUSPEND);
    }

    @Override
    public String getLabel() {
        return (isSuspended() ? "<supsended>" : "") + super.getLabel();
    }

    private void fireEvent(final int kind) {
        fireEvent(new DebugEvent(this, kind));
    }
}

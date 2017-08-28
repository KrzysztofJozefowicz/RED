/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.views.message;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotTestExecutionService;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotTestExecutionService.RobotTestExecutionListener;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotTestExecutionService.RobotTestsLaunch;
import org.robotframework.red.swt.SwtThread;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author mmarzec
 *
 */
public class MessageLogView {
    
    public static final String ID = "org.robotframework.ide.MessageLogView";
    
    private StyledText styledText;

    private final RobotTestExecutionService executionService;

    private final RobotTestExecutionListener executionListener = new ExecutionListener();

    private ScheduledExecutorService executor;
    
    public MessageLogView() {
        this(RedPlugin.getTestExecutionService());
    }

    @VisibleForTesting
    MessageLogView(final RobotTestExecutionService executionService) {
        this.executionService = executionService;
    }

    @VisibleForTesting
    StyledText getTextControl() {
        return styledText;
    }

    @PostConstruct
    public void postConstruct(final Composite parent) {
        final FillLayout layout = new FillLayout();
        layout.marginHeight=2;
        layout.marginWidth=2;
        parent.setLayout(layout);
        
        styledText = new StyledText(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        styledText.setFont(JFaceResources.getTextFont());
        styledText.setEditable(false);

        setInput();
    }

    private void setInput() {
        // synchronize on service, so that any thread which would like to start another launch
        // will have to wait for input loading
        synchronized (executionService) {
            executionService.addExecutionListener(executionListener);
            executionService.getLastLaunch().ifPresent(this::setInput);
        }
    }

    private void setInput(final RobotTestsLaunch launch) {
        if (executor != null) {
            executor.shutdownNow();
        }
        SwtThread.syncExec(() -> styledText.setText(""));

        // this launch may be currently running, so we have to synchronize in order
        // to get proper state of messages, as other threads may change it in the meantime
        synchronized (launch) {
            final ExecutionMessagesStore messagesStore = launch.getExecutionData(ExecutionMessagesStore.class,
                    ExecutionMessagesStore::new);
            
            if (launch.isTerminated()) {
                // since the given launch is terminated it will not change anymore
                SwtThread.syncExec(() -> setMessage(messagesStore.getMessage()));
                setMessage(messagesStore.getMessage());
            } else {
                executor = Executors.newScheduledThreadPool(1);
                final Runnable command = () -> {
                    if (messagesStore.checkDirtyAndReset()) {
                        SwtThread.asyncExec(() -> {
                            setMessage(messagesStore.getMessage());
                        });
                    }
                };
                executor.scheduleAtFixedRate(command, 0, 300, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void setMessage(final String message) {
        styledText.setRedraw(false);
        try {
            styledText.setText(message);
            styledText.setTopIndex(styledText.getLineCount() - 1);
        } finally {
            styledText.setRedraw(true);
        }
    }

    @Focus
    public void onFocus() {
        styledText.setFocus();
    }

    protected void toggleWordsWrapping() {
        styledText.setWordWrap(!styledText.getWordWrap());
    }

    @PreDestroy
    public void dispose() {
        synchronized (executionService) {
            executionService.removeExecutionListener(executionListener);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private class ExecutionListener implements RobotTestExecutionListener {

        @Override
        public void executionStarting(final RobotTestsLaunch launch) {
            setInput(launch);
        }

        @Override
        public void executionEnded(final RobotTestsLaunch launch) {
            executor.shutdown();

            // in order to be sure that there is nothing missing
            SwtThread.asyncExec(() -> {
                try {
                    executor.awaitTermination(3, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    // ok, fine
                }
                setInput(launch);
            });
        }
    }
}

package org.robotframework.ide.eclipse.main.plugin.debug.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.robotframework.ide.core.executor.RobotExecutor;
import org.robotframework.ide.eclipse.main.plugin.debug.KeywordFinder;

/**
 * @author mmarzec
 *
 */
public class RobotDebugTarget extends RobotDebugElement implements IDebugTarget {

    // associated system process (Robot)
    private IProcess process;

    // containing launch object
    private ILaunch launch;

    // program name
    private String name;

    // socket to communicate with Agent
    private Socket messageSocket;

    private PrintWriter messageWriter;

    private BufferedReader messageReader;

    private ServerSocket serverSocket;

    // suspend state
    private boolean isSuspended = false;

    // terminated state
    private boolean isTerminated = false;

    // threads
    private RobotThread thread;

    private IThread[] threads;

    // event dispatch job
    private EventDispatchJob eventDispatch;

    private ObjectMapper mapper = new ObjectMapper();

    private Map<String, Map<String, String>> currentFrames;

    private String currentKeyword = "";

    private String currentSuite = "";

    private String executedSuite = "";

    private IStackFrame[] stackFrames;

    private KeywordFinder keywordFinder = new KeywordFinder();

    private List<Integer> currentExecutionLines;

    private List<Integer> executedBreakpointsLines;

    private IProject project;

    private IEventBroker broker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);
    
    private RobotExecutor executor;
    private Path tempAgentPath;

    /**
     * Listens to events from the TestRunnerAgent and fires corresponding
     * debug events.
     */
    class EventDispatchJob extends Job {

        public EventDispatchJob() {
            super("Robot Event Dispatch");
            setSystem(true);
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        @SuppressWarnings("unchecked")
        protected IStatus run(IProgressMonitor monitor) {
            try {
                String event;
                while (!isTerminated() && (event = messageReader.readLine()) != null && messageReader != null) {

                    Map<String, Object> map = mapper.readValue(event, Map.class);

                    if (map.containsKey("pid")) {
                        broker.send("MessageLogView/Clear", "");
                        started();
                    }

                    if (map.containsKey("start_suite")) {
                        List<Object> list = (List<Object>) map.get("start_suite");
                        Map<String, String> elements = (Map<String, String>) list.get(1);
                        currentSuite = new File(elements.get("source")).getName();
                    }

                    if (map.containsKey("start_keyword")) {
                        List<Object> list = (List<Object>) map.get("start_keyword");
                        currentKeyword = (String) list.get(0);
                        Map<String, Object> elements = (Map<String, Object>) list.get(1);
                        List<String> args = (List<String>) elements.get("args");
                        
                        // TODO: check keywords in currentFrames and search keywords only after
                        // parent keywords
                        int keywordLine = keywordFinder.getKeywordLine(project, executedSuite, currentKeyword, args,
                                currentExecutionLines);
                        if (keywordLine >= 0) {
                            currentExecutionLines.add(keywordLine);
                        }

                        boolean isBreakpoint = false;
                        IBreakpoint[] currentBreakpoints = DebugPlugin.getDefault()
                                .getBreakpointManager()
                                .getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID);
                        for (int i = 0; i < currentBreakpoints.length; i++) {
                            IBreakpoint currentBreakpoint = currentBreakpoints[i];
                            String breakpointResourceName = currentBreakpoint.getMarker().getResource().getName();
                            try {
                                if (breakpointResourceName.equals(currentSuite) && currentBreakpoint.isEnabled()) {
                                    int breakpointLineNum = (Integer) currentBreakpoint.getMarker().getAttribute(
                                            IMarker.LINE_NUMBER);
                                    if (!executedBreakpointsLines.contains(breakpointLineNum)
                                            && keywordFinder.isKeywordInBreakpointLine(currentBreakpoint, breakpointLineNum,
                                                    currentKeyword, args, keywordLine)) {
                                        executedBreakpointsLines.add(breakpointLineNum);
                                        isBreakpoint = true;
                                        breakpointHit(currentBreakpoint);
                                        sendLineBreakpointEventToTextEditor(currentSuite, breakpointLineNum);
                                    }

                                }
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                        }

                        if (isBreakpoint || thread.isStepping()) {

                            if (thread.isStepping()) {
                                sendLineBreakpointEventToTextEditor(currentSuite, keywordLine);
                            }
                            stackFrames = null;
                            sendMessageToAgent("stop");
                        } else {
                            sendMessageToAgent("run");
                        }
                    }

                    if (map.containsKey("vars")) {
                        List<Object> list = (List<Object>) map.get("vars");
                        Map<String, String> mm = (Map<String, String>) list.get(1);
                        currentFrames.put(currentKeyword, mm);
                    }

                    if (map.containsKey("end_keyword")) {
                        List<Object> list = (List<Object>) map.get("end_keyword");
                        String keyword = (String) list.get(0);
                        currentFrames.remove(keyword);
                    }

                    if (map.containsKey("paused")) {
                        suspended(DebugEvent.CLIENT_REQUEST);
                    }

                    if (map.containsKey("close")) {
                        broker.send("TextEditor/ClearLineBreakpoint", 0);
                        terminated();
                    }

                    if (map.containsKey("log_message")) {
                        List<Object> list = (List<Object>) map.get("log_message");
                        Map<String, String> elements = (Map<String, String>) list.get(0);
                        String line = elements.get("timestamp") + " : " + elements.get("level") + " : "
                                + elements.get("message") + '\n';
                        broker.send("MessageLogView/AppendLine", line);
                    }
                }
            } catch (IOException e) {
                terminated();
            }
            return Status.OK_STATUS;
        }
    }

    public RobotDebugTarget(ILaunch launch, IProcess process, int requestPort, IProject project, String suite, RobotExecutor executor, Path tempAgentPath)
            throws CoreException {
        super(null);
        this.launch = launch;
        target = this;
        this.process = process;
        currentFrames = new LinkedHashMap<>();
        currentExecutionLines = new ArrayList<>();
        executedBreakpointsLines = new ArrayList<>();
        this.project = project;
        this.executedSuite = suite;
        
        this.executor = executor;
        this.tempAgentPath = tempAgentPath;

        try {
            serverSocket = new ServerSocket(54470);
            serverSocket.setReuseAddress(true);
            messageSocket = serverSocket.accept();
            messageReader = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));
            messageWriter = new PrintWriter(messageSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        thread = new RobotThread(this);
        threads = new IThread[] { thread };
        eventDispatch = new EventDispatchJob();
        eventDispatch.schedule();
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
     */
    public IProcess getProcess() {
        return process;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
     */
    public IThread[] getThreads() throws DebugException {
        return threads;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
     */
    public boolean hasThreads() throws DebugException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugTarget#getName()
     */
    public String getName() throws DebugException {
        if (name == null) {
            name = "Robot Test";

        }
        return name;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
     */
    public IDebugTarget getDebugTarget() {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
     */
    public ILaunch getLaunch() {
        return launch;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDisconnect#canDisconnect()
     */
    public boolean canDisconnect() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDisconnect#isDisconnected()
     */
    public boolean isDisconnected() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
     */
    public void disconnect() throws DebugException {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
     */
    public boolean canTerminate() {
        return getProcess().canTerminate();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
     */
    public boolean isTerminated() {
        return getProcess().isTerminated();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ITerminate#terminate()
     */
    public void terminate() throws DebugException {
        if (messageSocket != null) {
            sendMessageToAgent("interrupt");
        }
        terminated();

        broker.send("TextEditor/ClearLineBreakpoint", 0);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
     */
    public boolean canSuspend() {
        return !isTerminated() && !isSuspended();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
     */
    public boolean isSuspended() {
        return isSuspended;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
     */
    public void suspend() throws DebugException {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
     */
    public boolean canResume() {
        return !isTerminated() && isSuspended();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.ISuspendResume#resume()
     */
    public void resume() throws DebugException {
        thread.setStepping(false);
        sendMessageToAgent("resume");
        resumed(DebugEvent.CLIENT_REQUEST);

        broker.send("TextEditor/ClearLineBreakpoint", 0);
    }

    /**
     * Single step
     */
    protected void step() {
        thread.setStepping(true);
        sendMessageToAgent("resume");
        resumed(DebugEvent.CLIENT_REQUEST);
    }

    /**
     * Notification the target has resumed for the given reason
     * 
     * @param detail
     *            reason for the resume
     */
    private void resumed(int detail) {
        isSuspended = false;
        thread.fireResumeEvent(detail);
    }

    /**
     * Notification the target has suspended for the given reason
     * 
     * @param detail
     *            reason for the suspend
     */
    private void suspended(int detail) {
        isSuspended = true;
        thread.fireSuspendEvent(detail);
    }

    /**
     * Notification we have connected to the Agent and it has started.
     */
    private void started() {
        fireCreationEvent();
        installDeferredBreakpoints();
    }

    /**
     * Called when this debug target terminates.
     */
    private void terminated() {
        isTerminated = true;
        isSuspended = false;
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
        fireTerminateEvent();
        
        executor.removeTempDir(tempAgentPath.getParent());
        
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(org.eclipse.debug.core.model.
     * IBreakpoint)
     */
    public void breakpointAdded(IBreakpoint breakpoint) {

    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(org.eclipse.debug.core.model
     * .IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
     */
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (supportsBreakpoint(breakpoint)) {

        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse.debug.core.model
     * .IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
     */
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (supportsBreakpoint(breakpoint)) {
            try {
                if (breakpoint.isEnabled()) {
                    breakpointAdded(breakpoint);
                } else {
                    breakpointRemoved(breakpoint, null);
                }
            } catch (CoreException e) {
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(org.eclipse.debug.core.model
     * .IBreakpoint)
     */
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        if (breakpoint.getModelIdentifier().equals(RobotDebugElement.DEBUG_MODEL_ID)) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#supportsStorageRetrieval()
     */
    public boolean supportsStorageRetrieval() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long, long)
     */
    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
        return null;
    }

    /**
     * Install breakpoints that are already registered with the breakpoint
     * manager.
     */
    private void installDeferredBreakpoints() {
        // IBreakpoint[] breakpoints = DebugPlugin.getDefault()
        // .getBreakpointManager()
        // .getBreakpoints(RobotDebugElement.DEBUG_MODEL_ID);
        // for (int i = 0; i < breakpoints.length; i++) {
        // breakpointAdded(breakpoints[i]);
        // }
    }

    /**
     * Returns the current stack frames in the target.
     * 
     * @return the current stack frames in the target
     */
    protected IStackFrame[] getStackFrames() {

        if (stackFrames == null) {
            stackFrames = new IStackFrame[currentFrames.size()];
            int i = 1;
            for (String key : currentFrames.keySet()) {
                stackFrames[currentFrames.size() - i] = new RobotStackFrame(thread, key, currentSuite, currentFrames.get(key), i);
                i++;
            }
        }

        return stackFrames;
    }

    /**
     * Set the thread's breakpoint
     * 
     * @param breakpoint
     */
    private void breakpointHit(IBreakpoint breakpoint) {

        if (breakpoint instanceof ILineBreakpoint) {
            thread.setBreakpoints(new IBreakpoint[] { breakpoint });
        }
    }

    /**
     * Sends a message to the TestRunnerAgent
     * 
     * @param message
     */
    private void sendMessageToAgent(String message) {

        synchronized (messageSocket) {
            messageWriter.print(message);
            messageWriter.flush();
        }
    }

    /**
     * Sends a message with change variable request to the TestRunnerAgent
     * 
     * @param variable
     * @param value
     */
    public void sendChangeVariableRequest(String variable, String value) {

        synchronized (messageSocket) {
            messageWriter.print("{\"" + variable + "\":\"" + value + "\"}");
            messageWriter.flush();
        }
    }

    public void sendLineBreakpointEventToTextEditor(String file, int line) {

        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("file", file);
        eventMap.put("line", String.valueOf(line));
        broker.send("TextEditor/LineBreakpoint", eventMap);
    }
}

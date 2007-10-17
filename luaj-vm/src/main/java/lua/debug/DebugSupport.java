package lua.debug;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import lua.debug.event.DebugEvent;
import lua.debug.event.DebugEventListener;
import lua.debug.request.DebugRequest;
import lua.debug.request.DebugRequestListener;
import lua.debug.response.DebugResponse;

public class DebugSupport implements DebugRequestListener, DebugEventListener {
	protected static final int UNKNOWN = 0;
	protected static final int RUNNING = 1;
	protected static final int STOPPED = 2;

    protected DebugStackState vm;
    protected int requestPort;
    protected int eventPort;
    protected Thread requestWatcherThread;
    protected int state = UNKNOWN;
        
    protected DataInputStream requestReader;
    protected DataOutputStream requestWriter;
    protected DataOutputStream eventWriter;
        
    public DebugSupport(int requestPort, 
                        int eventPort) {
    	if (requestPort == -1) {
    		throw new IllegalArgumentException("requestPort is invalid");
    	}
    	
    	if (eventPort == -1) {
    		throw new IllegalArgumentException("eventPort is invalid");
    	}
    	
        this.requestPort = requestPort;
        this.eventPort = eventPort;
    }
        
    public void setDebugStackState(DebugStackState vm) {
        this.vm = vm;
    }
    
    protected void releaseServer() {
        DebugUtils.println("shutting down the debug server...");
        if (requestReader != null) {
            try {
                requestReader.close();
            } catch (IOException e) {}
        }
        
        if (requestWriter != null) {
            try {
                requestWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
                
        if (eventWriter != null) {
            try {
                eventWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }

    public synchronized boolean isStarted() {
    	return (state == RUNNING || state == STOPPED);
    }
    
    /* (non-Javadoc)
	 * @see lua.debug.j2se.DebugSupport#start()
	 */
    public synchronized void start() throws IOException {
    	if (this.vm == null) {
    		throw new IllegalStateException("DebugStackState is not set. Please call setDebugStackState first.");
    	}
    	
        this.requestWatcherThread = new Thread(new Runnable() {
            public void run() {
                if (getState() != STOPPED) {
                    handleRequest();
                } else {
                    releaseServer();
                }
            }
        });
        this.requestWatcherThread.start();
        this.state = RUNNING;
        
        System.out.println("LuaJ debug server is started on ports: " + requestPort + ", " + eventPort);
    }
    
    protected synchronized int getState() {
        return this.state;
    }
    
    /* (non-Javadoc)
	 * @see lua.debug.j2se.DebugSupport#stop()
	 */
    public synchronized void stop() {
    	DebugUtils.println("stopping the debug support...");
        this.state = STOPPED;
    }
    
    protected void handleRequest() {        
        try {
            while (getState() != STOPPED) {
            	int size = requestReader.readInt();
            	byte[] data = new byte[size];
            	requestReader.readFully(data);                	
                DebugRequest request 
                    = (DebugRequest) SerializationHelper.deserialize(data);
                DebugUtils.println("SERVER receives request: " + request.toString());
                
                DebugResponse response = handleRequest(request);
                data = SerializationHelper.serialize(response);
                requestWriter.writeInt(data.length);
                requestWriter.write(data);
                requestWriter.flush();
                DebugUtils.println("SERVER sends response: " + response);
            }
            
            if (getState() == STOPPED) {
                cleanup();
            }
        } catch (EOFException e) {
            cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        DebugUtils.println("SERVER terminated...");
        releaseServer();
        System.exit(0);
    }

    /**
     * This method provides the second communication channel with the debugging
     * client. The server can send events via this channel to notify the client
     * about debug events (see below) asynchronously.
     * 
     * The following events can be fired:
     * 1. started    -- the vm is started and ready to receive debugging requests
     *                  (guaranteed to be the first event sent)
     * 2. terminated -- the vm is terminated (guaranteed to be the last event sent)
     * 3. suspended client|step|breakpoint N
     *               -- the vm is suspended by client, due to a stepping request or
     *                  the breakpoint at line N is hit 
     * 4. resumed client|step
     *               -- the vm resumes execution by client or step
     *              
     * @param event
     */
    protected void sendEvent(DebugEvent event) {
        DebugUtils.println("SERVER sending event: " + event.toString());
        try {
        	byte[] data = SerializationHelper.serialize(event);
            eventWriter.writeInt(data.length);
            eventWriter.write(data);
            eventWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }         
    }

    /* (non-Javadoc)
     * @see lua.debug.DebugEventListener#notifyDebugEvent(lua.debug.DebugEvent)
     */
    /* (non-Javadoc)
	 * @see lua.debug.j2se.DebugSupport#notifyDebugEvent(lua.debug.event.DebugEvent)
	 */
    public void notifyDebugEvent(DebugEvent event) {
        sendEvent(event);        
    }

    /* (non-Javadoc)
     * @see lua.debug.DebugRequestListener#handleRequest(java.lang.String)
     */
    /* (non-Javadoc)
	 * @see lua.debug.j2se.DebugSupport#handleRequest(lua.debug.request.DebugRequest)
	 */
    public DebugResponse handleRequest(DebugRequest request) {  
    	if (DebugUtils.IS_DEBUG) {
    		DebugUtils.println("handling request: " + request.toString());
    	}
    	
		DebugResponse response = vm.handleRequest(request);
		return response;
    }
}
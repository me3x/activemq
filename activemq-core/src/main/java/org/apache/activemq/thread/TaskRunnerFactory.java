/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.thread;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Manages the thread pool for long running tasks.
 * 
 * Long running tasks are not always active but when they are active, they may
 * need a few iterations of processing for them to become idle. The manager
 * ensures that each task is processes but that no one task overtakes the
 * system.
 * 
 * This is kina like cooperative multitasking.
 * 
 * @version $Revision: 1.5 $
 */
public class TaskRunnerFactory {

    private Executor executor;
    private int maxIterationsPerRun;
    private String name;
    private int priority;
    private boolean daemon;

    public TaskRunnerFactory() {
        this("ActiveMQ Task", Thread.NORM_PRIORITY, true, 1000);
    }

    public TaskRunnerFactory(String name, int priority, boolean daemon, int maxIterationsPerRun) {
        
        this.name = name;
        this.priority = priority;
        this.daemon = daemon;
        this.maxIterationsPerRun = maxIterationsPerRun;
        
        // If your OS/JVM combination has a good thread model, you may want to avoid 
        // using a thread pool to run tasks and use a DedicatedTaskRunner instead.
        if( "true".equals(System.getProperty("org.apache.activemq.UseDedicatedTaskRunner")) ) {
            executor = null;
        } else {
            executor = createDefaultExecutor();
        }
    
    }

    public TaskRunner createTaskRunner(Task task, String name) {
        if( executor!=null ) {
            return new PooledTaskRunner(executor, task, maxIterationsPerRun);
        } else {
            return new DedicatedTaskRunner(task, name, priority, daemon);
        }
    }
    
    protected Executor createDefaultExecutor() {
        
        ThreadPoolExecutor rc = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, name);
                thread.setDaemon(daemon);
                thread.setPriority(priority);
                return thread;
            }
        });
        rc.allowCoreThreadTimeOut(true);
        return rc;
            
    }

}

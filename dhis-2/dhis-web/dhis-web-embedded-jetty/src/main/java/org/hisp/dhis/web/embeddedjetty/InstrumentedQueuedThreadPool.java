package org.hisp.dhis.web.embeddedjetty;

import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.thread.QueuedThreadPool;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;

/**
 * DHIS2 specific implementation of {@link io.micrometer.core.instrument.binder.jetty.InstrumentedQueuedThreadPool}
 * Created to implement support for more fine grained control over thread parameters
 */
public class InstrumentedQueuedThreadPool extends QueuedThreadPool {

    private final MeterRegistry registry;

    public InstrumentedQueuedThreadPool(
            MeterRegistry registry,
            int maxThreads,
            int minThreads,
            int idleTimeout,
            BlockingQueue<Runnable> queue) {
        super(maxThreads, minThreads, idleTimeout, queue);
        this.registry = registry;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
//        JettyServerThreadPoolMetrics threadPoolMetrics = new JettyServerThreadPoolMetrics(this, null);
//        threadPoolMetrics.bindTo(registry);
    }

}

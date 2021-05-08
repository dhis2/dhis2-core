package org.hisp.dhis.web.embeddedjetty;

import org.slf4j.Logger;

public final class EmbeddedJettyUncaughtExceptionHandler
{

    private EmbeddedJettyUncaughtExceptionHandler()
    {
    }

    /**
     * Returns an exception handler that exits the system. This is particularly
     * useful for the main thread, which may start up other, non-daemon threads,
     * but fail to fully initialize the application successfully.
     *
     * <p>
     * Example usage:
     * 
     * <pre>
     * public static void main(String[] args) {
     *   Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
     *   ...
     * </pre>
     *
     * <p>
     * The returned handler logs any exception as error and then shuts down the
     * process with an exit status of 1, indicating abnormal termination.
     */
    public static Thread.UncaughtExceptionHandler systemExit( Logger log )
    {
        return new Exiter( log, Runtime.getRuntime() );
    }

    static final class Exiter implements Thread.UncaughtExceptionHandler
    {

        private final Runtime runtime;

        private final Logger logger;

        Exiter( Logger log, Runtime runtime )
        {
            this.logger = log;
            this.runtime = runtime;
        }

        @Override
        public void uncaughtException( Thread t, Throwable e )
        {
            try
            {
                logger.error( String.format( "Caught an exception in %s.  Shutting down.", t ), e );
            }
            catch ( Throwable errorInLogging )
            {
                // If logging fails, e.g. due to low memory conditions, at least
                // try to log the
                // message and the cause for the failed logging to system error.
                System.err.println( e.getMessage() );
                System.err.println( errorInLogging.getMessage() );
            }
            finally
            {
                runtime.exit( 1 );
            }
        }
    }
}

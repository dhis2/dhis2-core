package org.hisp.dhis.scheduling;

public interface JobProgress
{
    JobProgress IGNORANT = new JobProgress()
    {
        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public void startingStage( String name, int workItems )
        {

        }

        @Override
        public void startingWorkItem( String name )
        {

        }

        @Override
        public void workItemDone( String summary )
        {

        }

        @Override
        public void workItemFailed( String error )
        {

        }
    };

    /**
     * @return true, if the job got cancelled and requests the processing thread
     *         to terminate, else false to continue processing the job
     */
    boolean isCancelled();

    /**
     *
     * @param name descriptive name for the stage that starts now
     * @param workItems number of work items in the stage, -1 if unknown
     */
    void startingStage( String name, int workItems );

    void startingWorkItem( String name );

    void workItemDone( String summary );

    void workItemFailed( String error );

    default void startingWorkItem( int i )
    {
        startingWorkItem( "#" + (i + 1) );
    }

    // it could be part of the job config to select if the progress should also
    // be forwarded to the notifier - this allows to keep updating the notifier
    // but also to replace notifier updates with pure in-memory progress
    // tracking state

    // the job config ID does not need to be passed around as the progress
    // instance is created for a particular job and job config so it can already
    // know the ID internally
}

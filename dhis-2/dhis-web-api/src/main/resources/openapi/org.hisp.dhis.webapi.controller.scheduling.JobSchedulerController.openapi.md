# Scheduler Queue API

## Endpoint `getSchedulerEntries`
Details the scheduler list entries.

## Endpoint `getQueueableJobs`
Details of the jobs that can be added to a queue.
Provide a name to narrow the selection to a specific existing queue.

## Endpoint `getQueueNames`
List all names of queues.

## Endpoint `getQueue`
Show CRON expression and sequence of the named queue.

## Endpoint `createQueue`
Create a new queue with CRON expression and sequence.

## Endpoint `updateQueue`
Update a queue CRON expression and/or sequence.

## Endpoint `deleteQueue`
Delete a queue by name.

## Any Endpoint `*`

### Parameter: `name`
Name of the scheduling queue

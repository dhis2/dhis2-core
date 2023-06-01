# Scheduler Queue API

## Specific endpoints

### `getSchedulerEntries`
Details the scheduler list entries.

### `getQueueableJobs`
Details of the jobs that can be added to a queue.
Provide a name to narrow the selection to a specific existing queue.

### `getQueueNames`
List all names of queues.

### `getQueue`
Show CRON expression and sequence of the named queue.

### `createQueue`
Create a new queue with CRON expression and sequence.

### `updateQueue`
Update a queue CRON expression and/or sequence.

### `deleteQueue`
Delete a queue by name.

## Common for all endpoints

### `*.parameter.name`
Name of the scheduling queue

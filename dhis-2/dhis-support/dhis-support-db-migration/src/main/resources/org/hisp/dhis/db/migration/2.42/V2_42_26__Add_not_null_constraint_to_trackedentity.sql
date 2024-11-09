-- Add not null constraint to trackedentitytypeid column in trackedentity table
ALTER TABLE trackedentity
    ALTER COLUMN trackedentitytypeid SET NOT NULL;
-- Remove the column to skip individual analytics on the program level
alter table program_attributes
    drop column if exists skipindividualanalytics;
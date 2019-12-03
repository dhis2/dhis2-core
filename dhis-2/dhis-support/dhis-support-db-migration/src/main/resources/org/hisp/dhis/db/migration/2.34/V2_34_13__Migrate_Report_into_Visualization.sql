
-- Update the column visualizationid of the public.report table
-- so it receives all the reporttableid's.
UPDATE public.report SET visualizationid = reporttableid;

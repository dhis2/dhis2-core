
-- Update the column visualizationid of the public.dashboarditem table
-- so it receives all the chartid's.
UPDATE public.dashboarditem SET visualizationid = chartid
WHERE chartid IS NOT NULL;

-- Update the column visualizationid of the public.dashboarditem table
-- so it receives all the reporttableid's.
UPDATE public.dashboarditem SET visualizationid = reporttable
WHERE reporttable IS NOT NULL;

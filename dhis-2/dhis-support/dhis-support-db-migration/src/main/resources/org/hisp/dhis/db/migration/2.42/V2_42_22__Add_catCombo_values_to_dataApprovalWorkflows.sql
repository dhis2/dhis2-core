-- DHIS2-11518: Add categoryCombo values to existing data approval workflows
-- For workflows with one or more datasets assigned, assign the catCombo from one of the datasets.
-- (All the datasets in a workflow *should* have the same catCombo but if they don't, we pick a random assigned one.)
-- If no datasets are assigned, assign the default catCombo.
--
-- Note that pre-existing workflows may have a null catCombo (if they were created
-- before the catCombo column was first added several releases ago) or they may have
-- the defaut catCombo (if they were created since the catCombo column was added).
update dataapprovalworkflow daw
set categorycomboid = coalesce(
        (select categorycomboid from dataset ds where daw.workflowid = ds.workflowid limit 1),
        (select categorycomboid from categorycombo where name = 'default' limit 1)
    )
where categorycomboid is null
   or categorycomboid = (select categorycomboid from categorycombo where name = 'default' limit 1);

alter table dataapprovalworkflow alter column categorycomboid set not null;

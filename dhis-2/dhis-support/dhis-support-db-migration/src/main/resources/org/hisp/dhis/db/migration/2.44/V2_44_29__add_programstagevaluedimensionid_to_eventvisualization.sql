-- Adds the program stage reference of the value dimension (value.programStage) to event visualizations.

alter table if exists eventvisualization add column if not exists programstagevaluedimensionid int8 null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_evisualization_programstagevaluedimensionid'
    ) then
        alter table eventvisualization
            add constraint fk_evisualization_programstagevaluedimensionid
            foreign key (programstagevaluedimensionid) references programstage (programstageid);
    end if;
end $$;

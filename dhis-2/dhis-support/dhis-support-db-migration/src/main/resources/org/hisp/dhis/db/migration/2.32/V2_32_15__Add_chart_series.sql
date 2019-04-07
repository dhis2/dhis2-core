
-- Create series table

create table public.series (
  seriesid int8 not null, 
  series varchar(255) not null, 
  axis int4 not null,
  constraint series_pkey primary key(seriesid)
);

-- Create chart series link table

create table public.chart_seriesitems (
  chartid int8 not null,
  sort_order int4 not null,
  seriesid int8 not null,
  constraint chart_seriesitems_pkey primary key(chartid, sort_order),
  constraint fk_chart_seriesitems_chartid foreign key (chartid) references chart(chartid),
  constraint fk_chart_seriesitems_seriesid foreign key (seriesid) references series(seriesid)
);

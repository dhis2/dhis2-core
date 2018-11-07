--Creating tables chart_yearlyseries if not already created
CREATE TABLE IF NOT EXISTS chart_yearlyseries (
    chartid integer NOT NULL,
    sort_order integer NOT NULL,
    yearlyseries character varying(255)
);

--Droping existing constraints in chart_yearlyseries
ALTER TABLE chart_yearlyseries 
DROP CONSTRAINT IF EXISTS chart_yearlyseries_pkey,
DROP CONSTRAINT IF EXISTS fk_yearlyseries_chartid;

--Adding constraints for chart_yearlyseries
ALTER TABLE chart_yearlyseries
ADD CONSTRAINT chart_yearlyseries_pkey PRIMARY KEY (chartid, sort_order),
ADD CONSTRAINT fk_yearlyseries_chartid FOREIGN KEY (chartid) REFERENCES chart(chartid);
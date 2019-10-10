alter table programruleaction add column if not exists evaluationtime varchar(50);
alter table programruleaction add column if not exists environments jsonb;

update programruleaction set evaluationtime = 'ALWAYS' where evaluationtime is null;
update programruleaction set environments = '["WEB", "ANDROID"]'::jsonb where environments is null;


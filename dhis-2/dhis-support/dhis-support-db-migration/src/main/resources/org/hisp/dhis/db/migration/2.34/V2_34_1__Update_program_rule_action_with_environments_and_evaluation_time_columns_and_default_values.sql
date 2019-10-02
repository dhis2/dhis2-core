alter table programruleaction add evaluationtime varchar(50);
alter table programruleaction add environments jsonb;

update programruleaction set evaluationtime = 'ALWAYS' where evaluationtime is null;
update programruleaction set environments = '["WEB", "ANDROID"]' where environments is null;


alter table if exists users
    add idtoken character varying(255);

alter table users drop column if exists restorecode;
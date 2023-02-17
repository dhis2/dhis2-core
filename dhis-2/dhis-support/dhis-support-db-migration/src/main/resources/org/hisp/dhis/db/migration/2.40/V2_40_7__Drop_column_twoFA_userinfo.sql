update userinfo set secret = null where twoFA = false;

alter table userinfo drop column if exists twoFA;
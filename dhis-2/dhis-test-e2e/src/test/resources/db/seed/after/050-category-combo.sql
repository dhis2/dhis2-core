DELETE FROM public.category WHERE uid = 'MMMu9s5a52V';

INSERT INTO public.category
(categoryid, "name", uid, code, lastupdated, created, datadimension, userid, publicaccess, datadimensiontype, lastupdatedby, translations, attributevalues, sharing, shortname, description)
VALUES(nextval('hibernate_sequence'), 'Category e2e test', 'MMMu9s5a52V', 'CAT_E2E_TEST', '2013-12-23 18:07:58.771', '2013-12-20 22:13:46.348', true, 1150127, 'rw------', 'ATTRIBUTE', NULL, '[]'::jsonb, '{}'::jsonb, '{"owner": "GOLswS44mh8", "public": "rw------", "external": false}'::jsonb, 'cet', NULL);

-- DHIS2-17496: Make sure API tokens are backward compatible
update api_token set type = 'PERSONAL_ACCESS_TOKEN_V1' where type = 'PERSONAL_ACCESS_TOKEN';
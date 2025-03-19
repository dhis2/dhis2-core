-- OAuth2 authorization table
create table if not exists oauth2_authorization
(
    id                            bigint                      not null primary key,

    uid                           character varying(11)       not null unique,
    code                          character varying(50) unique,
    created                       timestamp without time zone not null,
    lastUpdated                   timestamp without time zone not null,
    lastupdatedby                 bigint                      ,
    createdby                     bigint                      ,

    principal_name                varchar(255)                not null,
    registered_client_id          varchar(255)                not null,
    authorization_grant_type      varchar(255)                not null,

    authorized_scopes             varchar(1000),
    attributes                    text,
    state                         varchar(500),

    authorization_code_value      varchar(4000),
    authorization_code_issued_at  timestamp without time zone,
    authorization_code_expires_at timestamp without time zone,
    authorization_code_metadata   text,

    access_token_value            varchar(4000),
    access_token_issued_at        timestamp without time zone,
    access_token_expires_at       timestamp without time zone,
    access_token_metadata         text,
    access_token_type             varchar(255),
    access_token_scopes           varchar(1000),

    refresh_token_value           varchar(4000),
    refresh_token_issued_at       timestamp without time zone,
    refresh_token_expires_at      timestamp without time zone,
    refresh_token_metadata        text,

    oidc_id_token_value           varchar(4000),
    oidc_id_token_issued_at       timestamp without time zone,
    oidc_id_token_expires_at      timestamp without time zone,
    oidc_id_token_metadata        text,
    oidc_id_token_claims          text,

    user_code_value               varchar(4000),
    user_code_issued_at           timestamp without time zone,
    user_code_expires_at          timestamp without time zone,
    user_code_metadata            text,

    device_code_value             varchar(4000),
    device_code_issued_at         timestamp without time zone,
    device_code_expires_at        timestamp without time zone,
    device_code_metadata          text
);

-- OAuth2 client table
create table if not exists oauth2_client
(
    id                            bigint                      not null primary key,

    uid                           character varying(11)       not null unique,
    code                          character varying(50) unique,
    created                       timestamp without time zone not null,
    lastUpdated                   timestamp without time zone not null,
    lastupdatedby                 bigint                      ,
    createdby                     bigint                      ,

    client_id                     varchar(255)                not null unique,
    client_secret                 varchar(255),
    client_secret_expires_at      timestamp without time zone,
    client_id_issued_at           timestamp without time zone,
    client_authentication_methods varchar(1000),

    authorization_grant_types     varchar(1000),
    redirect_uris                 varchar(1000),
    post_logout_redirect_uris     varchar(1000),
    scopes                        varchar(1000),
    client_settings               text,
    token_settings                text
);

-- OAuth2 authorization consent table
create table if not exists oauth2_authorization_consent
(
    id                   bigint                      not null primary key,

    uid                  character varying(11)       not null unique,
    code                 character varying(50) unique,
    created              timestamp without time zone not null,
    lastUpdated          timestamp without time zone not null,
    lastupdatedby        bigint                      ,
    createdby            bigint                      ,

    principal_name       varchar(255)                not null,
    registered_client_id varchar(255)                not null,

    authorities          varchar(1000)
);

-- Foreign key constraints
alter table oauth2_authorization add constraint fk_oauth2_authorization_lastupdateby_userinfoid
    foreign key (lastupdatedby) references userinfo(userinfoid);
alter table oauth2_authorization add constraint fk_oauth2_authorization_createdby_userinfoid
    foreign key (createdby) references userinfo(userinfoid);

alter table oauth2_authorization_consent add constraint fk_oauth2_authorization_consent_lastupdateby_userinfoid
    foreign key (lastupdatedby) references userinfo(userinfoid);
alter table oauth2_authorization_consent add constraint fk_oauth2_authorization_consent_createdby_userinfoid
    foreign key (createdby) references userinfo(userinfoid);

alter table oauth2_client add constraint fk_oauth2_client_lastupdateby_userinfoid
    foreign key (lastupdatedby) references userinfo(userinfoid);
alter table oauth2_client add constraint fk_oauth2_client_createdby_userinfoid
    foreign key (createdby) references userinfo(userinfoid);

-- Create indexes for commonly queried fields
create index if not exists oauth2_client_client_id_idx on oauth2_client (client_id);
create index if not exists oauth2_authorization_registered_client_id_idx on oauth2_authorization (registered_client_id);
create index if not exists oauth2_authorization_state_idx on oauth2_authorization (state);
create index if not exists oauth2_authorization_auth_code_value_idx on oauth2_authorization (authorization_code_value);
create index if not exists oauth2_authorization_access_token_value_idx on oauth2_authorization (access_token_value);
create index if not exists oauth2_authorization_refresh_token_value_idx on oauth2_authorization (refresh_token_value);
create index if not exists oauth2_authorization_user_code_value_idx on oauth2_authorization (user_code_value);
create index if not exists oauth2_authorization_device_code_value_idx on oauth2_authorization (device_code_value);
create index if not exists oauth2_authorization_consent_client_principal_idx on oauth2_authorization_consent (registered_client_id, principal_name);
CREATE TABLE IF NOT EXISTS expression_dimensionitem
(
    expressionid bigint NOT NULL,
    description character varying(255),
    expression text,
    missingvaluestrategy character varying(255) NOT NULL,
    slidingwindow boolean,
    translations jsonb DEFAULT '[]'::jsonb,
    CONSTRAINT expression_dimensionitem_pkey PRIMARY KEY (expressionid)
);

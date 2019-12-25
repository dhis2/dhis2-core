ALTER TABLE validationrule DROP CONSTRAINT IF EXISTS unique_right_expression;
ALTER TABLE validationrule DROP CONSTRAINT IF EXISTS unique_left_expression;
ALTER TABLE validationrule ADD CONSTRAINT unique_right_expression UNIQUE (rightexpressionid);
ALTER TABLE validationrule ADD CONSTRAINT unique_left_expression UNIQUE (leftexpressionid);
ALTER TABLE validationrule ALTER COLUMN rightexpressionid DROP NOT NULL;
ALTER TABLE validationrule ALTER COLUMN leftexpressionid DROP NOT NULL;

ALTER TABLE predictor DROP CONSTRAINT IF EXISTS unique_generator_expression;
ALTER TABLE predictor DROP CONSTRAINT IF EXISTS unique_skip_test_expression;
ALTER TABLE predictor ADD CONSTRAINT unique_generator_expression UNIQUE (generatorexpressionid);
ALTER TABLE predictor ADD CONSTRAINT unique_skip_test_expression UNIQUE (skiptestexpressionid);
ALTER TABLE predictor ALTER COLUMN generatorexpressionid DROP NOT NULL;
ALTER TABLE predictor ALTER COLUMN skiptestexpressionid DROP NOT NULL;
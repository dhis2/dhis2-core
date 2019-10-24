-- Fix case in predictor exprsesions, including the following:
--
-- 1. Before 2.34, predictor expressions were parsed using regular expressions,
-- with case-insensitive matching for function names. As of 2.34, predictor
-- expression functions are parsed using ANTLR and are case sensitive (as with
-- indicators, program indicators, and validation rules.)
--
-- 2. Before 2.34, predictor functions could have white space between the
-- function name and the opening parenthesis for the arguments. This is no
-- longer allowed, so any such white space is removed.
--
-- 3. Before 2.34, the "stddev" predictor function returned the population
-- standard deviation, unlike with program indicators and postgres in which
-- the sample standard deviation is returned. With 2.34 it uses the sample
-- standard deviation. Any previous references to 'stddev(' are changed to
-- 'stddevPop(', so that predictors will work as they did before.

update expression set expression =
regexp_replace(
	regexp_replace(
		regexp_replace(
			regexp_replace(
				regexp_replace(
					regexp_replace(
						regexp_replace(
							regexp_replace(
								regexp_replace(
									regexp_replace(
										regexp_replace(
											regexp_replace(
												regexp_replace(
													expression,
													'avg\s*\(', 'avg(', 'i'),
												'count\s*\(', 'count(', 'i'),
											'max\s*\(', 'max(', 'i'),
										'median\s*\(', 'median(', 'i'),
									'min\s*\(', 'min(', 'i'),
								'percentileCont\s*\(', 'percentileCont(', 'i'),
							'stddev\s*\(', 'stddev(', 'i'),
						'stddev\(', 'stddevPop(', 'i'),
					'stddevPop\s*\(', 'stddevPop(', 'i'),
				'stddevSamp\s*\(', 'stddevSamp(', 'i'),
			'sum\s*\(', 'sum(', 'i'),
		'if\s*\(', 'if(', 'i'),
	'isNull\s*\(', 'isNull(', 'i')
where expressionid in (select generatorexpressionid from predictor union select skiptestexpressionid from predictor)

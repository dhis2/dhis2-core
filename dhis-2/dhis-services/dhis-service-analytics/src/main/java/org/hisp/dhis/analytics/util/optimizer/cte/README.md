# SQL Transformation Process: Subquery to CTE Conversion

## Technical Documentation

### Overview
This document describes the process of transforming SQL queries containing correlated subqueries into optimized forms using Common Table Expressions (CTEs). The transformation specifically addresses limitations in database engines like Doris that do not support correlation with outer layers of parent queries.

### Transformation Pipeline

The pipeline is orchestrated by the `CteOptimizationPipeline` component, which is also the entry point for the transformation process.

#### 1. Query Identification
The pipeline identifies qualifying queries by:
- Presence of CTEs prefixed with "pi_"
- Existence of correlated subqueries in WHERE clauses
- References to the correlation alias 'subax'

The component that identifies subqueries is `CteSubqueryIdentifier`.

#### 2. Query Decomposition
The transformation process decomposes complex queries into Common Table Expressions (CTE):
```sql
Original Pattern:
SELECT col FROM table WHERE col = (SELECT x FROM y WHERE y.id = outer.id)

Transformed Pattern:
WITH extracted_cte AS (
	SELECT id, x FROM y
)
SELECT col FROM table 
LEFT JOIN extracted_cte ON extracted_cte.id = table.id
```

#### 3. Expression Transformation
The core transformation occurs in three key areas:

##### a. Subquery Extraction
- Identifies correlated subqueries within WHERE clauses
- Converts them to window functions using `row_number()`
- Creates dedicated CTEs for each extracted subquery

##### b. Join Construction
- Replaces correlated references with LEFT JOINs
- Maintains original correlation conditions in JOIN clauses
- Preserves NULL handling semantics

##### c. WHERE Clause Reconstruction
- Rebuilds WHERE conditions using CTE references
- Maintains operator precedence
- Preserves original expression semantics

### ExpressionTransformer Deep Dive

The `ExpressionTransformer` class handles the complex task of transforming SQL expressions while preserving their logical structure.

#### Key Components

1. **Operator Precedence Management**
```java
@Override
public void visit(OrExpression expr) {
	// Transform left and right expressions
	// Ensure proper parenthesization
	currentTransformedExpression = new OrExpression(
		new Parenthesis(transformedLeft),
		new Parenthesis(transformedRight)
	);
}
```

2. **CASE Expression Handling**
```java
@Override
public void visit(CaseExpression caseExpr) {
	// Preserve CASE structure
	// Transform WHEN/THEN conditions
	// Maintain nested CASE expressions
}
```

3. **CAST Expression Processing**
```java
@Override
public void visit(CastExpression cast) {
	// Transform inner expression
	// Preserve CAST type information
	// Maintain date arithmetic operations
}
```

#### Expression Transformation Rules

1. **Parentheses Management**
    - Double-wrap AND expressions within OR conditions
    - Preserve explicit parentheses from original query
    - Add parentheses for operator precedence clarity

2. **Type Preservation**
    - Maintain CAST operations and their types
    - Preserve date arithmetic operations
    - Handle numeric type conversions

3. **Logical Structure**
    - Preserve AND/OR relationship hierarchy
    - Maintain CASE expression branching logic
    - Handle nested expressions recursively

### Example Transformation

```sql
-- Original Query
WHERE (cast('2020-06-01' as date) - cast(
	(SELECT created FROM events 
	 WHERE events.id = outer.id) as date)) > 1
	AND (complex_expression)
	OR (other_condition)

-- Transformed Query
WITH extracted_dates AS (
	SELECT id, created 
	FROM (SELECT id, created,
		  row_number() OVER (PARTITION BY id 
							ORDER BY date DESC) as rn
		  FROM events) t
	WHERE rn = 1
)
WHERE ((CAST('2020-06-01' AS date) - 
		CAST(extracted_dates.created AS date)) > 1
	   AND (complex_expression))
	OR ((other_condition))
```

### Visitor Pattern Implementation in SQL Transformation

The ExpressionParser utilizes the Visitor design pattern through JSQLParser's infrastructure to traverse and transform SQL expression trees. This section details the technical implementation and mechanics of the parser.

#### Visitor Pattern Implementation

##### Core Mechanism
```java
public class ExpressionTransformer extends ExpressionVisitorAdapter {
	private Expression currentTransformedExpression;
	private final SubSelectTransformer subSelectTransformer;
	
	@Override
	public void visit(OrExpression expr) {
		// Transform expression
	}
}
```
The transformer extends `ExpressionVisitorAdapter`, which provides a default implementation of the `ExpressionVisitor` interface. Each `visit` method represents a node type in the expression tree.

##### Expression Tree Traversal

1. **Entry Point**
```java
expression.accept(expressionTransformer);
```
When an expression "accepts" a visitor, it triggers a depth-first traversal of the expression tree.

2. **Visit Chain**
```java
@Override
public void visit(OrExpression expr) {
	// Visit left expression
	expr.getLeftExpression().accept(this);
	Expression transformedLeft = currentTransformedExpression;
	
	// Visit right expression
	expr.getRightExpression().accept(this);
	Expression transformedRight = currentTransformedExpression;
	
	// Construct transformed expression
	currentTransformedExpression = new OrExpression(transformedLeft, transformedRight);
}
```
Each visit method:
- Receives the current expression node
- Processes its components recursively
- Stores intermediate results in `currentTransformedExpression`
- Constructs a new transformed expression

#### Expression Types and Their Handling

##### Binary Expressions
```java
// Logical operators
visit(AndExpression)
visit(OrExpression)

// Comparison operators
visit(EqualsTo)
visit(GreaterThan)
visit(MinorThan)

// Arithmetic operators
visit(Addition)
visit(Subtraction)
```
Binary expressions follow a common pattern:
1. Transform left operand
2. Transform right operand
3. Combine with appropriate operator

##### Complex Expressions

##### CASE Expressions
```java
@Override
public void visit(CaseExpression caseExpr) {
	// Transform WHEN conditions
	for (WhenClause whenClause : caseExpr.getWhenClauses()) {
		whenClause.getWhenExpression().accept(this);
		whenClause.getThenExpression().accept(this);
	}
	
	// Transform ELSE expression
	if (caseExpr.getElseExpression() != null) {
		caseExpr.getElseExpression().accept(this);
	}
}
```

##### Parenthesized Expressions
```java
@Override
public void visit(Parenthesis parenthesis) {
	// Visit inner expression
	parenthesis.getExpression().accept(this);
	Expression transformed = currentTransformedExpression;
	
	// Maintain parentheses in output
	currentTransformedExpression = new Parenthesis(transformed);
}
```

#### State Management

##### Transformation State
```java
private Expression currentTransformedExpression;
private final Map<SubSelect, FoundSubSelect> extractedSubSelects;
```
The transformer maintains:
- Current transformed expression state
- Mapping of extracted subqueries
- Transformation context

#### Expression Building
```java
// Example of building a transformed expression
private Expression buildTransformedExpression(Expression original) {
	original.accept(this);
	return currentTransformedExpression;
}
```

#### Visitor Chain Example

For the expression `(A AND B) OR C`:

1. **Visit Sequence**:
```
OrExpression
  ├─ Parenthesis
  │   └─ AndExpression
  │       ├─ Column(A)
  │       └─ Column(B)
  └─ Column(C)
```

2. **Transformation Flow**:
```java
visit(OrExpression) {
	visit(Parenthesis) {
		visit(AndExpression) {
			visit(Column A)
			visit(Column B)
			return new AndExpression(A, B)
		}
		return new Parenthesis(AND_result)
	}
	visit(Column C)
	return new OrExpression(PAREN_result, C)
}
```

#### Error Handling and Edge Cases

The transformer includes handling of:
- Nested subqueries
- Complex CASE expressions
- Date arithmetic operations
- NULL value semantics
- Type casting preservation

### Performance Considerations

The transformation process:
- Reduces repeated subquery execution
- Optimizes for JOIN operations
- Maintains index usage potential
- Preserves query plan optimization opportunities


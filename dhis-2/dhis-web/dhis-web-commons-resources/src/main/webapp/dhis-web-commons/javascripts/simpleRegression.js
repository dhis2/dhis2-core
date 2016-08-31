
/**
 * Partial js port of commons math SimpleRegression class.
 */
function SimpleRegression()
{
	var sumX = 0; // Sum of x values
	var sumY = 0; // Sum of y values
	var sumXX = 0; // Total variation in x
	var sumXY = 0; // Sum of products
	var n = 0; // Number of observations
	var xbar = 0; // Mean of accumulated x values, used in updating formulas
	var ybar = 0; // Mean of accumulated y values, used in updating formulas
	
	this.addData = function( x, y )
	{
		if ( n == 0 )
		{
			xbar = x;
			ybar = y;
		}
		else
		{
			var dx = x - xbar;
			var dy = y - ybar;
			sumXX += dx * dx * n / ( n + 1 );
			sumXY += dx * dy * n / ( n + 1 );
			xbar += dx / ( n + 1 );
			ybar += dy / ( n + 1 );
		}
		
		sumX += x;
		sumY += y;
		n++;
	};
	
	this.predict = function( x )
	{
		var b1 = this.getSlope();
		
		return this.getIntercept( b1 ) + b1 * x;
	};
	
	this.getSlope = function()
	{
		if ( n < 2 )
		{
			return Number.NaN;
		}
		
		return sumXY / sumXX;
	};
	
	this.getIntercept = function( slope )
	{
		return ( sumY - slope * sumX ) / n;
	};
}

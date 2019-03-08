package org.opencadc.tap.impl;

import java.util.ArrayList;
import java.util.List;

import ca.nrc.cadc.tap.parser.navigator.ExpressionNavigator;
import ca.nrc.cadc.tap.parser.navigator.FromItemNavigator;
import ca.nrc.cadc.tap.parser.navigator.ReferenceNavigator;
import ca.nrc.cadc.tap.parser.RegionFinder;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import org.apache.log4j.Logger;

import org.opencadc.tap.impl.QServCircle;
import org.opencadc.tap.impl.QServPoint;

/**
 * This class implements the rewriting of all ADQL geometry constructs
 * as QServ specific geometry functions.  This extends the RegionFinder,
 * which by default for functions not overridden in this class throws
 * UnsupportedOperationException.
 *
 * @author cbanek
 */

public class QServRegionConverter extends RegionFinder
{
    private static Logger log = Logger.getLogger(QServRegionConverter.class);

    public QServRegionConverter(ExpressionNavigator en, ReferenceNavigator rn, FromItemNavigator fn)
    {
        super(en, rn, fn);
    }

    /**
     * This method is called when a REGION PREDICATE function is
     * one of the arguments in a binary expression, and after the
     * direct function conversion (like handleCircle, etc).
     *
     */
    @Override
    protected Expression handleRegionPredicate(BinaryExpression biExpr)
    {
        log.debug("handleRegionPredicate(" + biExpr.getClass().getSimpleName() + "): " + biExpr);
        return biExpr;
    }

    /**
     * This function is called to parse out a CONTAINS, and the expressions are
     * already parsed arguments.
     */
    @Override
    protected Expression handleContains(Expression left, Expression right)
    {
        if(!(left instanceof QServPoint)) {
            throw new UnsupportedOperationException("CONTAINS first argument must be a point");

        }

        if(!(right instanceof QServRegion)) {
            throw new UnsupportedOperationException("CONTAINS second argument must be a region");
        }

        QServPoint point = (QServPoint)left;
        QServRegion region = (QServRegion)right;
        return region.pointInRegion(point);
    }

    @Override
    protected Expression handlePoint(Expression coordsys, Expression ra, Expression dec)
    {
        return new QServPoint(coordsys, ra, dec);
    }

    /**
     * This method is called when a CIRCLE geometry value is found.
     *
     * From CIRCLE(coordinate_system, ra, dec, radius)
     * To qserv_areaspec_circle(long [deg], lat [deg], radius [deg])
     *
     */
    @Override
    protected Expression handleCircle(Expression coordsys, Expression ra, Expression dec, Expression radius)
    {
        return new QServCircle(coordsys, ra, dec, radius);
    }

    /**
     * This method is called whenever BOX geometry is found.
     * We don't support this.
     */
    @Override
    protected Expression handleBox(Function adqlFunction)
    {
        throw new UnsupportedOperationException("ADQL BOX is not supported.  You might be able to use qserv_areaspec_box and scisql_s2PtInBox.");
    }

    /**
     * This method is called when a POLYGON geometry value is found.
     **/
    @Override
    protected Expression handlePolygon(List<Expression> expressions)
    {
        return new QServPolygon(expressions);
    }

    /**
     * This method is called when a DISTANCE function is found.
     **/
    @Override
    protected Expression handleDistance(Expression left, Expression right)
    {
        if(!(left instanceof QServPoint)) {
            throw new UnsupportedOperationException("DISTANCE first argument must be a point");

        }

        if(!(right instanceof QServPoint)) {
            throw new UnsupportedOperationException("DISTANCE second argument must be a point");
        }

        QServPoint p1 = (QServPoint)left;
        QServPoint p2 = (QServPoint)right;

        List<Expression> params = new ArrayList<Expression>();
        params.add(p1.getRA());
        params.add(p1.getDec());
        params.add(p2.getRA());
        params.add(p2.getDec());

        Function distanceFunction = new Function();
        distanceFunction.setName("scisql_angSep");
        distanceFunction.setParameters(new ExpressionList(params));
        return distanceFunction;
    }
}

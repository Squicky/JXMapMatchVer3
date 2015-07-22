/*
 * This class provides methods
 * to get cartesian coordinates from geographic coordinates (lat, lon)
 * to get euclidean distance between two cartesian coordinates
 * to get a point on a line segment with shortest distance to any c. coordinate
 * 
 */

package cartesian;

import myOSM.myOSMNode;
import myOSM.myOSMWayPart;
import org.jdesktop.swingx.*;
import org.jdesktop.swingx.mapviewer.*;
import algorithm.MatchedPoint;
import gps.GPSNode;
import java.awt.geom.Point2D;

/**
 * @author Tob
 * @author Daniel Sathees Elmo
 */
public class Coordinates {

	// use static JXMapViewer instance to convert geographic coordinates
	private static JXMapViewer MapViewer = new JXMapViewer();

	/**
	 * set JXMapViewer for converting coordinates
	 * 
	 * @param JXMapViewer
	 *            map
	 */
	public static void setMapViewer(JXMapViewer map) {
		MapViewer = map;
	}

	// Plate carrée projection
	/**
	 *
	 * @param lon
	 * @param lat
	 * @return cartesian X coordinate (int) public int getCartesianX(double lon,
	 *         double lat){ return (int)(lon/((double)180)*40000000); }
	 */

	/**
	 *
	 * @param lon
	 * @param lat
	 * @return cartesian Y coordinate (int) public int getCartesianY(double lon,
	 *         double lat){ return (int)(lat/((double)90)*20000000); }
	 */

	// /////////////////////////////////////////// Convert GEO / PIXEL
	// ///////////////////////////////////////////////

	// Mercator projection (used by JXMapView)

	/**
	 * get converted x y value of a geographical position according zoom level 1
	 * 
	 * @param lon
	 * @param lat
	 * @return cartesian Y coordinate (int)
	 */
	public static Point2D getCartesianXY(double lon, double lat) {
		// use JXMapViewer to convert geographic coordinates to cartesian
		// geoToPixel(geoPosition(lat,lon),MapViewer.getZoom()) use Zoom=1
		Point2D point = getCartesianXY(lon, lat, 1);
		return point;
	}

	/**
	 * get converted y value of a geographical position according to given zoom
	 * level
	 * 
	 * @param lon
	 * @param lat
	 * @param zoom
	 * @return cartesian Y coordinate (int)
	 */
	public static Point2D getCartesianXY(double lon, double lat, int zoom) {
		// use JXMapViewer to convert geographic coordinates to cartesian
		Point2D point = MapViewer.getTileFactory().geoToPixel(
				new GeoPosition(lat, lon), zoom);
		return point;
	}

	/**
	 * converts x, y pixel coordinates back to longitude and latitude x, y pixel
	 * values must be normalized to zoom factor 1
	 * 
	 * @param x
	 * @param y
	 * @return GeoPosition
	 */
	public static GeoPosition getGeoPos(double x, double y) {
		// Point2D point2d = new Point2D.Double(x,y);
		return getGeoPos(x, y, 1);
	}

	/**
	 * converts x, y pixel coordinates back to longitude and latitude
	 * considering zoom
	 * 
	 * @param x
	 * @param y
	 * @return GeoPosition
	 */
	public static GeoPosition getGeoPos(double x, double y, int zoom) {
		// Point2D point2d = new Point2D.Double(x,y);
		return MapViewer.getTileFactory().pixelToGeo(new Point2D.Double(x, y),
				zoom);
	}

	/**
	 * returns middle position of given boundary
	 * 
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 * @return
	 */
	public static GeoPosition getMiddleGeoPos(double minX, double minY,
			double maxX, double maxY) {
		double midX = (double) (minX + ((maxX - minX) / (double) 2));
		double midY = (double) (minY + ((maxY - minY) / (double) 2));
		// return middle position via GeoPosition
		return Coordinates.getGeoPos(midX, midY);
	}

	/**
	 * convert geographical position to pixel coordinates and store as Point2D
	 * 
	 * @param lon
	 * @param lat
	 * @return cartesian coordinate (Point2D)
	 */
	public static Point2D getCartesian(double lon, double lat) {
		// use JXMapViewer to convert geographic coordinates to cartesian
		// geoToPixel(geoPosition(lat,lon),MapViewer.getZoom()) use Zoom=1
		Point2D point = MapViewer.getTileFactory().geoToPixel(
				new GeoPosition(lat, lon), 1);
		return point;
	}

	/**
	 * convert geographical position to pixel coordinates and store as Point2D
	 * considering zoom
	 * 
	 * @param lon
	 * @param lat
	 * @param zoom
	 * @return cartesian coordinate (Point2D)
	 */
	public static Point2D getCartesian(double lon, double lat, int zoom) {
		// use JXMapViewer to convert geographic coordinates to cartesian
		Point2D point = MapViewer.getTileFactory().geoToPixel(
				new GeoPosition(lat, lon), zoom);
		return point;
	}

	// /////////////////////////////////////////// Distances / matched Points
	// ////////////////////////////////////////

	/**
	 * get squared euclidean distance between (x1,y1) and (x2,y2)
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return double
	 */
	public static double getDistanceSquared(double x1, double y1, double x2,
			double y2) {
		return Math.abs(((double) (x2 - x1) * (double) (x2 - x1))
				+ ((double) (y2 - y1) * (double) (y2 - y1)));
	}

	/*
	 * public static double getDistanceSquared(double x1, double y1, double x2,
	 * double y2){ return getDistanceSquared((double) x1, (double) y1, x2, y2);
	 * }
	 */

	/**
	 * get euclidean distance between (x1,y1) and (x2,y2)
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return double
	 */
	public static double getDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(getDistanceSquared(x1, y1, x2, y2));
	}

	/**
	 * gets nearest matched GPS point x-position onto street link
	 * 
	 * @param px
	 * @param py
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @return X coordinate on line (from (ax,ay) to (bx,by)) with shortest
	 *         distance to point (px,py)
	 */
	public static double getNearestPointX(double px, double py, double ax,
			double ay, double bx, double by) {

		if (ax == bx && ay == by) {
			return ax;
		}

		// berechne m:
		// m ist Anteil der gesamten Strecke (ax,ay) nach (bx,by), so dass die
		// kürzeste
		// Distanz zu Punkt (px,py) erreicht ist
		double m = (double) ((bx - ax) * (px - ax) + (by - ay) * (py - ay))
				/ ((bx - ax) * (bx - ax) + (by - ay) * (by - ay));

		// Wenn m außerhalb der Strecke liegt
		// pruefe, welcher Endpunkt (ax,ay) oder (bx,by) naeher an Punkt (px,py)
		// liegt
		if ((m > 1) || (m < 0)) {
			if (getDistanceSquared(px, py, ax, ay) < getDistanceSquared(px, py,
					bx, by)) {
				return ax;
			} else {
				return bx;
			}
		}
		// gib nur X koordinate aus
		double d = (double) (ax + m * (bx - ax));
		return d;
	}

	/*
	 * public static double getNearestPointX(int px, int py, int ax, int ay, int
	 * bx, int by){ return getNearestPointX((double) px, (double) py, (double)
	 * ax, (double) ay, (double) bx, (double) by); }
	 */

	/**
	 * gets nearest matched GPS point y-position onto street link
	 * 
	 * @param px
	 * @param py
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @return Y coordinate on line (from (ax,ay) to (bx,by)) with shortest
	 *         distance to point (px,py)
	 */
	public static double getNearestPointY(double px, double py, double ax,
			double ay, double bx, double by) {

		if (ax == bx && ay == by) {
			return ay;
		}

		// berechne m:
		// m ist Anteil der gesamten Strecke (ax,ay) nach (bx,by), sodass die
		// kürzeste
		// Distanz zu Punkt (px,py) erreicht ist
		double m = (double) ((bx - ax) * (px - ax) + (by - ay) * (py - ay))
				/ ((bx - ax) * (bx - ax) + (by - ay) * (by - ay));

		// Wenn m außerhalb der Strecke liegt
		// pruefe, welcher Endpunkt (ax,ay) oder (bx,by) naeher an Punkt (px,py)
		// liegt
		if ((m > 1) || (m < 0)) {
			if (getDistanceSquared(px, py, ax, ay) < getDistanceSquared(px, py,
					bx, by))
				return ay;
			else
				return by;
		}
		// gib nur Y koordinate aus
		double d = (double) (ay + m * (by - ay));
		return d;
	}

	/*
	 * public static double getNearestPointY(int px, int py, int ax, int ay, int
	 * bx, int by){ return getNearestPointY((double)px, (double)py, (double)ax,
	 * (double)ay, (double)bx, (double)by); }
	 */

	public static double getPercentOfPointInWayPart(double matchedX,
			double matchedY, double ax, double ay, double bx, double by) {

		double lenghtWayPart;

		double lenghtWayPart2 = ((bx - ax) * (bx - ax))
				+ ((by - ay) * (by - ay));

		double d1 = (bx - ax);
		d1 = d1 * d1;
		double d3 = (by - ay);
		d3 = d3 * d3;

		if (d1 + d3 != lenghtWayPart2) {
			lenghtWayPart = lenghtWayPart2;
		}

		lenghtWayPart = Math.sqrt(d1 + d3);

		double ppx = getNearestPointX(matchedX, matchedY, ax, ay, bx, by);
		double ppy = getNearestPointY(matchedX, matchedY, ax, ay, bx, by);

		if (ppx == ax && ppy == ay) {
			return 0;
		}

		if (ppx == bx && ppy == by) {
			return 100;
		}

		double lenghtStartToPint;
		// lenghtStartToPint2 = ((ppx - ax) * (ppx - ax)) + ((ppy - ay) * (ppy -
		// ay));
		d1 = (ppx - ax);
		d1 = d1 * d1;
		d3 = (ppy - ay);
		d3 = d3 * d3;
		lenghtStartToPint = Math.sqrt(d1 + d3);

		lenghtWayPart = lenghtWayPart / 100.0;

		double perc = lenghtStartToPint / lenghtWayPart;

		return perc;
	}

	/**
	 * gets nearest matched gps point x-position onto street link
	 * 
	 * @param gpsNode
	 * @param streetLink
	 * @return X coordinate on street link with shortest distance to gps node
	 */
	public static double getNearestPointX(GPSNode gpsNode,
			myOSMWayPart myWayPart) {
		// delegate
		return getNearestPointX(gpsNode.getX(), gpsNode.getY(),
				myWayPart.startNode.x, myWayPart.startNode.y,
				myWayPart.endNode.x, myWayPart.endNode.y);
	}

	/**
	 * gets nearest matched gps point y-position onto street link
	 * 
	 * @param gpsNode
	 * @param streetLink
	 * @return Y coordinate on street link with shortest distance to gps node
	 */
	public static double getNearestPointY(GPSNode gpsNode,
			myOSMWayPart myWayPart) {
		// delegate
		return getNearestPointY(gpsNode.getX(), gpsNode.getY(),
				myWayPart.startNode.x, myWayPart.startNode.y,
				myWayPart.endNode.x, myWayPart.endNode.y);
	}

	/**
	 * get nearest euclidean point for a point p (x,y) to a line a-b
	 * 
	 * @param px
	 *            point x position
	 * @param py
	 *            point y position
	 * @param ax
	 *            a point of line x position
	 * @param ay
	 *            a point of line y position
	 * @param bx
	 *            b point of line x position
	 * @param by
	 *            b point of line y position
	 * @return MatchedPoint
	 */
	public static MatchedPoint getNearestEuclidianPoint(double px, double py,
			double ax, double ay, double bx, double by) {
		double x;
		double y;
		double distance;
		boolean euclidian;

		// berechne m:
		// m ist Anteil der gesamten Strecke (ax,ay) nach (bx,by), sodass die
		// kürzeste
		// Distanz zu Punkt (px,py) erreicht ist
		double m = (double) ((bx - ax) * (px - ax) + (by - ay) * (py - ay))
				/ ((bx - ax) * (bx - ax) + (by - ay) * (by - ay));

		// Wenn m außerhalb der Strecke liegt
		// pruefe, welcher Endpunkt (ax,ay) oder (bx,by) naeher an Punkt (px,py)
		// liegt
		if ((m > 1) || (m < 0)) {
			euclidian = false;
			double distanceA = getDistanceSquared(px, py, ax, ay);
			double distanceB = getDistanceSquared(px, py, bx, by);
			if (distanceA < distanceB) {
				x = ax;
				y = ay;
				distance = distanceA;
			} else {
				x = bx;
				y = by;
				distance = distanceB;
			}
		} else {
			euclidian = true;
			x = (double) (ax + m * (bx - ax));
			y = (double) (ay + m * (by - ay));
			distance = getDistanceSquared(px, py, x, y);
		}

		// gib alle informationen als klasse zurück
		return new MatchedPoint(x, y, distance, euclidian);
	}

	public static MatchedPoint getNearestEuclidianPoint(GPSNode gpsNode,
			myOSMWayPart streetLink) {
		// delegate
		return getNearestEuclidianPoint(gpsNode.getX(), gpsNode.getY(),
				streetLink.getStartX(), streetLink.getStartY(),
				streetLink.getEndX(), streetLink.getEndY());
	}

	/**
	 * get radical distance between GPS node and street node
	 * 
	 * @param gpsNode
	 * @param streetNode
	 * @return double
	 */
	public static double getDistance(GPSNode gpsNode, myOSMNode myNode) {
		return getDistance(gpsNode.getX(), gpsNode.getY(), myNode.x, myNode.y);
	}

	/**
	 * get radical distance between GPS node and street node
	 * 
	 * @param gpsNode
	 * @param streetNode
	 * @return double
	 */
	public static double getDistance(myOSMNode myNode1, myOSMNode myNode2) {
		return getDistance(myNode1.x, myNode1.y, myNode2.x, myNode2.y);
	}

	/**
	 * gets distance between two GPS nodes
	 * 
	 * @param gpsNode1
	 * @param gpsNode2
	 * @return
	 */
	public static double getDistanceSquared(GPSNode gpsNode1, GPSNode gpsNode2) {
		return getDistanceSquared(gpsNode1.getX(), gpsNode1.getY(),
				gpsNode2.getX(), gpsNode2.getY());
	}

	/**
	 * gets radical distance between two GPS nodes
	 * 
	 * @param gpsNode1
	 * @param gpsNode2
	 * @return
	 */
	public static double getDistance(GPSNode gpsNode1, GPSNode gpsNode2) {
		return getDistance(gpsNode1.getX(), gpsNode1.getY(), gpsNode2.getX(),
				gpsNode2.getY());
	}

	/**
	 * gets distance between GPS point and street link
	 * 
	 * @param gpsNode
	 * @param streetLink
	 * @return int distance gps node to street link
	 */
	public static double getDistanceSquared(GPSNode gpsNode,
			myOSMWayPart myWayPart) {
		return getDistanceSquared(gpsNode.getX(), gpsNode.getY(),
				getNearestPointX(gpsNode, myWayPart),
				getNearestPointY(gpsNode, myWayPart));
	}

	/**
	 * gets radical distance between GPS point and street link
	 * 
	 * @param gpsNode
	 * @param streetLink
	 * @return int distance gps node to street link
	 */
	public static double getDistance(GPSNode gpsNode, myOSMWayPart myWayPart) {

		double x = getNearestPointX(gpsNode, myWayPart);
		double y = getNearestPointY(gpsNode, myWayPart);

		return getDistance(gpsNode.getX(), gpsNode.getY(), x, y);
	}

	/**
	 * gets street length
	 * 
	 * @param streetLink
	 * @return
	 */
	public static double getStreetLength(myOSMWayPart myWayPart) {
		return getDistance(myWayPart.startNode, myWayPart.endNode);
	}

}

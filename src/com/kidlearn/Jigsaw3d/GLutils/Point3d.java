package com.kidlearn.Jigsaw3d.GLutils;

public class Point3d {
	
	public Point3d(float X, float Y, float Z) {
		x = X;
		y = Y;
		z = Z;
	}
	
	public Point3d(Point3d point) {
		x = point.x;
		y = point.y;
		z = point.z;
	}
	
	public Point3d add(Point3d p) {
		return new Point3d(x + p.x, y + p.y, z + p.z);
	}

	public Point3d subtract(Point3d p) {
		return new Point3d(x - p.x, y - p.y, z - p.z);
	}

	public float magnitude() {
		return (float)Math.sqrt(x*x + y*y + z*z);
	}

	public float distance(Point3d p) {
		float dx = (p.x - x);
		float dy = (p.y - y);
		float dz = (p.z - z);
		return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public Point3d scale(float s) {
		return new Point3d(x * s, y * s, z * s);
	}
	
	public float dotProduct(Point3d p) {
		return this.x * p.x + this.y*p.y + this.z*p.z;
	}

	public Point3d crossProduct(Point3d p) {
		return new Point3d(this.y*p.z - this.z*p.y, this.z*p.x - this.x*p.z, this.x*p.y - this.y*p.x);
	}
	
	public enum POSITION {BEHIND, COPLANER, INFRONT};

	public POSITION planePosition(Point3d planePoint, Point3d planeNormal) {
		
		/*
		 * Plane equation: Ax+By+Cz+D=0
		 * (x,y,z) is a point on the plane
		 * (A,B,C) the normal
		 * D the distance to the origin
		 * If you plug in a point not lying on the plane, but rather behind or in front of it,
		 * the result will be positive or negative respectively:
		 * 
		 * res = Nx*x + Ny*y + Nz*z + D
		 * 
		 * *** planeNormal must be normalized
		 */
		
		float D = -planePoint.dotProduct(planeNormal);
		float res = planeNormal.dotProduct(this) + D;
		return res < -mEpsilon ? POSITION.BEHIND : (res > mEpsilon ? POSITION.INFRONT : POSITION.COPLANER);	// -1 behind, +1 in front, 0 co-planer
	}
	
	public String toString() {
		
		return "(" + x + "," + y + "," + z + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		
		Point3d p = (Point3d)o;
		if(Math.abs(p.x - x) > mEpsilon)
			return false;
		if(Math.abs(p.y - y) > mEpsilon)
			return false;
		if(Math.abs(p.z - z) > mEpsilon)
			return false;
		
		return true;
	}
	
	private static final float mEpsilon = 0.0005f;

	public float x, y, z;
}

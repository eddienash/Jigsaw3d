package com.kidlearn.Jigsaw3d.Puzzle;

import com.kidlearn.Jigsaw3d.GLutils.Point3d;

class Edge {

	Edge(Vertex v1, Vertex v2, Point3d edgeNormal, Point3d surfaceNormal) {

		mV1 = v1;
		mV2 = v2;
		mEdgeNormal = edgeNormal;
		mSurfaceNormal = surfaceNormal;
		id = idset++;
	}
	
	@Override
	public String toString() {
		
		return "Edge(" + id + "): " + mV1.mCoord.toString() + ", " + mV2.mCoord.toString() + " d: " + mV1.mCoord.distance(mV2.mCoord);
		
	}

	Vertex mV1, mV2;
	Point3d mEdgeNormal; // points to outside of polygon to be created
	Point3d mSurfaceNormal; // normal of the surface after the loop is closed
	
	int id;
	private static int idset = 0;
}

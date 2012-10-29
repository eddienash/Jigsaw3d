package com.toychest3d.Jigsaw3d.Puzzle;

import android.content.Context;

import com.toychest3d.Jigsaw3d.GLutils.Common;
import com.toychest3d.Jigsaw3d.GLutils.Point3d;

class Cutter {

	Cutter(Context context, Point3d knifePreferredPoint, Point3d knifeNormal) {

		// knife point may change to avoid coplanar points
		mKnifePreferredPoint = new Point3d(knifePreferredPoint);
		mKnifeNormal = new Point3d(knifeNormal);
		mContext = context;
	}

	Piece[] cut(Piece piece, float knifeNudgeDistance) {

		// knifeNudgeDistance is amount to move to avoid Coplanar points

		Piece infront = new Piece(mContext);
		infront.mPosition = Triangle.POSITION.FRONT;
		Piece behind = new Piece(mContext);
		behind.mPosition = Triangle.POSITION.BEHIND;

		// find a knife point with no coplanar triangles
		Point3d knifePoint = new Point3d(mKnifePreferredPoint);
		Point3d knifeNudge = mKnifeNormal.scale(knifeNudgeDistance);

		boolean anyCoplanar = true;
		while (anyCoplanar) {

			anyCoplanar = false;

			for (int i = 0; i < piece.mTriangles.size(); i++) {
				if (piece.mTriangles.get(i).calcPositions(knifePoint, mKnifeNormal) == Triangle.POSITION.COPLANAR) {
					// move the knife
					knifePoint = knifePoint.add(knifeNudge);
					anyCoplanar = true;
					break;
				}
			}
		}

		for (int i = 0; i < piece.mTriangles.size(); i++) {

			Triangle t = piece.mTriangles.get(i);

			if (t.mPosition == Triangle.POSITION.SPLIT) {

				if (t.mV0.mPosition == t.mV1.mPosition)
					splitTriangle(infront, behind, knifePoint, t, t.mV2, t.mV0, t.mV1);
				else if (t.mV0.mPosition == t.mV2.mPosition)
					splitTriangle(infront, behind, knifePoint, t, t.mV1, t.mV2, t.mV0);
				else if (t.mV1.mPosition == t.mV2.mPosition)
					splitTriangle(infront, behind, knifePoint, t, t.mV0, t.mV1, t.mV2);
			}

			else {
				// no split so just clone this one
				Triangle t2 = new Triangle(t.mV0, t.mV1, t.mV2, t.mSurfaceIdx);
				t2.mPosition = t.mPosition;
				if (t2.mPosition == Triangle.POSITION.FRONT)
					infront.mTriangles.add(t2);
				else
					behind.mTriangles.add(t2);
			}
		}
		if ((behind.mTriangles.size() > 0) && (infront.mTriangles.size() > 0)) {
			behind.createShearedSurface();
			infront.createShearedSurface();
			return new Piece[] { behind, infront };
		} else if (behind.mTriangles.size() > 0) {
			behind.createShearedSurface();
			return new Piece[] { behind };
		} else {
			infront.createShearedSurface();
			return new Piece[] { infront };
		}
	}

	private void splitTriangle(Piece infront, Piece behind, Point3d knifePoint, Triangle t0, Vertex v0, Vertex v1, Vertex v2) {

		// p0 is always odd man out

		// split the edges
		Vertex v01 = newVert(t0.mNormal, v0, v1, knifePoint, mKnifeNormal);
		Vertex v02 = newVert(t0.mNormal, v0, v2, knifePoint, mKnifeNormal);

		infront.mOpenEdges.add(new Edge(v01, v02, t0.mNormal, mKnifeNormal.scale(-1)));
		behind.mOpenEdges.add(new Edge(v01, v02, t0.mNormal, mKnifeNormal));

		Triangle t1 = new Triangle(v0, v01, v02, t0.mSurfaceIdx);
		t1.mPosition = v0.mPosition == Point3d.POSITION.BEHIND ? Triangle.POSITION.BEHIND : Triangle.POSITION.FRONT;
		placeTriangle(infront, behind, t1);

		Triangle t2 = new Triangle(v1, v2, v01, t0.mSurfaceIdx);
		t2.mPosition = v0.mPosition != Point3d.POSITION.BEHIND ? Triangle.POSITION.BEHIND : Triangle.POSITION.FRONT;
		placeTriangle(infront, behind, t2);

		Triangle t3 = new Triangle(v2, v02, v01, t0.mSurfaceIdx);
		t3.mPosition = v0.mPosition != Point3d.POSITION.BEHIND ? Triangle.POSITION.BEHIND : Triangle.POSITION.FRONT;
		placeTriangle(infront, behind, t3);
	}

	private void placeTriangle(Piece infront, Piece behind, Triangle t) {
		if (t.mPosition == Triangle.POSITION.FRONT)
			infront.mTriangles.add(t);
		else
			behind.mTriangles.add(t);
	}

	private Vertex newVert(Point3d normal, Vertex v0, Vertex v1, Point3d knifePoint, Point3d knifeNormal) {

		Point3d p0p1 = Common.linePlaneIntersection(v0.mCoord, v1.mCoord, knifePoint, knifeNormal);

		int idx = Puzzle.mPiece0.mVerticies.size();
		Vertex v = new Vertex(p0p1, normal, idx);

		// extrapolate the UV values
		float dV0V1 = v0.mCoord.distance(v1.mCoord);
		float dV0Split = v0.mCoord.distance(v.mCoord);
		v.mU = ((v1.mU - v0.mU) * (dV0Split / dV0V1)) + v0.mU;
		v.mV = ((v1.mV - v0.mV) * (dV0Split / dV0V1)) + v0.mV;

		// add it to the master list of verticies
		Puzzle.mPiece0.mVerticies.add(v);

		return v;
	}

	private Point3d mKnifePreferredPoint, mKnifeNormal;
	private Context mContext;
}

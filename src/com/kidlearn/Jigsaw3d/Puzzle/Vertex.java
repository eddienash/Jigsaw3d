package com.kidlearn.Jigsaw3d.Puzzle;

import com.kidlearn.Jigsaw3d.GLutils.Point3d;

class Vertex {

	Vertex(float[] verts, int vertIdx) {

		int idx = Mesh.VERT_STRIDE * vertIdx;

		mCoord = new Point3d(verts[idx], verts[idx + 1], verts[idx + 2]);
		idx += 3;
		mNormal = new Point3d(verts[idx], verts[idx + 1], verts[idx + 2]);
		idx += 3;
		mU = verts[idx];
		mV = verts[idx + 1];
		mId = vertIdx;
	}

	Vertex(Point3d coord, Point3d normal, int idx) {

		mCoord = new Point3d(coord);
		mNormal = new Point3d(normal);
		mId = idx;
	}

	boolean equalCoords(Vertex v) {

		if (v.mId == this.mId)
			return true;

		if (Math.abs(v.mCoord.x - this.mCoord.x) > mEpsilon)
			return false;
		if (Math.abs(v.mCoord.y - this.mCoord.y) > mEpsilon)
			return false;
		if (Math.abs(v.mCoord.z - this.mCoord.z) > mEpsilon)
			return false;

		return true;
	}

	boolean equal(Vertex v) {

		if (v.mId == this.mId)
			return true;

		if (Math.abs(v.mCoord.x - this.mCoord.x) > mEpsilon)
			return false;
		if (Math.abs(v.mCoord.y - this.mCoord.y) > mEpsilon)
			return false;
		if (Math.abs(v.mCoord.z - this.mCoord.z) > mEpsilon)
			return false;

		if (Math.abs(v.mNormal.x - this.mNormal.x) > mEpsilon)
			return false;
		if (Math.abs(v.mNormal.y - this.mNormal.y) > mEpsilon)
			return false;
		if (Math.abs(v.mNormal.z - this.mNormal.z) > mEpsilon)
			return false;

		return true;
	}

	private static final float mEpsilon = 0.00005f;

	Point3d mCoord;
	Point3d mNormal;
	float mU, mV;
	Point3d.POSITION mPosition;
	int mId;
}

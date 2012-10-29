package com.toychest3d.Jigsaw3d.Puzzle;

import com.toychest3d.Jigsaw3d.GLutils.Point3d;

class Triangle {

	Triangle(Vertex v0, Vertex v1, Vertex v2, int surfaceIdx) {

		mV0 = v0;
		mV1 = v1;
		mV2 = v2;
		mNormal = v0.mNormal;
		mSurfaceIdx = surfaceIdx;
	}

	enum POSITION {
		BEHIND, FRONT, SPLIT, COPLANAR
	};

	POSITION calcPositions(Point3d knifePoint, Point3d knifeNormal) {

		int numBehind = 0, numInfront = 0;

		mV0.mPosition = mV0.mCoord.planePosition(knifePoint, knifeNormal);
		numBehind += mV0.mPosition == Point3d.POSITION.BEHIND ? 1 : 0;
		numInfront += mV0.mPosition == Point3d.POSITION.INFRONT ? 1 : 0;
		mV1.mPosition = mV1.mCoord.planePosition(knifePoint, knifeNormal);
		numBehind += mV1.mPosition == Point3d.POSITION.BEHIND ? 1 : 0;
		numInfront += mV1.mPosition == Point3d.POSITION.INFRONT ? 1 : 0;
		mV2.mPosition = mV2.mCoord.planePosition(knifePoint, knifeNormal);
		numBehind += mV2.mPosition == Point3d.POSITION.BEHIND ? 1 : 0;
		numInfront += mV2.mPosition == Point3d.POSITION.INFRONT ? 1 : 0;

		if ((numBehind + numInfront) < 3)
			mPosition = POSITION.COPLANAR;

		else if ((numBehind > 0) && (numInfront > 0))
			mPosition = POSITION.SPLIT;

		else
			mPosition = numInfront > 0 ? POSITION.FRONT : POSITION.BEHIND;

		return mPosition;
	}

	Vertex mV0, mV1, mV2;
	POSITION mPosition;
	Point3d mNormal;
	int mSurfaceIdx;
}

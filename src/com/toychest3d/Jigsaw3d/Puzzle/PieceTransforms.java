package com.toychest3d.Jigsaw3d.Puzzle;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import com.toychest3d.Jigsaw3d.GLutils.Common;
import com.toychest3d.Jigsaw3d.GLutils.Point3d;

class PieceTransforms {

	private static boolean hit(Piece moving, Piece still, float[] dVector) {

		int overlapCount = 0;
		AXIS[] overlaps = new AXIS[3];

		mTentry = 0;
		mTleave = 1;

		setAxis(moving, still, dVector, AXIS.X);
		POSITION positionX = calcEnterAndLeave();
		Trace.traceHit(moving, still, dVector, AXIS.X, positionX, mMovingMin, mMovingMax, mStillMin, mStillMax, mD, mTentry, mTleave);
		if (positionX == POSITION.NoHit)
			return false;
		else if (positionX == POSITION.Overlap)
			overlaps[overlapCount++] = AXIS.X;

		setAxis(moving, still, dVector, AXIS.Y);
		POSITION positionY = calcEnterAndLeave();
		Trace.traceHit(moving, still, dVector, AXIS.Y, positionY, mMovingMin, mMovingMax, mStillMin, mStillMax, mD, mTentry, mTleave);
		if (positionY == POSITION.NoHit)
			return false;
		else if (positionY == POSITION.Overlap)
			overlaps[overlapCount++] = AXIS.Y;

		setAxis(moving, still, dVector, AXIS.Z);
		POSITION positionZ = calcEnterAndLeave();
		Trace.traceHit(moving, still, dVector, AXIS.Z, positionZ, mMovingMin, mMovingMax, mStillMin, mStillMax, mD, mTentry, mTleave);
		if (positionZ == POSITION.NoHit)
			return false;
		else if (positionZ == POSITION.Overlap)
			overlaps[overlapCount++] = AXIS.Z;
		
		// test for pieces jammed together
		if (overlapCount == 3) {
			Log.d("nash", "overlapCount == 3. moving, still:  " + moving.mIndex + " " + still.mIndex);
			RuntimeException e = new RuntimeException("overlapCount == 3. moving, still:  " + moving.mIndex + " " + still.mIndex);
			Trace.traceAll(new float[] { 88, 88, 88 }, -88);
			Trace.dump(still, moving);
			throw (e);
		}

		// handle going parallel to the axis of overlap
		if ((overlapCount == 1) && (mTentry == 0)) {
			if (overlaps[0] == AXIS.X)
				return !((dVector[1] == 0) && (dVector[2] == 0));
			else if (overlaps[0] == AXIS.Y)
				return !((dVector[0] == 0) && (dVector[2] == 0));
			else
				// must be overlap == AXIS.Z
				return !((dVector[0] == 0) && (dVector[1] == 0));
		}

		// handle going parallel to the plane of overlap
		else if ((overlapCount == 2) && (mTentry == 0)) {
			if ((overlaps[0] == AXIS.X) && (overlaps[1] == AXIS.Y))
				return !(dVector[2] == 0);
			else if ((overlaps[0] == AXIS.X) && (overlaps[1] == AXIS.Z))
				return !(dVector[1] == 0);
			else
				// must be (overlaps[0] == AXIS.Y) && (overlaps[1] == AXIS.Z)
				return !(dVector[0] == 0);
		}

		return true;
	}

	private static POSITION calcEnterAndLeave() {

		float tEntry = 0, tLeave = 0, gapEnter = 0, gapLeave = 0;

		// case 1 - moving is further from origin than still
		if ((mMovingMin - mStillMax) > -mEpsilon) {

			if (mD >= 0)
				return POSITION.NoHit; // never gonna hit

			gapEnter = mMovingMin - mStillMax;
			tEntry = -(gapEnter / mD);
			if (tEntry >= 1)
				return POSITION.NoHit; // hit is past vector

			gapLeave = mMovingMax - mStillMin;
			tLeave = -(gapLeave / mD);
		}

		// case 2 - moving is closer origin than still
		else if ((mStillMin - mMovingMax) > -mEpsilon) {

			if (mD <= 0)
				return POSITION.NoHit; // never gonna hit

			gapEnter = mStillMin - mMovingMax;
			tEntry = gapEnter / mD;
			if (tEntry >= 1)
				return POSITION.NoHit; // hit is past vector

			gapLeave = mStillMax - mMovingMin;
			tLeave = gapLeave / mD;
		}

		// case 3: they overlap
		else
			return POSITION.Overlap;

		if (tEntry < 0)
			tEntry = 0;
		if (tLeave < 0)
			tLeave = 0;

		// update overall entry and leave time
		mTentry = tEntry < mTentry ? tEntry : mTentry;
		mTleave = tLeave > mTleave ? tLeave : mTleave;

		// have to get there before you can leave
		if (mTentry <= mTleave)
			return POSITION.Hit;
		else
			return POSITION.NoHit;
	}

	private static void setAxis(Piece moving, Piece still, float[] dVector, AXIS axis) {

		float[] transStill = getOneTranslate(still.mIndex);
		float[] transMoving = getOneTranslate(moving.mIndex);

		switch (axis) {
		case X:
			mMovingMin = moving.bbMin.x + transMoving[0];
			mMovingMax = moving.bbMax.x + transMoving[0];
			mStillMin = still.bbMin.x + transStill[0];
			mStillMax = still.bbMax.x + transStill[0];
			mD = dVector[0];
			break;

		case Y:
			mMovingMin = moving.bbMin.y + transMoving[1];
			mMovingMax = moving.bbMax.y + transMoving[1];
			mStillMin = still.bbMin.y + transStill[1];
			mStillMax = still.bbMax.y + transStill[1];
			mD = dVector[1];
			break;

		case Z:
			mMovingMin = moving.bbMin.z + transMoving[2];
			mMovingMax = moving.bbMax.z + transMoving[2];
			mStillMin = still.bbMin.z + transStill[2];
			mStillMax = still.bbMax.z + transStill[2];
			mD = dVector[2];
		}
	}

	static float[] unProjectRay(Piece piece, float x, float y) {

		int[] view = new int[] { 0, 0, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight };
		float[] obj = new float[8];
		if(GLU.gluUnProject(x, y, 0, piece.mDrawMatrix, 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj, 0) != GL10.GL_TRUE)
			return null;
		if(GLU.gluUnProject(x, y, 1, piece.mDrawMatrix, 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj, 4) != GL10.GL_TRUE)
			return null;
		// normalize
		for (int i = 0; i < 4; i++) {
			obj[i] /= obj[3];
			obj[i + 4] /= obj[7];
		}
		return obj;
	}

	private static Point3d intersectOneFace(Point3d rayStart, Point3d rayEnd, FACE face, float[] parms) {

		Point3d pR = new Point3d(parms[4], parms[5], parms[6]);
		Point3d nR = new Point3d(parms[8], parms[9], parms[10]);
		Point3d inter = Common.linePlaneIntersection(rayStart, rayEnd, pR, nR);

		float minP1 = parms[0];
		float maxP1 = parms[1];
		float minP2 = parms[2];
		float maxP2 = parms[3];

		if (inter != null) {

			// is it on the face
			if ((face == FACE.Front) || (face == FACE.Back)) {
				if ((inter.x < minP1) || (inter.x > maxP1))
					return null;
				if ((inter.y < minP2) || (inter.y > maxP2))
					return null;
			}

			else if ((face == FACE.Left) || (face == FACE.Right)) {
				if ((inter.y < minP1) || (inter.y > maxP1))
					return null;
				if ((inter.z < minP2) || (inter.z > maxP2))
					return null;
			} else { // if((face == FACE.Top) || (face == FACE.Bottom)) {
				if ((inter.x < minP1) || (inter.x > maxP1))
					return null;
				if ((inter.z < minP2) || (inter.z > maxP2))
					return null;
			}
		}

		return inter;
	}

	private static float[] faceParms(Piece piece, FACE face) {

		switch (face) {

		case Front:
			return new float[] { piece.bbMin.x, piece.bbMax.x, piece.bbMin.y, piece.bbMax.y, piece.bbMax.x, piece.bbMax.y, piece.bbMax.z, 1, 0, 0, -1, 1 };
		case Back:
			return new float[] { piece.bbMin.x, piece.bbMax.x, piece.bbMin.y, piece.bbMax.y, piece.bbMin.x, piece.bbMin.y, piece.bbMin.z, 1, 0, 0, 1, 1 };
		case Left:
			return new float[] { piece.bbMin.y, piece.bbMax.y, piece.bbMin.z, piece.bbMax.z, piece.bbMin.x, piece.bbMin.y, piece.bbMin.z, 1, -1, 0, 0, 1 };
		case Right:
			return new float[] { piece.bbMin.y, piece.bbMax.y, piece.bbMin.z, piece.bbMax.z, piece.bbMax.x, piece.bbMax.y, piece.bbMax.z, 1, 1, 0, 0, 1 };
		case Top:
			return new float[] { piece.bbMin.x, piece.bbMax.x, piece.bbMin.z, piece.bbMax.z, piece.bbMax.x, piece.bbMax.y, piece.bbMax.z, 1, 0, 1, 0, 1 };
		case Bottom:
			return new float[] { piece.bbMin.x, piece.bbMax.x, piece.bbMin.z, piece.bbMax.z, piece.bbMin.x, piece.bbMin.y, piece.bbMin.z, 1, 0, -1, 0, 1 };
		}

		return null;
	}

	private static float closestFace(Piece piece, FACE face, float dMin, Point3d target) {

		float[] parms = faceParms(piece, face);
		Point3d isect = intersectOneFace(piece.mRayStart, piece.mRayEnd, face, parms);
		if (isect != null) {
			float d = isect.distance(target);
			if (d < dMin) {
				dMin = d;
				piece.mFacePoint = new Point3d(isect);
				piece.mFaceNormal = new Point3d(parms[8], parms[9], parms[10]);
				piece.mFaceIntersect = new Point3d(isect);
				piece.mTouchedFace = face;
			}
		}

		return dMin;
	}

	static void onTouch(Piece mTouchedPiece, MotionEvent event) {
		
		float[] touchRay = null;
		float[] drawTransform = new float[16];

		switch (event.getActionMasked()) {

		case MotionEvent.ACTION_DOWN:
			break;

		case MotionEvent.ACTION_MOVE:

			// intersection with constrained plane
			touchRay = unProjectRay(mTouchedPiece, event.getX(), PlayRenderer.mSurfaceHeight - event.getY());
			if (touchRay == null) {
				RuntimeException e = new RuntimeException("ACTION_MOVE null touchRay 1");
				throw (e);
			}

			Point3d rayStart = new Point3d(touchRay[0], touchRay[1], touchRay[2]);
			Point3d rayEnd = new Point3d(touchRay[4], touchRay[5], touchRay[6]);

			Point3d inter = Common.linePlaneIntersection(rayStart, rayEnd, mTouchedPiece.mFacePoint, mTouchedPiece.mFaceNormal);

			// moving out of sight
			if (inter == null)
				break;

			float[] rotatedV = new float[4];
			rotatedV[0] = inter.x - mTouchedPiece.mFaceIntersect.x;
			rotatedV[1] = inter.y - mTouchedPiece.mFaceIntersect.y;
			rotatedV[2] = inter.z - mTouchedPiece.mFaceIntersect.z;
			
			// collision detection with any other piece
			float scale = hitScale(mTouchedPiece, Puzzle.mPieces, rotatedV);

			float[] trans = addToTranslates(mTouchedPiece.mIndex, rotatedV[0] * scale, rotatedV[1] * scale, rotatedV[2] * scale);

			Matrix.setIdentityM(drawTransform, 0);
			Matrix.translateM(drawTransform, 0, trans[0], trans[1], trans[2]);
			Matrix.multiplyMM(mTouchedPiece.mDrawMatrix, 0, WorldTransforms.getRotatedView(), 0, drawTransform, 0);

			touchRay = unProjectRay(mTouchedPiece, event.getX(), PlayRenderer.mSurfaceHeight - event.getY());
			if (touchRay == null) {
				RuntimeException e = new RuntimeException("ACTION_MOVE null touchRay 2");
				throw (e);
			}
			rayStart = new Point3d(touchRay[0], touchRay[1], touchRay[2]);
			rayEnd = new Point3d(touchRay[4], touchRay[5], touchRay[6]);
			mTouchedPiece.mFaceIntersect = Common.linePlaneIntersection(rayStart, rayEnd, mTouchedPiece.mFacePoint, mTouchedPiece.mFaceNormal);
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:

			if (snap(mTouchedPiece)) {
				if (Persistance.mSounds)
					PuzzleActivity.mSoundPool.play(PuzzleActivity.mPopSound, 1, 1, 5, 0, 1);

				setPieceTranslate(mTouchedPiece.mIndex, 0, 0, 0);

				Matrix.setIdentityM(drawTransform, 0);
				Matrix.multiplyMM(mTouchedPiece.mDrawMatrix, 0, WorldTransforms.getRotatedView(), 0, drawTransform, 0);
			}
			mTouchedPiece.mTouched = false;
		}
	}

	static float hitScale(Piece testPiece, ArrayList<Piece> pieces, float[] deltaV) {

		float scale = 1;
		mHitPiece = null;

		// test every piece for collision
		for (int i = 0; i < pieces.size(); i++) {

			Piece piece = pieces.get(i);

			if (testPiece != piece) {
				if (hit(testPiece, piece, deltaV)) {
					if (mTentry < scale) {
						scale = mTentry;
						mHitPiece = piece;
					}
				}
			}
		}

		return scale;
	}

	private static boolean snap(Piece piece) {

		float[] trans = new float[3 * Puzzle.mPieces.size()];
		getTransforms(trans);
		
		// distance to home
		float d = trans[piece.mIndex * 3] * trans[piece.mIndex * 3];
		d += trans[(piece.mIndex * 3) + 1] * trans[(piece.mIndex * 3) + 1];
		d += trans[(piece.mIndex * 3) + 2] * trans[(piece.mIndex * 3) + 2];
		d = (float) Math.sqrt(d);

		// / too far to snap
		if (d > (piece.mDiagonal / 4))
			return false;

		// will it be blocked?
		for (Piece blocker : Puzzle.mPieces) {

			if (piece != blocker) {
				
				int idx = blocker.mIndex * 3;

				// if the blocker is home, it's OK - avoid rounding errors
				if ((trans[idx] == 0) && (trans[idx+1] == 0) && (trans[idx+2] == 0))
					continue;
				if (piece.bbMin.x > (blocker.bbMax.x + trans[idx]))
					continue;
				if (piece.bbMax.x < (blocker.bbMin.x + trans[idx]))
					continue;
				if (piece.bbMin.y > (blocker.bbMax.y + trans[idx+1]))
					continue;
				if (piece.bbMax.y < (blocker.bbMin.y + trans[idx+1]))
					continue;
				if (piece.bbMin.z > (blocker.bbMax.z + trans[idx+2]))
					continue;
				if (piece.bbMax.z < (blocker.bbMin.z + trans[idx+2]))
					continue;

				// Something blocking
				return false;
			}
		}

		// no blockers, can snap
		return true;
	}
	
	static int touchedPiece(MotionEvent event) {
		
		float[] touchRay = null;
		int pieceIdx = Puzzle.mPieces.size();
		float pMax = Float.MAX_VALUE;
			
		for (Piece piece : Puzzle.mPieces) {
				
			// get ray from near plane to far plane
			touchRay = unProjectRay(piece, event.getX(), PlayRenderer.mSurfaceHeight - event.getY());
			if(touchRay == null)
				continue;
				
			piece.mRayStart = new Point3d(touchRay[0], touchRay[1], touchRay[2]);
			piece.mRayEnd = new Point3d(touchRay[4], touchRay[5], touchRay[6]);

			// use the piece/face whose intersection is closest to the eye
			piece.mFaceIntersect = null;
			float dMax = piece.mRayStart.distance(piece.mRayEnd); // from eye at 0,0,0 to far plane
			
			for(FACE face: FACE.values())
				dMax = closestFace(piece, face, dMax, piece.mRayStart);
			
			if((piece.mFaceIntersect != null) && (dMax < pMax)) {
				pMax = dMax;
				pieceIdx = piece.mIndex;
			}
		}
		
		return pieceIdx;
	}
	
	static void initialize(int numPieces) {
		mPieceTranslates = new float[numPieces * 3];
	}
	
	static /* synchronized */ void setPieceTranslate(int index, float x, float y, float z) {
		mPieceTranslates[index * 3] = x;
		mPieceTranslates[(index * 3) + 1] = y;
		mPieceTranslates[(index * 3) + 2] = z;
	}
	
	static /* synchronized */ void getTransforms(float[] translates) {

		if(translates != null) {
			for(int i=0; i<mPieceTranslates.length; i++)
				translates[i] = mPieceTranslates[i];
		}
	}
	
	static /* synchronized */ float[] addToTranslates(int index, float x, float y, float z) {
		float[] trans = new float[3];
		int idx = index * 3;
		mPieceTranslates[idx] += x;
		trans[0] = mPieceTranslates[idx];
		mPieceTranslates[idx + 1] += y;
		trans[1] = mPieceTranslates[idx + 1];
		mPieceTranslates[idx + 2] += z;
		trans[2] = mPieceTranslates[idx + 2];
		return trans;
	}

	static /* synchronized */ float[] setTranslates(int index, float x, float y, float z) {
		float[] trans = new float[3];
		int idx = index * 3;
		mPieceTranslates[idx] = x;
		trans[0] = mPieceTranslates[idx];
		mPieceTranslates[idx + 1] = y;
		trans[1] = mPieceTranslates[idx + 1];
		mPieceTranslates[idx + 2] = z;
		trans[2] = mPieceTranslates[idx + 2];
		return trans;
	}

	static /* synchronized */ float[] getOneTranslate(int index) {
		float[] trans = new float[3];
		trans[0] = mPieceTranslates[index * 3];
		trans[1] = mPieceTranslates[(index * 3) + 1];
		trans[2] = mPieceTranslates[(index * 3) + 2];
		return trans;
	}

	private static float[] mPieceTranslates;
	
	private static final float mEpsilon = 0.00005f;

	static float[] mRotatedDepth = new float[4];
	static float[] mDeltaV = new float[] { 0, 0, 0, 1 };
	static float[] mInverseRotate = new float[16];
	static Piece mHitPiece;

	enum FACE {Front, Back, Left, Right, Top, Bottom}
	enum POSITION {Hit, NoHit, Overlap}
	enum AXIS {X, Y, Z}

	private static float mTentry;
	private static float mMovingMin, mMovingMax, mStillMin, mStillMax, mD;
	private static float mTleave;
}

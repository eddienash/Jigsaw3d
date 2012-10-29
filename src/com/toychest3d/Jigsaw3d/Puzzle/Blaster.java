package com.toychest3d.Jigsaw3d.Puzzle;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.opengl.GLU;
import android.opengl.Matrix;

//import com.toychest3d.Jigsaw3d.Puzzle.GameEvent.RENDER_CMD;
import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.GLutils.Point3d;

class Blaster {

	private static final float EXPLODE_VELOCITY = 1.5f;
	private static final int EXPLODE_RUN_SECONDS = 10;
	private static final float START_ZOOM = .75f;

	static void initialize(Context context) {
		
		mContext = context;
		
		// reset the zoom and rotation
		WorldTransforms.setRotatedView(START_ZOOM, WorldTransforms.InitialRotateX, WorldTransforms.InitialRotateY, 0, 0);
		
		// reset all pieces to home
		for(Piece p : Puzzle.mPieces)
			PieceTransforms.setPieceTranslate(p.mIndex, 0, 0, 0);
		
		mBlastTimer = null;
		mBlastTimerTask = null;

		// initialize the queues of moving pieces
		mQ1 = new ArrayList<Piece>(Puzzle.mPieces.size());
		mThisQ = mQ1;
		mQ2 = new ArrayList<Piece>(Puzzle.mPieces.size());
		mNextQ = mQ2;
		
		// TODO randomize blast point
		Random rand = new Random();
		float blastPointRange = Puzzle.mPiece0.mDiagonal * 0.33f; // arbitrary
		Point3d blastPoint = new Point3d(rand.nextFloat() * blastPointRange,rand.nextFloat() * blastPointRange,rand.nextFloat() * blastPointRange);

		for (Piece piece : Puzzle.mPieces) {

			// mass is equal to volume
			piece.mMass = (piece.bbMax.x - piece.bbMin.x) * (piece.bbMax.y - piece.bbMin.y) * (piece.bbMax.z - piece.bbMin.z);
			float blastMag = blastPoint.distance(piece.mHome);
			Point3d blastDir = new Point3d(piece.mHome.x - blastPoint.x, piece.mHome.y - blastPoint.y, piece.mHome.z - blastPoint.z);
			blastDir = blastDir.scale(blastDir.magnitude());
			piece.mV = blastDir.scale(blastMag == 0 ? 0 : EXPLODE_VELOCITY / blastMag);
			mNextQ.add(piece);
			piece.mCurrentExplodeQ = mNextQ;
		}

		// calculate frustum "walls" - normals will point to inside of frustum
		int[] viewPort = new int[] { 0, 0, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight };
		mTopWall = frustumSideWall(viewPort, 0, PlayRenderer.mSurfaceHeight, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight);
		mBottomWall = frustumSideWall(viewPort, PlayRenderer.mSurfaceWidth, 0, 0, 0);
		mLeftWall = frustumSideWall(viewPort, 0, 0, 0, PlayRenderer.mSurfaceHeight);
		mRightWall = frustumSideWall(viewPort, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight, PlayRenderer.mSurfaceWidth, 0);
		mFrontWall = frustumFacingWall(viewPort, 0);
		mBackWall = frustumFacingWall(viewPort, 1);
		
		alert = null;
		
		Trace.initialize();
	}
	
	static void start() {
		
		mBlastClock = new GameClock();
		mBlastClock.reset();
		
		mBlastTimerTask = new TimerTask() {
			@Override
			public void run() {
				
				mBlastClock.tick();
				
				if (mBlastClock.elapsedSecs < EXPLODE_RUN_SECONDS) {
					advance(mBlastClock);
					((Activity)PuzzleActivity.mContext).runOnUiThread(mClockUpdate);
					PuzzleActivity.mGl.requestRender();
				}
				else {
					complete();
				}
			}
		};

		alert = new AlertDialog.Builder(PuzzleActivity.mContext).create();
		alert.setCancelable(true);
		alert.setMessage(PuzzleActivity.mContext.getString(R.string.msg_shuffling));
		alert.setOnCancelListener(cancelListener);
		alert.show();

		mBlastTimer = new Timer();
		mBlastTimer.schedule(mBlastTimerTask, 0, 30);
	}
	
	private static OnCancelListener cancelListener = new OnCancelListener() {

		public void onCancel(DialogInterface dialog) {
			((Activity)mContext).onBackPressed();
		}
	};
	
	private static void complete() {
		
		alert.dismiss();

		mBlastTimer.cancel();
		mBlastTimerTask = null;
		mBlastTimer = null;
		
		((Activity)PuzzleActivity.mContext).runOnUiThread(new Runnable() {
			public void run() {
				StateMachine2.postEvent(StateMachine2.Gevent.BLASTER_COMPLETE);
			}
		});
	}
	
	
	static void pause() {
		
		if((alert != null) && alert.isShowing())
			alert.dismiss();
		
		if(mBlastTimer != null)
			mBlastTimer.cancel();
		
		if(mBlastTimerTask != null)
			mBlastTimerTask.cancel();
	}
	
	private static Runnable mClockUpdate = new Runnable() {
		public void run() {
			
			String display = PuzzleActivity.mContext.getString(R.string.msg_shuffling);
			display += String.format(" 0:%02d", EXPLODE_RUN_SECONDS - (int)mBlastClock.elapsedSecs);
			alert.setMessage(display);
		}
	};

	private static void advance(GameClock clock) {
		
		Trace.traceAll(new float[] {99,99,99}, -99);

		// swap the queues
		mThisQ = mThisQ == mQ1 ? mQ2 : mQ1;
		mNextQ = mNextQ == mQ1 ? mQ2 : mQ1;

		// process all moving pieces
		while (!mThisQ.isEmpty()) {

			Piece moving = mThisQ.remove(0);

			// if he was bounced from this queue, ignore
			if (moving.mCurrentExplodeQ != mThisQ)
				continue;
			
			Trace.traceAll(new float[] {66,66,66}, -66);

			float[] deltaV = new float[] {moving.mV.x * clock.deltaSecs, moving.mV.y * clock.deltaSecs, moving.mV.z * clock.deltaSecs};

			// hit another piece?
			float scale = PieceTransforms.hitScale(moving, Puzzle.mPieces, deltaV);

			// check for wall hit
			mNormalWallHit = null;
			scale = testOneWall(moving, mTopWall, deltaV, scale);
			scale = testOneWall(moving, mBottomWall, deltaV, scale);
			scale = testOneWall(moving, mLeftWall, deltaV, scale);
			scale = testOneWall(moving, mRightWall, deltaV, scale);
			scale = testOneWall(moving, mFrontWall, deltaV, scale);
			scale = testOneWall(moving, mBackWall, deltaV, scale);

			// move the piece (scale will b1 1 if no hit)
			float[] trans = PieceTransforms.addToTranslates(moving.mIndex, deltaV[0] * scale, deltaV[1] * scale, deltaV[2] * scale);

			Trace.traceAll(new float[] {44,44,44}, -44);
			Trace.traceAll(deltaV, scale);

			float[] drawTransform = new float[16];
			Matrix.setIdentityM(drawTransform, 0);
			Matrix.translateM(drawTransform, 0, trans[0], trans[1], trans[2]);
			Matrix.multiplyMM(moving.mDrawMatrix, 0, WorldTransforms.getRotatedView(), 0, drawTransform, 0);

			// piece hit
			if (PieceTransforms.mHitPiece != null) {

				// calculate the new vectors, exchange momentum
				Point3d temp = moving.mV;
				moving.mV = PieceTransforms.mHitPiece.mV;
				moving.mV.scale(PieceTransforms.mHitPiece.mMass / moving.mMass);
				PieceTransforms.mHitPiece.mV = temp;
				PieceTransforms.mHitPiece.mV.scale(moving.mMass / PieceTransforms.mHitPiece.mMass);
			}

			// wall hit
			else if (mNormalWallHit != null) {

				// calculate new vector - reverse direction
				moving.mV = moving.mV.scale(-1);
			}

			// queue the moving piece if still moving
			if (moving.mV.magnitude() > 0) {
				mNextQ.add(moving);
				moving.mCurrentExplodeQ = mNextQ;
			} else
				moving.mCurrentExplodeQ = null;

			// if there was a hit piece, queue it if still moving
			if (PieceTransforms.mHitPiece != null) {
				if (PieceTransforms.mHitPiece.mV.magnitude() > 0) {
					if (PieceTransforms.mHitPiece.mCurrentExplodeQ == null) {
						mNextQ.add(PieceTransforms.mHitPiece);
						PieceTransforms.mHitPiece.mCurrentExplodeQ = mNextQ;
					}
				} else
					PieceTransforms.mHitPiece.mCurrentExplodeQ = null;
			}
		}
	}

	private static float testOneWall(Piece piece, Point3d[] wall, float[] deltaV, float t) {

		if (wallHit(piece, wall, deltaV)) {
			// if soonest hit
			if (mTwallPieceHit < t) {
				// save the normal and return new smallest
				mNormalWallHit = wall[1];
				return mTwallPieceHit;
			}
		}
		// no hit, no change
		return t;
	}

	private static boolean wallHit(Piece piece, Point3d[] wall, float[] deltaV) {

		Point3d point = wall[0];
		Point3d normal = wall[1];
		
		// parametric equation of plane p . n = d
		float d = point.dotProduct(normal);

		// movement vector
		Point3d dir = new Point3d(deltaV[0], deltaV[1], deltaV[2]);

		// heading in the direction (normal and dir opposite directions)?
		float dot = normal.dotProduct(dir);
		if (dot >= 0)
			return false;

		// point on the bounding box, closest to the plane
		float[] trans = PieceTransforms.getOneTranslate(piece.mIndex);
		Point3d closest = new Point3d(normal.x > 0 ? piece.bbMin.x + trans[0] : piece.bbMax.x + trans[0],
				normal.y > 0 ? piece.bbMin.y + trans[1] : piece.bbMax.y + trans[1],
						normal.z > 0 ? piece.bbMin.z + trans[2] : piece.bbMax.z + trans[2]);

		float t = (d - closest.dotProduct(normal)) / dot;

		mTwallPieceHit = t > 0 ? t : 0;

		return (mTwallPieceHit < 1);
		
	}

	static Point3d[] frustumFacingWall(int[] view, float z) {

		// returns 2 Point3d in array: first is plane point, second is normal
		// PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight

		// first corner at z
		float[] obj1 = new float[4];
		GLU.gluUnProject(0, 0, z, WorldTransforms.getRotatedView(), 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj1, 0);
		// normalize
		for (int i = 0; i < 4; i++)
			obj1[i] /= obj1[3];

		// second corner
		float[] obj2 = new float[4];
		GLU.gluUnProject(0, PlayRenderer.mSurfaceHeight, z, WorldTransforms.getRotatedView(), 0,
																WorldTransforms.getProjectionMatrix(), 0, view, 0, obj2, 0);
		// normalize
		for (int i = 0; i < 4; i++)
			obj2[i] /= obj2[3];

		// thisrd corner
		float[] obj3 = new float[4];
		GLU.gluUnProject(PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight, z, WorldTransforms.getRotatedView(), 0,
																WorldTransforms.getProjectionMatrix(), 0, view, 0, obj3, 0);
		// normalize
		for (int i = 0; i < 4; i++)
			obj3[i] /= obj3[3];

		// point on the plane (any corner will do)
		Point3d point = new Point3d(obj1[0], obj1[1], obj1[2]);

		// normal
		Point3d v1 = new Point3d(obj2[0] - obj1[0], obj2[1] - obj1[1], obj2[2] - obj1[2]);
		Point3d v2 = new Point3d(obj3[0] - obj2[0], obj3[1] - obj2[1], obj3[2] - obj2[2]);
		Point3d n = z == 0 ? v1.crossProduct(v2) : v2.crossProduct(v1);
		Point3d normal = n.scale(1 / n.magnitude());

		return new Point3d[] { point, normal };
	}

	static Point3d[] frustumSideWall(int[] view, float corner1X, float corner1Y, float corner2X, float corner2Y) {

		// returns 2 Point3d in array: first is plane point, second is normal

		// first corner near and far points
		float[] obj1 = new float[8];
		GLU.gluUnProject(corner1X, corner1Y, 0, WorldTransforms.getRotatedView(), 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj1, 0);
		GLU.gluUnProject(corner1X, corner1Y, 1, WorldTransforms.getRotatedView(), 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj1, 4);
		// normalize
		for (int i = 0; i < 4; i++) {
			obj1[i] /= obj1[3];
			obj1[i + 4] /= obj1[7];
		}

		// second corner near and far points
		float[] obj2 = new float[8];
		GLU.gluUnProject(corner2X, corner2Y, 0, WorldTransforms.getRotatedView(), 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj2, 0);
		GLU.gluUnProject(corner2X, corner2Y, 1, WorldTransforms.getRotatedView(), 0, WorldTransforms.getProjectionMatrix(), 0, view, 0, obj2, 4);
		// normalize
		for (int i = 0; i < 4; i++) {
			obj2[i] /= obj2[3];
			obj2[i + 4] /= obj2[7];
		}

		// point on the plane (any corner will do)
		Point3d point = new Point3d(obj1[0], obj1[1], obj1[2]);

		// normal
		Point3d v1 = new Point3d(obj1[4] - obj1[0], obj1[5] - obj1[1], obj1[6] - obj1[2]);
		Point3d v2 = new Point3d(obj2[0] - obj1[0], obj2[1] - obj1[1], obj2[2] - obj1[2]);
		Point3d n = v1.crossProduct(v2);
		Point3d normal = n.scale(1 / n.magnitude());

		return new Point3d[] { point, normal };
	}

	private static AlertDialog alert;
	private static Timer mBlastTimer;
	private static GameClock mBlastClock;
	private static TimerTask mBlastTimerTask;
	private static Context mContext;

	private static float mTwallPieceHit;
	private static Point3d mNormalWallHit;
	private static ArrayList<Piece> mQ1, mQ2, mThisQ, mNextQ;
	private static Point3d[] mTopWall, mBottomWall, mLeftWall, mRightWall, mFrontWall, mBackWall;
}
package com.kidlearn.Jigsaw3d.Puzzle;

import android.opengl.Matrix;
import android.view.MotionEvent;

import com.kidlearn.Jigsaw3d.GLutils.Common;
import com.kidlearn.Jigsaw3d.GLutils.Point3d;

class WorldTransforms {
	
	static final float InitialRotateX = 30;
	static final float InitialRotateY = -15;

	static void init() {
		
		mZoom = 1;
		mRotateX = InitialRotateX;
		mRotateY = InitialRotateY;
		mPanX = 0;
		mPanY = 0;
		mRotatePanZoomMatrix = new float[16];
		mRotatedView = new float[16];
		
		__setRotatedView(mZoom, mRotateX, mRotateY, mPanX, mPanY);
	}

	static synchronized boolean fit() {
		
		__setRotatedView(mZoom, mRotateX, mRotateY, 0, 0);

		// calculate frustum "walls" - normals will point to inside of frustum
		int[] viewPort = new int[] { 0, 0, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight };
		Point3d[] topWall = Blaster.frustumSideWall(viewPort, 0, PlayRenderer.mSurfaceHeight, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight);
		Point3d[] bottomWall = Blaster.frustumSideWall(viewPort, PlayRenderer.mSurfaceWidth, 0, 0, 0);
		Point3d[] leftWall = Blaster.frustumSideWall(viewPort, 0, 0, 0, PlayRenderer.mSurfaceHeight);
		Point3d[] rightWall = Blaster.frustumSideWall(viewPort, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight, PlayRenderer.mSurfaceWidth, 0);
		Point3d[] frontWall = Blaster.frustumFacingWall(viewPort, 0);
		Point3d[] backWall = Blaster.frustumFacingWall(viewPort, 1);

		// get maximum required zoom reduction across all pieces, all walls
		float[] translates = new float[3 * Puzzle.mPieces.size()];
		PieceTransforms.getTransforms(translates);
		float zoom = 0;
		for (Piece moving : Puzzle.mPieces) {
			float transalteX = translates[moving.mIndex * 3];
			float transalteY = translates[(moving.mIndex * 3) + 1];
			float transalteZ = translates[(moving.mIndex * 3) + 2];
			zoom = zoomBackInside(moving, topWall, zoom, transalteX, transalteY, transalteZ);
			zoom = zoomBackInside(moving, bottomWall, zoom, transalteX, transalteY, transalteZ);
			zoom = zoomBackInside(moving, leftWall, zoom, transalteX, transalteY, transalteZ);
			zoom = zoomBackInside(moving, rightWall, zoom, transalteX, transalteY, transalteZ);
			zoom = zoomBackInside(moving, frontWall, zoom, transalteX, transalteY, transalteZ);
			zoom = zoomBackInside(moving, backWall, zoom, transalteX, transalteY, transalteZ);
		}

		if (zoom > 0) {
			__setRotatedView(mZoom * (1 - zoom), mRotateX, mRotateY, mPanX, mPanY);
			return true;
		}

		return false;
	}

	static synchronized void rotateEvent(MotionEvent event) {

		switch (event.getActionMasked()) {

		case MotionEvent.ACTION_DOWN:
			mStartX = event.getX();
			mStartY = event.getY();
			break;

		case MotionEvent.ACTION_MOVE:

			if (mMode == Mode.ROTATE) {
				mRotateY += 75f * (event.getX() - mStartX) / PlayRenderer.mSurfaceWidth;
				mRotateX += 75f * (event.getY() - mStartY) / PlayRenderer.mSurfaceHeight;
			}
			
			else { // PAN
				mPanX += 3f * (event.getX() - mStartX) / PlayRenderer.mSurfaceWidth;
				mPanY -= 3f * (event.getY() - mStartY) / PlayRenderer.mSurfaceHeight;
			}

			mStartX = event.getX();
			mStartY = event.getY();
			
			break;
		}

		__setRotatedView(mZoom, mRotateX, mRotateY, mPanX, mPanY);
	}
	
	static synchronized void zoomIn() {
		__setRotatedView(mZoom * 1.1f, mRotateX, mRotateY, mPanX, mPanY);
	}
	static synchronized void zoomOut() {
		__setRotatedView(mZoom / 1.1f, mRotateX, mRotateY, mPanX, mPanY);
	}

	static synchronized void setRotatedView(float zoom, float rotateX, float rotateY, float panX, float panY) {
		__setRotatedView(zoom, rotateX, rotateY, panX, panY);
	}
	static synchronized float[] getRotatedView() {
		return mRotatedView;
	}
	static synchronized void saveRotatedView() {
		mSaveZoom = mZoom;
		mSaveRotateX = mRotateX;
		mSaveRotateY = mRotateY;
		mSavePanX = mPanX;
		mSavePanY = mPanY;
	}
	static synchronized void restoreRotatedView() {
		__setRotatedView(mSaveZoom, mSaveRotateX, mSaveRotateY, mSavePanX, mSavePanY);
	}
	private static void __setRotatedView(float zoom, float rotateX, float rotateY, float panX, float panY) {
		mZoom = zoom;
		mRotateX = rotateX;
		mRotateY = rotateY;
		mPanX = panX;
		mPanY = panY;
		Matrix.setIdentityM(mRotatePanZoomMatrix, 0);
		Matrix.translateM(mRotatePanZoomMatrix, 0, mPanX, mPanY, 0);
		Matrix.rotateM(mRotatePanZoomMatrix, 0, mRotateX, 1, 0, 0);
		Matrix.rotateM(mRotatePanZoomMatrix, 0, mRotateY, 0, 1, 0);
		Matrix.scaleM(mRotatePanZoomMatrix, 0, mZoom, mZoom, mZoom);
		Matrix.multiplyMM(mRotatedView, 0, mViewMatrix, 0, mRotatePanZoomMatrix, 0);
	}

	private static float zoomBackInside(Piece piece, Point3d[] wall, float zoom, float translateX, float translateY, float translateZ) {

		Point3d point = wall[0];
		Point3d normal = wall[1];

		// point on the bounding box, closest to the plane
		Point3d closest = new Point3d(	normal.x > 0 ? piece.bbMin.x + translateX : piece.bbMax.x + translateX,
										normal.y > 0 ? piece.bbMin.y + translateY : piece.bbMax.y + translateY,
										normal.z > 0 ? piece.bbMin.z + translateZ : piece.bbMax.z + translateZ);

		// intersection of point with wall - null if point is inside wall
		Point3d isect = Common.linePlaneIntersection(closest, new Point3d(0, 0, 0), point, normal);

		// new zoom value if there is one, or if greater than most recent
		return isect == null ? zoom : Math.max(isect.distance(closest) / closest.magnitude(), zoom);
	}
	
	static synchronized void setViewMatrix(float[] viewMatrix) {
		mViewMatrix = viewMatrix.clone();
	}
	
	static synchronized void setProjectionMatrix(float[] projectionMatrix) {
		mProjection = projectionMatrix.clone();
	}
	static synchronized float[] getProjectionMatrix() {
		return mProjection;
	}

	enum Mode {ROTATE, PAN};

	private static float[] mRotatePanZoomMatrix;
	private static float mStartX, mStartY;
	private static float mSaveZoom, mSaveRotateX, mSaveRotateY, mSavePanX, mSavePanY;
	
	static float mPanX;
	static float mPanY;
	static Mode mMode;
	static float mZoom;
	static float mRotateX, mRotateY;
	
	private static float[] mViewMatrix;
	private static float[] mRotatedView;
	private static float[] mProjection;
}

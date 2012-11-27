package com.kidlearn.Jigsaw3d.Puzzle;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.kidlearn.Jigsaw3d.GLutils.Common;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

class PlayRenderer implements GLSurfaceView.Renderer {

	PlayRenderer(Context context) {

		mContext = context;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		// initial states
		gl.glClearColor(mClearColor[0], mClearColor[1], mClearColor[2], mClearColor[3]);
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

		/*
		int[] cuts = new int[3];
		for(int x = 0; x<4; x++) {
			cuts[0] = x;
			for(int y = 0; y<4; y++) {
				cuts[1] = y;
				for(int z = 0; z<4; z++) {
					cuts[2] = z;
					try {
						Puzzle.initialize (gl, mContext, Persistance.getModelName(), cuts);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Log.d("nash","model x,y,z,cuts: " + Persistance.getModelName() + " " + x + " " + y + " " + z + " " + Puzzle.mPieces.size());
				}
			}
		}
		*/
		try {
			Puzzle.initialize (gl, mContext, Persistance.getModelName(), Persistance.getMeshCuts());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		PieceTransforms.initialize(Puzzle.mPieces.size());
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {

		mSurfaceWidth = w;
		mSurfaceHeight = h;

		/*
		 * Calculate perspective and view matrix based on best fit: 1. Assume
		 * screen held with same aspect magnitude as puzzle. 2. Make sure enough
		 * room for puzzle to stay in view regardless of rotation
		 */

		float fov = 45; // desired FOV
		float aspect = (float) w / (float) h;

		// dimensions
		float bbWidth = Puzzle.bbMax.x - Puzzle.bbMin.x;
		float bbHeight = Puzzle.bbMax.y - Puzzle.bbMin.y;

		// if puzzle width > height, assume landscape
		float fovX = fov, fovY = fov;
		if (bbWidth > bbHeight) // landscape
			fovX = (float) Math.toDegrees((2 * Math.asin(aspect * Math.toRadians(fovY) / 2)));
		else
			// portrait
			fovY = (float) Math.toDegrees((2 * Math.asin(Math.toRadians(fovX) / (aspect * 2))));

		mTanFovX = 2 * (float) Math.tan(Math.toRadians(fovX / 2));
		mTanFovY = 2 * (float) Math.tan(Math.toRadians(fovY / 2));

		// near and eyeZ based on width
		float nearW = (float) ((bbWidth / 2) / (Math.tan(Math.toRadians(fovX / 2))));
		// near and eyeZ based on height
		float nearH = (float) ((bbHeight / 2) / (Math.tan(Math.toRadians(fovY / 2))));

		float near = Math.max(nearW, nearH);
		float eyeZ = near + Puzzle.mPiece0.mDiagonal;

		// viewport
		gl.glViewport(0, 0, w, h);

		// Perspective projection
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float frustumNearHeight = Math.max(bbHeight, bbWidth);
		float[] projectionMatrix = new float[16];
		float	left = -aspect * frustumNearHeight / 2, right = aspect * frustumNearHeight / 2,
				top = frustumNearHeight / 2, bottom = -frustumNearHeight / 2,
				far = eyeZ + Puzzle.mPiece0.mDiagonal;
		Common.calcPerspectiveProjection(projectionMatrix, left, right, top, bottom, near, far);
		gl.glMultMatrixf(projectionMatrix, 0);
		WorldTransforms.setProjectionMatrix(projectionMatrix);

		// view matrix
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		float[] viewMatrix = new float[16];
		Matrix.setLookAtM(viewMatrix, 0, 0, 0, eyeZ, 0, 0, 0, 0, 1, 0);
		WorldTransforms.setViewMatrix(viewMatrix);
		WorldTransforms.init();

		Hints.initialize(gl);
		Puzzle.initializeBackdrop(gl);
	}

	public void onDrawFrame(GL10 gl) {

		// let state machine know we're ready to render and wait to proceed
		if(!mRendered) {
			((Activity)PuzzleActivity.mContext).runOnUiThread(new Runnable() {
				public void run() {
					StateMachine2.postEvent(StateMachine2.Gevent.RENDER_READY);
				}
			});
			mRendered = true;
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		Puzzle.draw(gl);
	}

	private static Context mContext;
	static boolean mRendered;
	static final float[] mClearColor = { 227f / 256f, 218f / 256f, 122f / 256f, 1 };
	static int mSurfaceWidth, mSurfaceHeight;
	static float mTanFovX, mTanFovY;
}
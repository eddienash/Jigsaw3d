package com.toychest3d.Jigsaw3d.Puzzle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import com.toychest3d.Jigsaw3d.GLutils.Point3d;
import com.toychest3d.Jigsaw3d.Installer.ZipInstaller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

class Puzzle {

	private final static int TOUCHED_PIXEL_WIDTH = 3;
	private final static float[] TOUCHED_COLOR = new float[] {1, 0, 0};
	private final static float[] TOUCHED_FACE_COLOR = new float[] {0, 1, 0};
	
	static void initialize (GL10 gl, Context context, String modelName, int[] cutsPerAxis) throws IOException {

		mContext = context;
		mNumCutsPerAxis = cutsPerAxis;
		mQinfront = new ArrayList<Piece>(); // still to be split
		mPieces = new ArrayList<Piece>(); // master list - ready to go

		// load the mesh
		LWmesh lwMesh = new LWmesh(ZipInstaller.meshDataInputStream(context, modelName));
		
		mSurfaces = lwMesh.surfaces;

		// initial piece0
		mPiece0 = new Piece(context);

		// add an interior surface
		Surface interiorSurface = new Surface(lwMesh.surfaces.get(0));
		interiorSurface.name = "interior";
		interiorSurface.textureFile = "interior.png";
		interiorSurface.idxLength = -1;
		mSurfaces.add(interiorSurface);

		// load the Vertex object array from the float array
		for (int i = 0; i < lwMesh.verts.length / Mesh.VERT_STRIDE; i++) {
			Vertex v = new Vertex(lwMesh.verts, i);
			mPiece0.mVerticies.add(v);
		}

		for (int s = 0; s < lwMesh.surfaces.size(); s++) {
			int idxStart = lwMesh.surfaces.get(s).idxStart * 3;

			for (int i = 0; i < lwMesh.surfaces.get(s).idxLength; i++) {
				int idxIdx = idxStart + i * 3;

				Vertex v0 = mPiece0.mVerticies.get(lwMesh.idxs[idxIdx++]);
				Vertex v1 = mPiece0.mVerticies.get(lwMesh.idxs[idxIdx++]);
				Vertex v2 = mPiece0.mVerticies.get(lwMesh.idxs[idxIdx++]);

				Triangle t = new Triangle(v0, v1, v2, s);
				mPiece0.mTriangles.add(t);
			}
		}

		mQinfront.add(mPiece0);
		
		cut(gl, modelName);
	}

	private static void cut(GL10 gl, String modelName) throws IOException {

		// build it just to calculate the bounding box
		mPiece0.build(gl, mSurfaces, mPiece0.mVerticies.size(), modelName);
		bbMin = new Point3d(mPiece0.bbMin.x, mPiece0.bbMin.y, mPiece0.bbMin.z);
		bbMax = new Point3d(mPiece0.bbMax.x, mPiece0.bbMax.y, mPiece0.bbMax.z);

		// make use of built surfaces to save texture duplication - reuse textureID
		for (int i = 0; i < mPiece0.mSurfaces.length; i++) {
			Surface s = mSurfaces.get(i);
			s.textureID = mPiece0.mSurfaces[i].textureID;
		}

		// **************************** Y axis ****************************
		if (mNumCutsPerAxis[1] > 0) {
			cutOneDirection(new Point3d(0, 1, 0), (bbMax.y - bbMin.y) / ((float) mNumCutsPerAxis[1] + 1f));
			// move to front queue to start over
			while (!mPieces.isEmpty())
				mQinfront.add(mPieces.remove(0));
		}

		// **************************** X axis ****************************
		if (mNumCutsPerAxis[0] > 0) {
			cutOneDirection(new Point3d(1, 0, 0), (bbMax.x - bbMin.x) / ((float) mNumCutsPerAxis[0] + 1f));
			while (!mPieces.isEmpty())
				mQinfront.add(mPieces.remove(0));
		}

		// **************************** Z axis ****************************
		if (mNumCutsPerAxis[2] > 0)
			cutOneDirection(new Point3d(0, 0, 1), (bbMax.z - bbMin.z) / ((float) mNumCutsPerAxis[2] + 1f));
		else {
			while (!mQinfront.isEmpty())
				mPieces.add(mQinfront.remove(0));
		}

		// now build them all, except interior pieces
		int i = 0;
		while (i < mPieces.size()) {
			
			Piece piece = mPieces.get(i);
			piece.build(gl, mSurfaces, mPiece0.mVerticies.size(), modelName);
			
			// toss if all interior
			boolean toss = true;
			int sLen = piece.mSurfaces.length;
			for(int sIdx=0; sIdx<sLen; sIdx++) {
				
				// not interior if any surface other than last on list
				if((piece.mSurfaces[sIdx].idxLength > 0) && (sIdx != (sLen - 1))) {
					toss = false;
					break;
				}
			}
			
			if(toss)
				mPieces.remove(i);
			else
				piece.mIndex = i++;
		}

		// break discontinuous into multiple pieces
		i = 0;
		while(i < mPieces.size()) {
			
			Piece[] ps = continuityTest(mPieces.get(i));
			
			if(ps != null) {
				
				// remove the culprit
				mPieces.remove(i);
				// discontinuous - broken into multiple
				for (Piece p : ps) {
					p.build(gl, mSurfaces, mPiece0.mVerticies.size(), modelName);
					mPieces.add(i++, p);
				}
			}
			else
				i++;
		}
		
		// renumber
		i = 0;
		for(Piece p : mPieces)
			p.mIndex = i++;

		mNumPieces = mPieces.size();
	}

	private static Piece[] continuityTest(Piece testPiece) {
		
		ArrayList<Triangle> ts = new ArrayList<Triangle> (testPiece.mTriangles);
		ArrayList< ArrayList<Triangle> > newPieces = new ArrayList< ArrayList<Triangle> >();
		
		while(!ts.isEmpty()) {

			// each "new piece" has its own list of "connected" triangles
			ArrayList<Triangle> p0 = new ArrayList<Triangle>();
			newPieces.add(p0);
			
			// start with top of original list on new piece
			p0.add(ts.remove(0));
			
			// scan all triangles on the new piece list to see if any are connected to any piece on the original list
			int pi = 0;
			while(pi < p0.size()) {

				int ti=0;
				while (ti < ts.size()) {
					
					// if connected, move from original to new
					if(connected(ts.get(ti), p0.get(pi)))
						p0.add(ts.remove(ti));
					else
						ti++;
				}
				
				pi++;
			}
		}
		
		// if everything moved, no discontinuity so nothing to do
		if(newPieces.size() == 1)
			return null;
		
		// return the new pieces
		Piece[] pieces = new Piece[newPieces.size()];
		for (int i=0; i<newPieces.size(); i++) {
			Piece p = new Piece(mContext);
			p.mTriangles = newPieces.get(i);
			
			// get all triangles in order of surface index (as expected by Piece.build )
			Collections.sort(p.mTriangles, new Comparator<Triangle>() {
				public int compare(Triangle lhs, Triangle rhs) {
					return lhs.mSurfaceIdx - rhs.mSurfaceIdx;
				}
			});
			
			pieces[i] = p;
		}
		
		return pieces;
	}

	private static boolean connected(Triangle t1, Triangle t2) {
		
		for(int i1=0; i1<3; i1++) {
			Vertex v1 = i1 == 0 ? t1.mV0 : (i1 == 1 ? t1.mV1 : t1.mV2);
			
			for(int i2=0; i2<3; i2++) {
				Vertex v2 = i2 == 0 ? t2.mV0 : (i2 == 1 ? t2.mV1 : t2.mV2);
				if(v1.equalCoords(v2))
					return true;
			}
		}
		return false;
	}

	private static void cutOneDirection(Point3d knifeNormal, float stepDistance) {

		float start = knifeNormal.dotProduct(bbMin);
		Point3d knifePoint = new Point3d(knifeNormal.scale(start));
		Point3d step = knifeNormal.scale(stepDistance);
		float nudgeDistance = stepDistance / 10;

		while (!mQinfront.isEmpty()) {

			knifePoint = knifePoint.add(step);
			Cutter cutter = new Cutter(mContext, knifePoint, knifeNormal);

			int mQsize = mQinfront.size();
			for (int p = 0; p < mQsize; p++) {

				Piece piece = mQinfront.remove(0); // get the head
				Piece[] splits = cutter.cut(piece, nudgeDistance);

				for (int i = 0; i < splits.length; i++) {

					if (splits[i].mPosition == Triangle.POSITION.BEHIND)
						mPieces.add(splits[i]);
					else
						mQinfront.add(splits[i]);
				}
			}
		}
	}

	static void draw(GL10 gl) {
		
		drawBackdrop(gl);

		if(mRenderMode == RenderMode.ALL) {

			if(StateMachine2.mState != StateMachine2.State.EXPLODING)
				Hints.draw(gl,WorldTransforms.getRotatedView());
			
			float[] trans = new float[3 * Puzzle.mPieces.size()];
			PieceTransforms.getTransforms(trans);
			for (Piece piece : mPieces) {
				
				float transX = trans[piece.mIndex * 3];
				float transY = trans[(piece.mIndex * 3) + 1];
				float transZ = trans[(piece.mIndex * 3) + 2];
				
				if(piece.mTouched)
					piece.drawBoundingBox(gl, WorldTransforms.getRotatedView(), transX, transY, transZ, TOUCHED_PIXEL_WIDTH, TOUCHED_COLOR, TOUCHED_FACE_COLOR);
				piece.draw(gl, WorldTransforms.getRotatedView(), transX, transY, transZ);
			}
		}
		
		else {

			for (Piece piece : mPieces) {
				// tiny space
				piece.draw(gl, WorldTransforms.getRotatedView(), piece.mHome.x * .05f, piece.mHome.y * .05f, piece.mHome.z * .05f);
			}
		}
	}

	static void updateHomeCount() {
		mHomeCount = 0;
		float[] trans = new float[3 * Puzzle.mPieces.size()];
		PieceTransforms.getTransforms(trans);
		for(Piece p : mPieces) {
			float transX = trans[p.mIndex * 3];
			float transY = trans[(p.mIndex * 3) + 1];
			float transZ = trans[(p.mIndex * 3) + 2];
			mHomeCount += ((transX == 0) && (transY == 0) && (transZ == 0)) ? 1 : 0;
		}
	}
	
	static void initializeBackdrop(GL10 gl) {
		
		Bitmap bmp = null;
		try {
			bmp = BitmapFactory.decodeStream(mContext.getAssets().open("backdrops/" + Persistance.mBackdrop));

			int[] textures = new int[1];
			gl.glGenTextures(1, textures, 0);
			mTextureBackdrop = textures[0];
			
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureBackdrop);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
			
			bmp.recycle();
		}
		catch (IOException e) {}
	}

	private static void drawBackdrop(GL10 gl) {
		
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureBackdrop);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

        gl.glDisable(GL10.GL_DEPTH_TEST);
		
        int crop[] = {0, 0, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight};
        ((GL11)gl).glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, crop, 0);
        ((GL11Ext)gl).glDrawTexiOES(0, 0, 1, PlayRenderer.mSurfaceWidth, PlayRenderer.mSurfaceHeight);

        gl.glEnable(GL10.GL_DEPTH_TEST);
	}

	enum RenderMode {ALL, ONE};
	static RenderMode mRenderMode;

	static Point3d bbMin, bbMax;
	static Piece mPiece0;
	static ArrayList<Piece> mPieces;
	static int mHomeCount, mNumPieces;

	private static ArrayList<Piece> mQinfront;
	private static ArrayList<Surface> mSurfaces;
	private static Context mContext;
	private static int[] mNumCutsPerAxis;
	private static int mTextureBackdrop;
}

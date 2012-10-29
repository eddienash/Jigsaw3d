package com.toychest3d.Jigsaw3d.Puzzle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import com.toychest3d.Jigsaw3d.GLutils.Point3d;
import com.toychest3d.Jigsaw3d.Installer.ZipInstaller;
import com.toychest3d.Jigsaw3d.Puzzle.PieceTransforms.FACE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.opengl.Matrix;

public class Mesh {

	public Mesh(Context context) {
		mDrawMatrix = new float[16];
		mContext = context;
	}

	public void build(GL10 gl, float[] verts, short[] idxs, ArrayList<Surface> surfaces, String modelName) throws IOException {

		loadTextures(gl, surfaces, modelName);
		
		mObjectBufNames = new int[2];
		((GL11) gl).glGenBuffers(2, mObjectBufNames, 0);
		loadVBOs(gl, mObjectBufNames, verts, idxs); 

		// compute bounding box
		bbMin = new Point3d(verts[0], verts[1], verts[2]);
		bbMax = new Point3d(verts[0], verts[1], verts[2]);
		for (int i = 0; i < verts.length; i += VERT_STRIDE) {
			bbMin.x = verts[i] < bbMin.x ? verts[i] : bbMin.x;
			bbMax.x = verts[i] > bbMax.x ? verts[i] : bbMax.x;
			bbMin.y = verts[i + 1] < bbMin.y ? verts[i + 1] : bbMin.y;
			bbMax.y = verts[i + 1] > bbMax.y ? verts[i + 1] : bbMax.y;
			bbMin.z = verts[i + 2] < bbMin.z ? verts[i + 2] : bbMin.z;
			bbMax.z = verts[i + 2] > bbMax.z ? verts[i + 2] : bbMax.z;
		}
		
		buildBoundingBox(gl);
	}
	
	private void buildBoundingBox(GL10 gl) {
		
		// 8 corners
		float[] verts = new float[] {
				bbMin.x, bbMin.y, bbMax.z, // front, lower left
				bbMax.x, bbMin.y, bbMax.z, // front, lower right 
				bbMax.x, bbMax.y, bbMax.z, // front, upper right
				bbMin.x, bbMax.y, bbMax.z, // front, upper left
				bbMin.x, bbMin.y, bbMin.z, // back, lower left
				bbMax.x, bbMin.y, bbMin.z, // back, lower right
				bbMax.x, bbMax.y, bbMin.z, // back, upper right
				bbMin.x, bbMax.y, bbMin.z // back, upper left
		};
		
		/*
		 * this index array is constructed so that the touched face will be
		 * framed in green and the other faces in red:
		 * There are 6 sets of 12 index pairs - each pair is a line
		 * The first 4 pairs are the touched face
		 */
		short[] idxs = new short[FACE.values().length * 12 * 2];
		mBBidxStart = new int[FACE.values().length];
		for (FACE face : FACE.values()) {
			switch (face) {
			case Front:
				loadBBidxs(face, new short[] {0,1,1,2,2,3,3,0, 2,6,6,5,5,1,3,7,7,4,4,0,7,6,4,5}, idxs);
				break;
			case Back:
				loadBBidxs(face, new short[] {7,4,7,6,6,5,4,5, 0,1,1,2,2,3,3,0,2,6,5,1,3,7,4,0}, idxs);
				break;
			case Top:
				loadBBidxs(face, new short[] {2,3,2,6,7,6,3,7, 0,1,1,2,3,0,6,5,5,1,7,4,4,0,4,5}, idxs);
				break;
			case Bottom:
				loadBBidxs(face, new short[] {0,1,5,1,4,5,4,0, 1,2,2,3,3,0,2,6,6,5,3,7,7,4,7,6}, idxs);
				break;
			case Left:
				loadBBidxs(face, new short[] {3,0,3,7,7,4,4,0, 0,1,1,2,2,3,2,6,6,5,5,1,7,6,4,5}, idxs);
				break;
			case Right:
				loadBBidxs(face, new short[] {1,2,2,6,6,5,5,1, 0,1,2,3,3,0,3,7,7,4,4,0,7,6,4,5}, idxs);
				break;
			}

			mBBidxStart[face.ordinal()] = face.ordinal() * 12 * 2;
		}
		
		mBBbufNames = new int[2];
		((GL11) gl).glGenBuffers(2, mBBbufNames, 0);
		loadVBOs(gl, mBBbufNames, verts, idxs); 
	}
	
	private void loadBBidxs(FACE face, short[] faceIdxs, short[] idxs) {
		for(int i=0; i<faceIdxs.length; i++)
			idxs[(face.ordinal() * 12 * 2) + i] = faceIdxs[i];
	}
	
	static void loadVBOs(GL10 gl, int[] bufNames, float[] verts, short[] idxs) {

		// VBO buffers - array buffer of all vert data
		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, bufNames[0]);
		ByteBuffer vbb = ByteBuffer.allocateDirect(verts.length * Float.SIZE / 8);
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer vertBuffer = vbb.asFloatBuffer();
		vertBuffer.put(verts);
		vertBuffer.position(0);
		((GL11) gl).glBufferData(GL11.GL_ARRAY_BUFFER, verts.length * BYTES_PER_FLOAT, vertBuffer, GL11.GL_STATIC_DRAW);

		// index buffer
		((GL11) gl).glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, bufNames[1]);
		ByteBuffer ibb = ByteBuffer.allocateDirect(idxs.length * BYTES_PER_SHORT);
		ibb.order(ByteOrder.nativeOrder());
		ShortBuffer idxBuffer = ibb.asShortBuffer();
		idxBuffer.put(idxs);
		idxBuffer.position(0);
		((GL11) gl).glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, idxs.length * BYTES_PER_SHORT, idxBuffer, GL11.GL_STATIC_DRAW);
	}
	
	private void loadTextures(GL10 gl, ArrayList<Surface> surfaces, String modelName) {
		
		mSurfaces = new Surface[surfaces.size()];
		mHasTexture = false;
		for (int i = 0; i < surfaces.size(); i++) {

			Surface surface = surfaces.get(i);
			mSurfaces[i] = surface;

			/*
			 * OpenGl wants buffer names to be unsigned. Since java has no such
			 * thing we have to consider a negative buffer name as valid and
			 * only 0 wont be used: per spec:
			 * "The value 0 is reserved to represent the default texture for each texture target."
			 */

			if ((surface.textureFile.length() > 0) && (surface.textureID == 0)) {

				try {
					Bitmap bmp = BitmapFactory.decodeStream(ZipInstaller.textureInputStream(mContext, modelName, surface.textureFile));
					int[] textures = new int[1];
					gl.glGenTextures(1, textures, 0);
					surface.textureID = textures[0];
					gl.glBindTexture(GL10.GL_TEXTURE_2D, surface.textureID);
					gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
					gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
					gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
					gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
					GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
					bmp.recycle();
					
				} catch (IOException e) {}
			}

			if (surface.textureID != 0)
				mHasTexture = true;
		}
	}

	public void draw(GL10 gl, float[] viewMatrix, float[] modelMatrix) {

		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mObjectBufNames[0]);
		((GL11) gl).glVertexPointer(3, GL10.GL_FLOAT, BYTES_PER_STRIDE, 0);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		((GL11) gl).glNormalPointer(GL10.GL_FLOAT, BYTES_PER_STRIDE, 3 * BYTES_PER_FLOAT);

		if (mHasTexture) {
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			((GL11) gl).glTexCoordPointer(2, GL10.GL_FLOAT, BYTES_PER_STRIDE, 6 * BYTES_PER_FLOAT);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_DECAL);
		} else {
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}

		Matrix.multiplyMM(mDrawMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		gl.glLoadMatrixf(mDrawMatrix, 0);

		((GL11) gl).glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mObjectBufNames[1]);
		
		for (int s = 0; s < mSurfaces.length; s++) {

			// skip surface with no triangles
			if (mSurfaces[s].idxLength > 0) {

				if (mSurfaces[s].textureID != 0) {

					// lay in the material color first
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mSurfaces[s].mAmbient, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mSurfaces[s].mDiffuse, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, mSurfaces[s].mEmissive, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mSurfaces[s].mSpecular, 0);
					gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mSurfaces[s].mShininess);
					gl.glColor4f(mSurfaces[s].mDiffuse[0], mSurfaces[s].mDiffuse[1], mSurfaces[s].mDiffuse[2], mSurfaces[s].mDiffuse[3]);
					// now lay in the texture
					gl.glEnable(GL10.GL_TEXTURE_2D);
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mSurfaces[s].textureID);
				} else {
					gl.glDisable(GL10.GL_TEXTURE_2D);
					gl.glDisable(GL10.GL_COLOR_MATERIAL);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mSurfaces[s].mAmbient, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mSurfaces[s].mDiffuse, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, mSurfaces[s].mEmissive, 0);
					gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, mSurfaces[s].mSpecular, 0);
					gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mSurfaces[s].mShininess);
					// TODO why is glColor4 here??
					gl.glColor4f(mSurfaces[s].mDiffuse[0], mSurfaces[s].mDiffuse[1], mSurfaces[s].mDiffuse[2], mSurfaces[s].mDiffuse[3]);
				}

				((GL11) gl).glDrawElements(GL10.GL_TRIANGLES, mSurfaces[s].idxLength * 3, GL10.GL_UNSIGNED_SHORT, mSurfaces[s].idxStart * 2 * 3);
			}
		}
	}

	public void drawSolidColor(GL10 gl, float[] viewMatrix, float[] modelMatrix, float[] rgb) {

		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mObjectBufNames[0]);
		((GL11) gl).glVertexPointer(3, GL10.GL_FLOAT, BYTES_PER_STRIDE, 0);
		
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		float[] drawMatrix = new float[16];
		Matrix.multiplyMM(drawMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		gl.glLoadMatrixf(drawMatrix, 0);

		((GL11) gl).glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mObjectBufNames[1]);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		gl.glColor4f(rgb[0], rgb[1], rgb[2], 1.0f);

		for (int s = 0; s < mSurfaces.length; s++) {
			((GL11) gl).glDrawElements(GL10.GL_TRIANGLES, mSurfaces[s].idxLength * 3, GL10.GL_UNSIGNED_SHORT, mSurfaces[s].idxStart * 2 * 3);
		}
	}

	public void drawBoundingBox(GL10 gl, float[] viewMatrix, float[] modelMatrix, FACE face, int lineWidth, float[] rgb, float[] touchRgb) {

		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mBBbufNames[0]);
		((GL11) gl).glVertexPointer(3, GL10.GL_FLOAT, 0, 0);

		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		float[] drawMatrix = new float[16];
		Matrix.multiplyMM(drawMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		gl.glLoadMatrixf(drawMatrix, 0);

		((GL11) gl).glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mBBbufNames[1]);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);

		gl.glLineWidth(lineWidth);
		
		if((face == null) || (touchRgb == null)) {
			gl.glColor4f(rgb[0], rgb[1], rgb[2], 1);
			((GL11) gl).glDrawElements(GL10.GL_LINES, 12 * 2, GL10.GL_UNSIGNED_SHORT, 0);
		}
		else {
			int startIdx = face.ordinal() * 2 * (12 * 2);
			
			gl.glColor4f(touchRgb[0], touchRgb[1], touchRgb[2], 1);
			((GL11) gl).glDrawElements(GL10.GL_LINES, 4 * 2, GL10.GL_UNSIGNED_SHORT, startIdx);

			gl.glColor4f(rgb[0], rgb[1], rgb[2], 1);
			((GL11) gl).glDrawElements(GL10.GL_LINES, 8 * 2, GL10.GL_UNSIGNED_SHORT, startIdx + (4 * 2 * 2));
		}
	}

	public void drawWireFrame(GL10 gl, float[] viewMatrix, float[] modelMatrix) {

		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mObjectBufNames[0]);
		((GL11) gl).glVertexPointer(3, GL10.GL_FLOAT, BYTES_PER_STRIDE, 0);

		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		float[] drawMatrix = new float[16];
		Matrix.multiplyMM(drawMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		gl.glLoadMatrixf(drawMatrix, 0);

		((GL11) gl).glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, mObjectBufNames[1]);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);

		for (int s = 0; s < mSurfaces.length; s++) {

			gl.glLineWidth(mSurfaces[s].mWireWidth);
			gl.glColor4f(mSurfaces[s].mWireframe[0], mSurfaces[s].mWireframe[1], mSurfaces[s].mWireframe[2], mSurfaces[s].mWireframe[3]);

			int idxStart = mSurfaces[s].idxStart * 2 * 3;
			int idxEnd = idxStart + mSurfaces[s].idxLength * 2 * 3;
			for (int idx = idxStart; idx < idxEnd; idx += 2 * 3) {
				((GL11) gl).glDrawElements(GL10.GL_LINE_LOOP, 3, GL10.GL_UNSIGNED_SHORT, idx);
			}
		}
	}

	public static final int VERT_STRIDE = 8; // must be coord, normal, texture U,V
	public static final int TEXTURE_HEIGHT = 128; // texture dimensions *must* be power of 2
	public static final int TEXTURE_WIDTH = 128;
	public static final int BYTES_PER_FLOAT = Float.SIZE / 8;
	public static final int BYTES_PER_SHORT = Short.SIZE / 8;
	final int BYTES_PER_STRIDE = VERT_STRIDE * BYTES_PER_FLOAT;

	public Surface[] mSurfaces;

	private int[] mObjectBufNames, mBBbufNames;
	private int[] mBBidxStart;
	private boolean mHasTexture;
	private Context mContext;

	public Point3d bbMin, bbMax;
	public float[] mDrawMatrix;
}

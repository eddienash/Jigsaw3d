package com.toychest3d.Jigsaw3d.Puzzle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;


public class Hints {
	
	private final static int HINT_DOTS_PIXEL_WIDTH = 5;
	private final static int HINT_BOXES_PIXEL_WIDTH = 3;
	private final static float[] HINT_COLOR = new float[] {.4f, .4f, .4f};

	static void initialize(GL10 gl) {
		
		mHintType = Persistance.mHint;		
		
		if(mHintType == Persistance.HINT.Dots)
			initializeDots(gl);
	}

	static void initializeDots(GL10 gl) {

		float[] verts = new float[Puzzle.mPieces.size() * 3];
		int i = 0;
		for (Piece piece : Puzzle.mPieces) {
			verts[i++] = piece.mHome.x;
			verts[i++] = piece.mHome.y;
			verts[i++] = piece.mHome.z;
		}

		// load the verts
		mHintBufName = new int[1];
		((GL11) gl).glGenBuffers(1, mHintBufName, 0);

		// VBO buffers - array buffer of all vert data
		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mHintBufName[0]);
		ByteBuffer vbb = ByteBuffer.allocateDirect(verts.length * Float.SIZE / 8);
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer vertBuffer = vbb.asFloatBuffer();
		vertBuffer.put(verts);
		vertBuffer.position(0);
		((GL11) gl).glBufferData(GL11.GL_ARRAY_BUFFER, verts.length * Mesh.BYTES_PER_FLOAT, vertBuffer, GL11.GL_STATIC_DRAW);
	}
	
	static void draw(GL10 gl, float[] rotatedView) {
		
		if(mHintType == Persistance.HINT.Boxes)
			drawBoxes(gl, rotatedView);
		else
			drawDots(gl, rotatedView);
	}
	
	static void drawBoxes(GL10 gl, float[] rotatedView) {
		
		for (Piece piece : Puzzle.mPieces) {
			// draw at home position
			piece.drawBoundingBox(gl, rotatedView, 0, 0, 0, HINT_BOXES_PIXEL_WIDTH, HINT_COLOR, null);
		}
	}

	static void drawDots(GL10 gl, float[] rotatedView) {

		((GL11) gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, mHintBufName[0]);
		((GL11) gl).glVertexPointer(3, GL10.GL_FLOAT, 0, 0);

		// no texture, no normal
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		// rotation and zoom transform
		gl.glLoadMatrixf(rotatedView, 0);

		// red hints
		gl.glColor4f(HINT_COLOR[0], HINT_COLOR[1], HINT_COLOR[2], 1.0f);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		gl.glPointSize(HINT_DOTS_PIXEL_WIDTH);
		gl.glDrawArrays(GL10.GL_POINTS, 0, Puzzle.mPieces.size());
	}
	
	private static int[] mHintBufName;
	private static Persistance.HINT mHintType;
}

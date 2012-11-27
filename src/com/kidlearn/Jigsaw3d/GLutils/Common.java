package com.kidlearn.Jigsaw3d.GLutils;

import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;



public class Common {
	
	public static void calcPerspectiveProjection(float[] m, float left, float right, float top, float bottom, float near, float far) {
		
		// from: http://www.songho.ca/opengl/gl_projectionmatrix.html#perspective
		// m[0] through m[3] is column 1, m[4] through m[7] is column 2, etc
		Arrays.fill(m, 0);
		m[0] = 2 * near / (right - left);
		m[5] = 2 * near / (top - bottom);
		m[8] = (right + left) / (right - left);
		m[9] = (top + bottom) / (top - bottom);
		m[10] = -(far + near) / (far - near);
		m[11] = -1;
		m[14] = -2 * far * near / (far-near);
	}
	
    public static void enableClientStates(GL10 gl) {
    	
		// enable client states
		gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glClearColor(227f/256f, 218f/256f, 122f/256f, 1);
        gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
    }
    
	public static int next2(int val) {
   		val--;
   		val = (val >> 1) | val;
   		val = (val >> 2) | val;
   		val = (val >> 4) | val;
   		val = (val >> 8) | val;
   		val = (val >> 16) | val;
   		return val+1;
	}
	
	public static Point3d linePlaneIntersection(Point3d lineStart, Point3d lineEnd, Point3d planePoint, Point3d planeNormal) {

		/*
		 * Calculation of intersection of plane and line segment
		 * see line-plane-intersection.doc which is derived (edited) from
		 * http://softsurfer.com/Archive/algorithm_0104/algorithm_0104B.htm#Line-Plane%20Intersection
		 * 
		 * S.P0 and S.P1 refer to start and end of line segment respectively
		 * Pn.V0 is any point on the plane, Pn.n is the normal to the plane
		 */
		
		//Vector    u = S.P1 - S.P0;
		Point3d u = new Point3d(lineEnd.x - lineStart.x, lineEnd.y - lineStart.y, lineEnd.z - lineStart.z);
		//Vector    w = S.P0 - Pn.V0;
		Point3d w = new Point3d(lineStart.x - planePoint.x, lineStart.y - planePoint.y, lineStart.z - planePoint.z);

		//float     D = dot(Pn.n, u);
		float D = planeNormal.dotProduct(u);
		//float     N = -dot(Pn.n, w);
		float N = -planeNormal.dotProduct(w);

		// parallel - should not happen - treat as miss
		if(Math.abs(D) < 1.0e-10)	// arbitrary, very small number
			return null;

		//float sI = N / D;
		float sI = N / D;
		//if (sI < 0 || sI > 1)
		if(sI < 0 || sI > 1) {
			//return 0;                       // no intersection
			return null;
		}

		//*I = S.P0 + sI * u;                 // compute segment intersect point
		u = u.scale(sI);
		Point3d hitPt = new Point3d(lineStart.x + u.x, lineStart.y + u.y, lineStart.z + u.z);
		
		return hitPt;
	}
	
}

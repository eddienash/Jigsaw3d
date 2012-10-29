package com.toychest3d.Jigsaw3d.Puzzle;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.Matrix;


import com.toychest3d.Jigsaw3d.GLutils.Point3d;
import com.toychest3d.Jigsaw3d.Puzzle.PieceTransforms.FACE;

public class Piece extends Mesh {

	Piece(Context context) {
		super(context);

		mTriangles = new ArrayList<Triangle>();
		mVerticies = new ArrayList<Vertex>();
		mOpenEdges = new ArrayList<Edge>();
	}

	void build(GL10 gl, ArrayList<Surface> surfaces, int maxVerts, String modelName) throws IOException {

		ArrayList<Vertex> newVerts = new ArrayList<Vertex>();

		short[] wasToIsMap = new short[maxVerts];
		for (int i = 0; i < wasToIsMap.length; i++)
			wasToIsMap[i] = -1;

		short[] idxs = new short[mTriangles.size() * 3];

		// clone the surfaces
		ArrayList<Surface> surfs = new ArrayList<Surface>();
		for (int i = 0; i < surfaces.size(); i++) {
			Surface s = new Surface(surfaces.get(i));
			s.idxLength = -1;
			surfs.add(s);
		}

		int thisSurf = -1;
		int triangleIdx = 0;
		Surface s = null;

		for (int i = 0; i < mTriangles.size(); i++) {

			Triangle t = mTriangles.get(i);

			if (thisSurf != t.mSurfaceIdx) {

				thisSurf = t.mSurfaceIdx;

				if (s != null)
					s.idxLength = (triangleIdx - s.idxStart * 3) / 3;

				s = surfs.get(thisSurf);
				s.idxStart = triangleIdx / 3;
			}

			idxs[triangleIdx++] = mapOneVert(wasToIsMap, newVerts, t.mV0);
			idxs[triangleIdx++] = mapOneVert(wasToIsMap, newVerts, t.mV1);
			idxs[triangleIdx++] = mapOneVert(wasToIsMap, newVerts, t.mV2);
		}

		s.idxLength = (triangleIdx - s.idxStart * 3) / 3;

		// load the verts data
		float[] verts = new float[newVerts.size() * Mesh.VERT_STRIDE];
		for (int i = 0; i < newVerts.size(); i++)
			loadOneVert(verts, i, newVerts.get(i));

		super.build(gl, verts, idxs, surfs, modelName);

		mHome = new Point3d((bbMax.x + bbMin.x) / 2, (bbMax.y + bbMin.y) / 2, (bbMax.z + bbMin.z) / 2);
		
		// Diagonal
		mDiagonal = (bbMax.x - bbMin.x) * (bbMax.x - bbMin.x);
		mDiagonal += (bbMax.y - bbMin.y) * (bbMax.y - bbMin.y);
		mDiagonal += (bbMax.z - bbMin.z) * (bbMax.z - bbMin.z);
		mDiagonal = (float) Math.sqrt(mDiagonal);
	}
	

	private short mapOneVert(short[] wasToIsMap, ArrayList<Vertex> newVerts, Vertex v) {

		int was = v.mId;

		if (wasToIsMap[was] == -1) { // new add it
			wasToIsMap[was] = (short) newVerts.size();
			newVerts.add(v);
		}

		return wasToIsMap[was];
	}

	private void loadOneVert(float[] verts, int idx, Vertex v) {

		int vertIdx = idx * Mesh.VERT_STRIDE;

		verts[vertIdx++] = v.mCoord.x;
		verts[vertIdx++] = v.mCoord.y;
		verts[vertIdx++] = v.mCoord.z;
		verts[vertIdx++] = v.mNormal.x;
		verts[vertIdx++] = v.mNormal.y;
		verts[vertIdx++] = v.mNormal.z;
		verts[vertIdx++] = v.mU;
		verts[vertIdx] = v.mV;
	}

	void draw(GL10 gl, float[] viewMatrix, float transalteX, float transalteY, float transalteZ) {
		super.draw(gl, viewMatrix, calcDrawTransform(transalteX, transalteY, transalteZ));
	}

	void drawWireFrame(GL10 gl, float[] viewMatrix, float transalteX, float transalteY, float transalteZ) {
		super.drawWireFrame(gl, viewMatrix, calcDrawTransform(transalteX, transalteY, transalteZ));
	}

	void drawSolidColor(GL10 gl, float[] viewMatrix, float transalteX, float transalteY, float transalteZ, float[] rgb) {
		super.drawSolidColor(gl, viewMatrix, calcDrawTransform(transalteX, transalteY, transalteZ), rgb);
	}

	synchronized void drawBoundingBox(GL10 gl, float[] viewMatrix, float transalteX, float transalteY, float transalteZ, int lineWidth, float[] rgb, float[] touchRgb) {
		super.drawBoundingBox(gl, viewMatrix, calcDrawTransform(transalteX, transalteY, transalteZ), mTouchedFace, lineWidth, rgb, touchRgb);
	}

	private float[] calcDrawTransform(float x, float y, float z) {
		float[] drawTransform = new float[16];
		Matrix.setIdentityM(drawTransform, 0);
		Matrix.translateM(drawTransform, 0, x, y, z);
		return drawTransform;
	}

	void createShearedSurface() {

		/*
		 * Ear clipping method of triangulation
		 * 
		 * 1. sort the edges so they are in connected order 2. eliminate
		 * colinear points (points on the same line connecting neighbors 3. grab
		 * 2 consecutive edges 4. if its an interior edge, clip the ear and add
		 * the triangle to the piece 5. if not, move to the next 3 points
		 */

		while (!mOpenEdges.isEmpty()) {

			// 1. sort the edges so they are in connected order
			ArrayList<Edge> edges = new ArrayList<Edge>();
			Edge edge = mOpenEdges.remove(0);

			Vertex head = edge.mV1;
			Vertex toMatch = edge.mV2;
			edges.add(edge);

			boolean closed = false;
			boolean found = true;

			while ((!closed) && (found)) {

				// find the match
				found = false;

				for (int i = 0; i < mOpenEdges.size(); i++) {

					edge = mOpenEdges.get(i);

					if (edge.mV1.equalCoords(toMatch)) {
						edges.add(mOpenEdges.remove(i));
						toMatch = edge.mV2;
						closed = toMatch.equalCoords(head);
						found = true;
						break;
					} else if (edge.mV2.equalCoords(toMatch)) {
						Vertex v = edge.mV1;
						edge.mV1 = edge.mV2;
						edge.mV2 = v;
						edges.add(mOpenEdges.remove(i));
						toMatch = edge.mV2;
						closed = toMatch.equalCoords(head);
						found = true;
						break;
					}
				}
			}

			// 2. eliminate colinear points (points on the same line connecting
			// neighbors
			int i = 0;
			while (i < edges.size()) {

				Edge e1 = edges.get(i);

				int next = i < edges.size() - 1 ? i + 1 : 0;
				Edge e2 = edges.get(next);

				if (colinear(e1.mV1, e1.mV2, e2.mV2)) {
					e1.mV2 = e2.mV2;
					edges.remove(next);
				} else
					i++;
			}
			
			// harvest points (needed for testing ears)
			Point3d[] testPoints = new Point3d[edges.size()];
			i = 0;
			for(Edge e : edges)
				testPoints[i++] = e.mV1.mCoord;

			// clip ears
			i = 0;
			while (edges.size() > 2) {

				// handle wrapping (second or more times around perimeter)
				i = i >= edges.size() ? 0 : i;

				// 3. grab 2 consecutive edges
				Edge e1 = edges.get(i);
				int next = i < edges.size() - 1 ? i + 1 : 0;
				Edge e2 = edges.get(next);

				/*
				 * create the new edge (provisionally) If the line from test midpoint to
				 * an edge midpoint points in the same direction (roughly) as the edge's normal
				 * (dot product >= 0), it's an interior and we're good otherwise it's
				 * an exterior (the vertex is concave so skip it
				 * 
				 * Then check to make sure it doesnt intersect with any other edge
				 */

				Edge testEdge = thirdSide(e1, e2);
				
				// vector from mid point of test edge to either side midpoint
				float midX = (testEdge.mV1.mCoord.x + testEdge.mV2.mCoord.x) / 2;
				float midY = (testEdge.mV1.mCoord.y + testEdge.mV2.mCoord.y) / 2;
				float midZ = (testEdge.mV1.mCoord.z + testEdge.mV2.mCoord.z) / 2;
				Point3d testMid = new Point3d(midX, midY, midZ);
				
				midX = (e1.mV1.mCoord.x + e1.mV2.mCoord.x) / 2;
				midY = (e1.mV1.mCoord.y + e1.mV2.mCoord.y) / 2;
				midZ = (e1.mV1.mCoord.z + e1.mV2.mCoord.z) / 2;
				Point3d e1Mid = new Point3d(midX, midY, midZ);
				
				Point3d midV = e1Mid.subtract(testMid);
				float dot = midV.dotProduct(e1.mEdgeNormal);
				
				// 4. if its an interior edge, clip the ear and add the triangle to the piece
				if (dot >= 0) {
					
					// inside, but does the new triangle surround any vertex?
					boolean intersect = false;
					for (Point3d p : testPoints) {
						if(p.equals(e1.mV1.mCoord) || p.equals(e1.mV2.mCoord) || p.equals(e2.mV2.mCoord))
							continue;
						if(pointInTriangle(e1, e2, p)) {
							intersect = true;
							break;
						}
					}

					
					if(!intersect) {
						// add the clipped triangle to the mesh
						int idx = Puzzle.mPiece0.mVerticies.size();
						Vertex v1 = new Vertex(e1.mV1.mCoord, e1.mSurfaceNormal, idx);
						calcUV(Puzzle.mPiece0.bbMin, Puzzle.mPiece0.bbMax, v1);
						Puzzle.mPiece0.mVerticies.add(v1);

						idx = Puzzle.mPiece0.mVerticies.size();
						Vertex v2 = new Vertex(e1.mV2.mCoord, e1.mSurfaceNormal, idx);
						calcUV(Puzzle.mPiece0.bbMin, Puzzle.mPiece0.bbMax, v2);
						Puzzle.mPiece0.mVerticies.add(v2);

						idx = Puzzle.mPiece0.mVerticies.size();
						Vertex v3 = new Vertex(e2.mV2.mCoord, e1.mSurfaceNormal, idx);
						calcUV(Puzzle.mPiece0.bbMin, Puzzle.mPiece0.bbMax, v3);
						Puzzle.mPiece0.mVerticies.add(v3);

						Triangle t = new Triangle(v1, v2, v3, Puzzle.mPiece0.mSurfaces.length - 1);
						mTriangles.add(t);

						// replace the 2 edges with the third
						edges.add(next, testEdge);
						edges.remove(e1);
						edges.remove(e2);
						i = edges.indexOf(testEdge);
					}
					else
						i++;
				}

				// 5. if not, move to the next
				else
					i++;
			}
		}
	}
	
	private boolean pointInTriangle(Edge e1, Edge e2, Point3d p) {
		
		/*
		 * from: http://www.blackpawn.com/texts/pointinpoly/default.html
		 * point is inside the triangle if in *all* cases same side of the edge
		 * as opposite vertex
		 */
		
		return	sameSide(p, e2.mV2.mCoord, e1.mV1.mCoord, e1.mV2.mCoord) &&
				sameSide(p, e1.mV1.mCoord, e2.mV1.mCoord, e2.mV2.mCoord) &&
				sameSide(p, e1.mV2.mCoord, e2.mV2.mCoord, e1.mV1.mCoord);
	}
	private boolean sameSide(Point3d p1, Point3d p2, Point3d a, Point3d b) {
		
		// determine if p1 and p2 are on the same side of segment a--b
		Point3d abVec = b.subtract(a);
		Point3d p1aVec = p1.subtract(a);
		Point3d p2aVec = p2.subtract(a);
		Point3d cp1 = abVec.crossProduct(p1aVec);
		Point3d cp2 = abVec.crossProduct(p2aVec);
		
		return cp1.dotProduct(cp2) >= 0;
	}
	
	private void calcUV(Point3d bbmin, Point3d bbmax, Vertex v) {

		float dU = 0, dV = 0, dVertU = 0, dVertV = 0;

		// ignore knife plane
		if (v.mNormal.x != 0) {
			dU = bbmax.z - bbmin.z;
			dV = bbmax.y - bbmin.y;
			dVertU = v.mCoord.z - bbmin.z;
			dVertV = v.mCoord.y - bbmin.y;
		} else if (v.mNormal.y != 0) {
			dU = bbmax.x - bbmin.x;
			dV = bbmax.z - bbmin.z;
			dVertU = v.mCoord.x - bbmin.x;
			dVertV = v.mCoord.z - bbmin.z;
		} else if (v.mNormal.z != 0) {
			dU = bbmax.x - bbmin.x;
			dV = bbmax.y - bbmin.y;
			dVertU = v.mCoord.x - bbmin.x;
			dVertV = v.mCoord.y - bbmin.y;
		}

		float uMult = dU > dV ? dU / dV : 1;
		float vMult = dU > dV ? 1 : dV / dU;

		v.mU = uMult * dVertU / dU;
		v.mV = vMult * dVertV / dV;
	}

	private Edge thirdSide(Edge e1, Edge e2) {

		/*
		 * from "3D Math Primer for Graphics and Game Development" page 278
		 * "13.2 Closest Point on Parametric Ray"
		 * 
		 * parametric equation of new edge: p(t) = p(org) + td
		 */

		// endpoints of the new edge
		Point3d pOrg = e1.mV1.mCoord;
		Point3d pEnd = e2.mV2.mCoord;
		Point3d q = e1.mV2.mCoord;

		// solve for direction vector d (unit length)
		Point3d d = new Point3d(pEnd);
		d = d.subtract(pOrg);
		float mag = d.magnitude();
		d = d.scale(1 / mag);

		// solve for t
		Point3d v = new Point3d(q);
		v = v.subtract(pOrg);
		float t = v.dotProduct(d);

		// q' = p(t)
		Point3d qPrimed = new Point3d(pOrg);
		d = d.scale(t);
		qPrimed = qPrimed.add(d);

		// normal of new edge (towards opposite vertex)
		Point3d normal = new Point3d(q.x - qPrimed.x, q.y - qPrimed.y, q.z - qPrimed.z);
		mag = normal.magnitude();
		normal = normal.scale(1 / mag);

		return new Edge(e1.mV1, e2.mV2, normal, e1.mSurfaceNormal);
	}

	private boolean colinear(Vertex v1, Vertex v2, Vertex v3) {

		// area of a triangle, using cross product
		Point3d u = v1.mCoord.subtract(v2.mCoord);
		Point3d v = v3.mCoord.subtract(v2.mCoord);
		Point3d cp = u.crossProduct(v);
		float mag = cp.magnitude();

		return Math.abs(mag) < mEpsilon / 10; // if this number isn't small enough you get open internal surfaces
	}

	private static final float mEpsilon = 0.0005f;
	
	Point3d mRayStart, mRayEnd, mFacePoint, mFaceNormal, mFaceIntersect;
	FACE mTouchedFace;

	float mMass;
	Point3d mV;
	ArrayList<Piece> mCurrentExplodeQ;

	int mIndex;
	float mStartX, mStartY, mPanX, mPanY; // may not need if unproject algorythm

	ArrayList<Triangle> mTriangles;
	Triangle.POSITION mPosition;
	Point3d mHome;
	float mDiagonal;

	ArrayList<Edge> mOpenEdges;
	boolean mTouched;

	// only used for piece0
	ArrayList<Vertex> mVerticies;
}

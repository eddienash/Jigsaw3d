package com.toychest3d.Jigsaw3d.Puzzle;

import java.util.ArrayList;

import android.util.Log;

import com.toychest3d.Jigsaw3d.GLutils.Point3d;

public class Trace {

	private final static boolean traceOn = false;
	private final static int traceLimit = 10000;

	static void initialize() {

		Log.d("nash", "--- Trace InitialIzing ---");

		historyScale = new ArrayList<ArrayList<Float>>();
		historyDeltaV = new ArrayList<ArrayList<float[]>>();
		history = new ArrayList<ArrayList<Point3d>>();

		for (int i = 0; i < Puzzle.mPieces.size(); i++) {
			history.add(i, new ArrayList<Point3d>());
			historyScale.add(i, new ArrayList<Float>());
			historyDeltaV.add(i, new ArrayList<float[]>());
		}

		hit1Still = new ArrayList<Integer>();
		hit1Moving = new ArrayList<Integer>();
		hit1floats = new ArrayList<float[]>();
		hit1axis = new ArrayList<PieceTransforms.AXIS>();
		hit1position = new ArrayList<PieceTransforms.POSITION>();
	}

	private static void trace(Piece p, float[] deltaV, float scale) {

		if (traceOn) {
			float[] translates = new float[3 * Puzzle.mPieces.size()];
			PieceTransforms.getTransforms(translates);
			float transalteX = translates[p.mIndex * 3];
			float transalteY = translates[(p.mIndex * 3) + 1];
			float transalteZ = translates[(p.mIndex * 3) + 2];
			Point3d p3d = new Point3d(transalteX, transalteY, transalteZ);
			if (history.get(p.mIndex).size() > traceLimit)
				initialize();

			history.get(p.mIndex).add(p3d);
			historyScale.get(p.mIndex).add(scale);
			historyDeltaV.get(p.mIndex).add(deltaV);
		}
	}

	static void traceAll(float[] deltaV, float scale) {
		for (Piece p : Puzzle.mPieces)
			trace(p, deltaV, scale);
	}

	static void traceHit(Piece moving, Piece still, float[] dVector, PieceTransforms.AXIS axis, PieceTransforms.POSITION position, float mMovingMin, float mMovingMax, float mStillMin,
			float mStillMax, float mD, float mTentry, float mTleave) {

		if (traceOn) {
			if (hit1Still.size() > traceLimit)
				initialize();

			hit1Still.add(still.mIndex);
			hit1Moving.add(moving.mIndex);
			hit1axis.add(axis);
			hit1position.add(position);
			hit1floats.add(new float[] { dVector[0], dVector[1], dVector[2], mMovingMin, mMovingMax, mStillMin, mStillMax, mD, mTentry, mTleave });
		}

	}

	static void dump(Piece still, Piece moving) {

		if (traceOn) {
			
			Piece p = still;
			ArrayList<Point3d> ps = history.get(p.mIndex);
			ArrayList<Float> ps2 = historyScale.get(p.mIndex);
			ArrayList<float[]> ps3 = historyDeltaV.get(p.mIndex);
			for (int i = ps.size() - 1; i >= 0; i--)
				Log.d("nash", "Piece: " + p.mIndex + " trace item: " + i + " translates (" + ps.get(i).x + ", " + ps.get(i).y + ", " + ps.get(i).x + ")" + " scale: " + ps2.get(i)
						+ " deltaV (" + ps3.get(i)[0] + ", " + ps3.get(i)[1] + ", " + ps3.get(i)[2] + ")");
			Log.d("nash", "------");

			p = moving;
			ps = history.get(p.mIndex);
			ps2 = historyScale.get(p.mIndex);
			ps3 = historyDeltaV.get(p.mIndex);
			for (int i = ps.size() - 1; i >= 0; i--)
				Log.d("nash", "Piece: " + p.mIndex + " trace item: " + i + " translates (" + ps.get(i).x + ", " + ps.get(i).y + ", " + ps.get(i).x + ")" + " scale: " + ps2.get(i)
						+ " deltaV (" + ps3.get(i)[0] + ", " + ps3.get(i)[1] + ", " + ps3.get(i)[2] + ")");
			Log.d("nash", "------");

			for (int i = 0; i < hit1Still.size(); i++) {

				if ((hit1Still.get(i) != still.mIndex) && (hit1Moving.get(i) != still.mIndex))
					continue;
				if ((hit1Still.get(i) != moving.mIndex) && (hit1Moving.get(i) != moving.mIndex))
					continue;
				String t = "hit():";
				t += " still: " + hit1Still.get(i);
				t += " moving: " + hit1Moving.get(i);
				t += " axis: " + hit1axis.get(i).toString();
				t += " position: " + hit1position.get(i).toString();

				float[] f = hit1floats.get(i);
				int j = 0;
				t += " dVector: (" + f[j++] + ",";
				t += " " + f[j++] + ",";
				t += " " + f[j++] + ")";

				t += " moving min: " + f[j++];
				t += " max: " + f[j++];

				t += " still min: " + f[j++];
				t += " max: " + f[j++];

				t += " mD: " + f[j++];
				t += " mTentry: " + f[j++];
				t += " mTleave: " + f[j++];
				Log.d("nash", t);
			}
		} else
			Log.d("nash", "Tracing was disabled. To turn on set traceOn=true in Trace class");
	}

	private static ArrayList<ArrayList<Point3d>> history;
	private static ArrayList<ArrayList<Float>> historyScale;
	private static ArrayList<ArrayList<float[]>> historyDeltaV;
	private static ArrayList<Integer> hit1Still;
	private static ArrayList<Integer> hit1Moving;
	private static ArrayList<float[]> hit1floats;
	private static ArrayList<PieceTransforms.AXIS> hit1axis;
	private static ArrayList<PieceTransforms.POSITION> hit1position;
}

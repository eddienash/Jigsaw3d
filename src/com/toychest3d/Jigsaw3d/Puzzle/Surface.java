package com.toychest3d.Jigsaw3d.Puzzle;

public class Surface {
	

	public Surface() {

		name = "";
		
		// default wireframe color
		mWireframe = new float[] {0, 0, 0, 1.0f};
		mWireWidth = 5;

		// material defaults
		mAmbient = new float[] {0.2f, 0.2f, 0.2f, 1.0f};
		mDiffuse = new float[] {0.8f, 0.8f, 0.8f, 1.0f};
		mSpecular = new float[] {0, 0, 0, 1};
		mEmissive = new float[] {0, 0, 0, 1};
		mShininess = 0;

		// texture
		textureFile = "";
		textureID = 0;
		textureOffset = -1;

		// polygons
		idxStart = -1;
		idxLength = -1;
	}
	
	public Surface(Surface surface) {
		
		this.ID = surface.ID;
		this.name = surface.name;
		
		// wireframe
		this.mWireframe = surface.mWireframe.clone();
		this.mWireWidth = surface.mWireWidth;
		
		// material
		this.mAmbient = surface.mAmbient.clone();
		this.mDiffuse = surface.mDiffuse.clone();
		this.mSpecular = surface.mSpecular.clone();
		this.mEmissive = surface.mEmissive.clone();
		this.mShininess = surface.mShininess;

		// texture
		this.textureFile = surface.textureFile;
		this.textureID = surface.textureID;
		this.textureOffset = surface.textureOffset;
		
		// polygons
		this.idxStart = surface.idxStart;
		this.idxLength = surface.idxLength;
	}
	
	public int ID;
	public String name;
	
	// wireframe
	public float[] mWireframe;
	public int mWireWidth;
	
	// material
	public float[] mAmbient, mDiffuse, mSpecular, mEmissive;
	public float mShininess;

	// texture
	public String textureFile;
	public int textureID, textureOffset;
	
	// polygons
	public int idxStart, idxLength;
}

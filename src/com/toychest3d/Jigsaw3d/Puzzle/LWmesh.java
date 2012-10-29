package com.toychest3d.Jigsaw3d.Puzzle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;



public class LWmesh {
	
	public LWmesh(DataInputStream lwoDis) throws IOException {
		
		version = lwoDis.readInt();
		
		int vertsLength = lwoDis.readInt();
		verts = new float[vertsLength];
		for(int i=0; i<verts.length; i++)
			verts[i] = lwoDis.readFloat();

		int idxsLength = lwoDis.readInt();
		idxs = new short[idxsLength];
		for(int i=0; i<idxs.length; i++)
			idxs[i] = lwoDis.readShort();
		
		int surfacesLength = lwoDis.readInt();
		surfaces = new ArrayList<Surface>(surfacesLength);
		for(int i=0; i<surfacesLength; i++) {
			
			Surface surf = new Surface();
			
			surf.name = lwoDis.readUTF();
			surf.textureFile = lwoDis.readUTF();
			readRGBA(lwoDis, surf.mDiffuse);
			readRGBA(lwoDis, surf.mSpecular);
			readRGBA(lwoDis, surf.mEmissive);
			surf.mShininess = lwoDis.readFloat();
			surf.idxStart = lwoDis.readInt();
			surf.idxLength = lwoDis.readInt();

			surfaces.add(i, surf);
		}
	}
	
	public LWmesh(float[] verts, short[] idxs, ArrayList<Surface> surfaces) {
		
		this.verts = verts;
		this.idxs = idxs;
		this.surfaces = surfaces;
	}
	
	public void dump(DataOutputStream lwoDos) throws IOException {
		
		lwoDos.writeInt(LWMESH_VERSION);
		
		lwoDos.writeInt(verts.length);
		for(int i=0; i<verts.length; i++)
			lwoDos.writeFloat(verts[i]);

		lwoDos.writeInt(idxs.length);
		for(int i=0; i<idxs.length; i++)
			lwoDos.writeShort(idxs[i]);
		
		lwoDos.writeInt(surfaces.size());
		for(int i=0; i<surfaces.size(); i++) {
			
			Surface surf = surfaces.get(i);
			
			lwoDos.writeUTF(surf.name);
			System.out.println(surf.name);
			lwoDos.writeUTF(surf.textureFile);
			writeRGBA(lwoDos, surf.mDiffuse);
			writeRGBA(lwoDos, surf.mSpecular);
			writeRGBA(lwoDos, surf.mEmissive);
			lwoDos.writeFloat(surf.mShininess);
			lwoDos.writeInt(surf.idxStart);
			lwoDos.writeInt(surf.idxLength);
		}
	}
	
	private void writeRGBA(DataOutputStream lwoDos, float[] rgba) throws IOException {
		for(int i=0; i<rgba.length; i++)
			lwoDos.writeFloat(rgba[i]);
	}
	private void readRGBA(DataInputStream lwoDis, float[] rgba) throws IOException {
		for(int i=0; i<rgba.length; i++)
			rgba[i] = lwoDis.readFloat();
	}
	
	public int LWMESH_VERSION = 1;
	public int version;
	public float[] verts;
	public short[] idxs;
	public ArrayList<Surface> surfaces;
}

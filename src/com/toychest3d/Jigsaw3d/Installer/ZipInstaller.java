package com.toychest3d.Jigsaw3d.Installer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Puzzle.Persistance;


import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;

public class ZipInstaller {

	final static String puzzlesAssetsDirectory = "puzzles";

	public static boolean installIncluded(Context context) {

		// get all the zip files (1 per puzzle)
		AssetManager mgr = context.getAssets();

		try {
			// all files in assets/puzzles directory
			String[] puzzleFiles = mgr.list(puzzlesAssetsDirectory);

			for (String zipFile : puzzleFiles) {
				// make sure they are zip files (at least by extension)
				if (zipFile.indexOf(".zip") > 0) {
					doZip(context, zipFile);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private static void doZip(Context context, String zipFile) throws IOException, NameNotFoundException {

		String modelName = zipFile.replace(".zip", "");

		InputStream is = new DataInputStream(context.getAssets().open(puzzlesAssetsDirectory + "/" + zipFile));
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

		for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {

			// mesh file goes to internal storage and extra processing
			if (ze.getName().indexOf(".lwx") >= 0)
				saveMeshData(context, zis, modelName);

			// anything else on SD card as is
			else
				saveNonMeshData(context, zis, ze, modelName);
		}
		
		// set if default model, easy difficulty, 1 cut for x,y & z
		if(modelName.equals(context.getResources().getString(R.string.sel_default_model_name))) {
			Persistance.saveNewPuzzle(modelName, new int[] {1,1,1}, 0);
		}
	}

	private static void saveMeshData(Context context, ZipInputStream zis, String modelName) throws NameNotFoundException, IOException {

		DataOutputStream os = new DataOutputStream(context.openFileOutput(modelName + ".mesh", Context.MODE_PRIVATE));

		// save version
		os.writeByte(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);

		// save dog tag
		byte[] dogtag = DogTag.get(context);
		os.writeByte(dogtag.length);
		os.write(dogtag, 0, dogtag.length);

		byte[] buffer = new byte[1024];
		int count;
		while ((count = zis.read(buffer)) != -1)
			os.write(buffer, 0, count);

		os.close();
	}

	private static void saveNonMeshData(Context context, ZipInputStream zis, ZipEntry ze, String modelName) throws IOException {

		DataOutputStream os = new DataOutputStream(context.openFileOutput(modelName + "_" + ze.getName(), Context.MODE_PRIVATE));

		byte[] buffer = new byte[1024];
		int count;
		while ((count = zis.read(buffer)) != -1)
			os.write(buffer, 0, count);

		os.close();
	}

	public static DataInputStream meshDataInputStream(Context context, String modelName) throws IOException {

		// open the mesh file
		DataInputStream is = new DataInputStream(context.openFileInput(modelName + ".mesh"));

		// toss the dogtag data
		is.readByte(); // version
		int dogTagLength = is.read();
		for (int i = 0; i < dogTagLength; i++)
			is.readByte();

		return is;
	}

	public static DataInputStream textureInputStream(Context context, String modelName, String textureFile) throws IOException {

		// texture file in internal storage
		return new DataInputStream(context.openFileInput(modelName + "_" + textureFile));
	}

	public static byte[] getDogTag(Context context, String modelName) throws IOException {

		// open the mesh file
		DataInputStream is = new DataInputStream(context.openFileInput(modelName + ".mesh"));

		// toss the dogtag data
		is.read(); // version
		int dogTagLength = is.read();

		byte[] tag = new byte[dogTagLength];

		for (int i = 0; i < dogTagLength; i++)
			tag[i] = is.readByte();

		return tag;
	}
	
	public static void reInstall(Context context) {
		
		// clear out the mesh files
		String[] meshes = context.fileList();
		for(String mesh: meshes) {
			if(mesh.indexOf(".mesh") >= 0)
				context.deleteFile(mesh);
		}
		
		// clear out sd card files
		for(String mesh: meshes) {
			
			if(mesh.indexOf(".mesh") >= 0) {
							
				File fullPath = new File(context.getExternalFilesDir(null), mesh.replace(".mesh", ""));
				String[] files = fullPath.list();
				
				for(String file : files)
					new File(fullPath + "/" + file).delete();
				
				fullPath.delete();
			}
		}
		
		// install
		ZipInstaller.installIncluded(context);
	}
}

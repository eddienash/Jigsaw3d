package com.toychest3d.Jigsaw3d.Installer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class DogTag {
	
	public static byte[] get(Context context) {
		
		 String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		 id = id == null? "" : id;
		 
		 String ser = Build.SERIAL == null ? "" : Build.SERIAL;

		 String clear = id + ser;
		 
		 MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1"); // SHA-1 or MD5
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		
		 return md.digest(clear.getBytes());
	}
	
	public static boolean check(Context context, byte[] checkTag) {
		
		byte[] tag = get(context);
		if(tag.length != checkTag.length)
			return false;
		
		for(int i=0; i<tag.length; i++) {
			if(tag[i] != checkTag[i])
				return false;
		}
		
		return true;
	}
}

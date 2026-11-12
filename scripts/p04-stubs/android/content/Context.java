package android.content;
import android.content.pm.PackageManager;
public class Context {
 public String getPackageName(){return "com.instagram.android";}
 public PackageManager getPackageManager(){return null;}
 public Context getApplicationContext(){return this;}
 public ClassLoader getClassLoader(){return getClass().getClassLoader();}
}

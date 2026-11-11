package android.app;
import android.content.Context;
import android.view.Window;
public class Activity extends Context {
 public Window getWindow(){return null;}
 public boolean isFinishing(){return false;}
 public boolean isDestroyed(){return false;}
 public void onResume(){}
 public void onPause(){}
 public void onDestroy(){}
}

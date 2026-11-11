package android.content;
public class Context {
 public static final String ACTIVITY_SERVICE="activity";
 public Context getApplicationContext(){return this;}
 public Object getSystemService(String name){return null;}
}

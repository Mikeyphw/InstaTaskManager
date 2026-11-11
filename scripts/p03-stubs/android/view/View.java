package android.view;
/** TEST-ONLY stub, never shipped in the APK. */
public class View {
    public static final int VISIBLE=0, INVISIBLE=4, GONE=8;
    private int visibility=VISIBLE;
    private CharSequence description;
    private boolean listener;
    public int getVisibility(){return visibility;}
    public void setVisibility(int v){visibility=v;}
    public CharSequence getContentDescription(){return description;}
    public void setContentDescription(CharSequence s){description=s;}
    public boolean hasOnClickListeners(){return listener;}
    public void setHasOnClickListeners(boolean value){listener=value;}
}

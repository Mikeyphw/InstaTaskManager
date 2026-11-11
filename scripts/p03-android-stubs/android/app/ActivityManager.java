package android.app;
import android.os.Debug;
import java.util.List;
public class ActivityManager {
 public static class RunningAppProcessInfo {public int uid;public int pid;public String processName;}
 public List<RunningAppProcessInfo> getRunningAppProcesses(){return null;}
 public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids){return null;}
}

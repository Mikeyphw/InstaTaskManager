package io.github.libxposed.api;
import java.lang.reflect.Method;
public class XposedModule {
 public static class ModuleLoadedParam {public String getProcessName(){return null;}}
 public static class PackageReadyParam {
  public String getPackageName(){return null;}
 }
 public interface Chain {Object proceed() throws Throwable; Object getThisObject();}
 public interface Interceptor {Object intercept(Chain chain) throws Throwable;}
 public static class Builder {
  public Builder setId(String id){return this;}
  public Builder setExceptionMode(XposedInterface.ExceptionMode m){return this;}
  public void intercept(Interceptor i){}
 }
 protected Builder hook(Method m){return new Builder();}
 public void onModuleLoaded(ModuleLoadedParam p){}
 public void onPackageReady(PackageReadyParam p){}
}

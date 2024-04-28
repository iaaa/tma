 Group location tracking project 
 ===============================

 Prerequisites
 -------------

 * [Android SDK](https://developer.android.com/studio/index.html#command-line-tools-only) (Android Studio is not needed).  
   Used `/opt/android/sdk` path by default, can be changed using `ANDROID_SDK` environment variable.
 * Android build-tools version 29.0.2 is needed.  
   Used `$(ANDROID_SDK)/build-tools/29.0.2` path by default, can be changed directly at the top of `Makefile`.

Issues
------
* `dx: can't find dx.jar` error:  
  copy `dx.jar` from `$(ANDROID_SDK)/build-tools/27.0.3/lib/` (or any similar existing) into `$(ANDROID_SDK)/build-tools/29.0.2/lib/`

Build
-----

```shell
$ make build
```

It should create `debug.final.apk` which is ready to be installed using `adb install` or directly on the device.

Makefile has few target to simplify app developing: `make debug` builds, installs, and runs app.
Additionally, `make install`, `make uninstall`, `make start`, `make stop`, and `make restart` are available.

Do a `make clean` to clear the build artifacts from the folder.

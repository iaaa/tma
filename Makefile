.PHONY: all clean
.PHONY: build debug
.PHONY: install uninstall start stop restart
.PHONY: xxx
all: build

## prerequisites
PLATFORM ?= android-30

ANDROID_SDK?=/opt/android/sdk
ANDROID_NDK?=/opt/android/ndk
BUILD_TOOLS?=$(ANDROID_SDK)/build-tools/33.0.0
OL_SRC_ROOT?=thirdparty/ol

ifeq ("$(wildcard $(ANDROID_SDK)/)","")
$(error ANDROID_SDK not set or invalid!)
endif

ifeq ("$(wildcard $(ANDROID_NDK)/)","")
$(error ANDROID_NDK not set or invalid!)
endif

## project build structure
$(shell mkdir -p obj dex lib)

## android builds
SHELL := /bin/sh
PATH = $(shell printenv PATH):$(ANDROID_SDK)/platform-tools

## ol thirdparty module
$(OL_SRC_ROOT)/Makefile:
	git clone https://github.com/otus-lisp/ol $(OL_SRC_ROOT)
	$(MAKE) -C $(OL_SRC_ROOT) android

## build java project
dex/classes.dex: $(shell find src/ -name '*.java')
	mkdir -p res # generate resources
	$(BUILD_TOOLS)/aapt package -f -m \
	      -S res -J src -M AndroidManifest.xml \
	      -I $(ANDROID_SDK)/platforms/$(PLATFORM)/android.jar
	mkdir -p obj # compile java files
	cd obj; unzip -o ../libs/snakeyaml-android-1.8.jar -x "META-INF/*"; cd ..
	javac -verbose -source 1.8 -target 1.8 -d obj \
	      -bootclasspath jre/lib/rt.jar \
	      -classpath $(ANDROID_SDK)/platforms/$(PLATFORM)/android.jar:obj:libs/snakeyaml-android-1.8.jar \
	      -sourcepath src $^
	mkdir -p dex # create classes.dex
	$(BUILD_TOOLS)/dx --verbose --dex --output=$@ obj

debug.apk: dex/classes.dex debug.keystore \
           $(OL_SRC_ROOT)/Makefile
	# making package
	$(BUILD_TOOLS)/aapt package -f \
	      -M AndroidManifest.xml -S res -A assets \
	      -I $(ANDROID_SDK)/platforms/$(PLATFORM)/android.jar \
	      -F $@ dex
	# copying olvm libraries
	find $(OL_SRC_ROOT)/libs/ -type f -name *olvm*.so -printf '%p lib/%P\n'| xargs --max-lines=1 install -D
	$(BUILD_TOOLS)/aapt add $@ `find -L lib/ -name *.so`

debug.final.apk: debug.apk
	$(BUILD_TOOLS)/zipalign -f 4 debug.apk debug.final.apk
	jarsigner -keystore debug.keystore -storepass debug33 -keypass debug33 \
	          -signedjar $@ $@ projectKey

debug.keystore:
	keytool -genkeypair -validity 1000 \
	        -dname "CN=debug,O=Android,C=ES" \
	        -keystore $@ \
	        -storepass 'debug33' -keypass 'debug33' \
	        -alias projectKey -keyalg RSA

# build automation
build: debug.final.apk
clean:
	rm -rf dex/* lib/* obj/*
	find src -name "R.java" -exec rm {} \;
	rm -f debug.apk debug.final.apk

install: build
	adb -d install debug.final.apk
	# grant default permission(s)
	adb shell pm grant com.track.my.ass android.permission.READ_EXTERNAL_STORAGE
uninstall:
	adb -d uninstall com.track.my.ass

# run and stop app on device
start:
	adb shell am start -n com.track.my.ass/com.track.my.ass.TheActivity
stop:
	adb shell am force-stop com.track.my.ass
restart:
	$(MAKE) stop
	$(MAKE) start

debug:
	$(MAKE) build
	$(MAKE) install
	$(MAKE) start

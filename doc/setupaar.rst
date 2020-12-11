Setup GroundSdk AAR
===================

The easiest way to use GroundSdk is to setup your project to use
Android Archive Libraries (AAR) from `jcenter <https://bintray.com/bintray/jcenter>`__.

For this purpose, edit your application `build.gradle` file to add
GroundSdk Android dependencies:

.. code-block:: groovy

    repositories {
        // Add jcenter repository (if not already present)
        jcenter()
    }

    dependencies {
        // Add GroundSdk dependencies
        implementation 'com.parrot.drone.groundsdk:groundsdk:{{version}}'
        runtimeOnly 'com.parrot.drone.groundsdk:arsdkengine:{{version}}'
    }

.. note:: you may replace {{version}} by the GroundSdk version you want to use.

This allows to download and link GroundSdk AARs to your project.

You also need to make your project compatible with GroundSdk:

- Increase minimum Android SDK version supported by your project to 24.
- Add Java 8 compilation compatibility.

In the same file:

.. code-block:: groovy

    android {
        defaultConfig {
            // Set minimum SDK version supported by GroundSdk
            minSdkVersion 24
        }

        // Add java 8 compatibility needed by GroundSdk
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }

Your project setup now is ready.

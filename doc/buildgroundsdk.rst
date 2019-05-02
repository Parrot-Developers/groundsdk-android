Install and build GroundSdk
===========================

Environment setup
-----------------

Linux
^^^^^

GroundSdk Android has been tested on Ubuntu 18.04.
You should adapt some commands to your Linux distribution.

Download the latest `Android Studio`_, and unpack it to ``/opt``

.. note:: GroundSdk has been successfully tested with Android Studio 3.4

Follow the `installation instructions for Linux`_, which include downloading Android SDK components.

Download `Android NDK Revision 17c for Linux`_, and unpack it to ``/opt``

.. note:: GroundSdk will not build with newer NDK revisions. That's why you
   *should not* install NDK from Android Studio SDK Manager.

   Note that you can still have several NDK revisions installed on your computer,
   you just have to reference the 17c revision as described below.

Permanently set environment variables (this will modify your ``~/.profile`` file) with the following commands

.. code-block:: console

    $ echo export JAVA_HOME=/opt/android-studio/jre >> ~/.profile
    $ echo export ANDROID_HOME=~/Android/Sdk >> ~/.profile
    $ echo export ANDROID_SDK_PATH=~/Android/Sdk >> ~/.profile
    $ echo export ANDROID_NDK_PATH=/opt/android-ndk-r17c >> ~/.profile

.. note:: You should use the Android Studio embedded JDK as shown in this example.

   You can find the default Android SDK path in Android Studio
   settings, in Appearance & Behavior > System Settings > Android SDK

Reload the ``.profile`` file to take your changes into account

.. code-block:: console

    $ . ~/.profile

Install the required tools

.. code-block:: console

    $ sudo apt install git python python3 make pkg-config

Install the **Repo** tool

   -  Download it from `Google APIs`_
   -  Put it in a directory that is included in your path
   -  Make it executable with ``chmod a+x repo``

Configure git with your real name and email address

.. code-block:: console

    $ git config --global user.name "Your Name"
    $ git config --global user.email "you@example.com"

You're ready to `clone the GroundSdk workspace`_.

.. _Android Studio: https://developer.android.com/studio/
.. _installation instructions for Linux: https://developer.android.com/studio/install#linux
.. _Android NDK Revision 17c for Linux: https://dl.google.com/android/repository/android-ndk-r17c-linux-x86_64.zip
.. _Google APIs: https://storage.googleapis.com/git-repo-downloads/repo

Mac OS
^^^^^^

GroundSdk Android has been tested on macOS 10.14.4 Mojave.

Download the latest `Android Studio`_

.. note:: GroundSdk has been successfully tested with Android Studio 3.4

Follow the `installation instructions for Mac`_, which include downloading Android SDK components.

Download `Android NDK Revision 17c for Mac`_, and copy the unpacked ``android-ndk-r17c`` directory to ``/usr/local``

.. note:: GroundSdk will not build with newer NDK revisions. That's why you
   *should not* install NDK from Android Studio SDK Manager.

   Note that you can still have several NDK revisions installed on your computer,
   you just have to reference the 17c revision as described below.

Permanently set environment variables (this will modify your ``~/.bash_profile`` file) with the following commands

.. code-block:: console

    $ echo export JAVA_HOME=\"/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home\" >> ~/.bash_profile
    $ echo export ANDROID_HOME=~/Library/Android/sdk >> ~/.bash_profile
    $ echo export ANDROID_SDK_PATH=~/Library/Android/sdk >> ~/.bash_profile
    $ echo export ANDROID_NDK_PATH=/usr/local/android-ndk-r17c >> ~/.bash_profile

.. note:: You should use the Android Studio embedded JDK as shown in this example.

   You can find the default Android SDK path in Android Studio
   settings, in Appearance & Behavior > System Settings > Android SDK

Reload the ``.bash_profile`` file to take your changes into account

.. code-block:: console

    $ . ~/.bash_profile

Install Homebrew

.. code-block:: console

    $ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

Install the required tools using Homebrew

.. code-block:: console

    $ brew install git repo python3 pkgconfig

Configure git with your real name and email address

.. code-block:: console

    $ git config --global user.name "Your Name"
    $ git config --global user.email "you@example.com"

.. _installation instructions for Mac: https://developer.android.com/studio/install#mac
.. _Android NDK Revision 17c for Mac: https://dl.google.com/android/repository/android-ndk-r17c-darwin-x86_64.zip

Clone the GroundSdk workspace
-----------------------------

Create your working directory

.. code-block:: console

    $ mkdir groundsdk
    $ cd groundsdk

Initialize Repo in your working directory

.. code-block:: console

    $ repo init -u https://github.com/Parrot-Developers/groundsdk-manifest

.. note:: You can learn how to use Repo on the `Repo command reference page`_

Download the GroundSdk source tree

.. code-block:: console

    $ repo sync

.. _Repo command reference page: https://source.android.com/setup/develop/repo

Build GroundSdk
---------------

Type the following command to build GroundSdk for Android, including the Demo application

.. code-block:: console

    $ ./build.sh -p groundsdk-android -t build -j

.. note:: You can run ``./build.sh --help`` to learn more about the building options

Run GroundSdk Demo
------------------

#. Launch Android Studio (on Linux, run ``/opt/android-studio/bin/studio.sh``)
#. Click on "Open an existing Android Studio project"
#. Select the ``groundsdk/groundsdk-android`` directory and click *OK* / *Open*
#. Connect an Android device to your computer
#. Run ``groundsdkdemo`` application

Connect to your drone
---------------------

#. Switch on your drone
#. Open wifi settings on your Android device
#. Select your drone's wifi access point (e.g. ANAFI-xxxxxxx)
#. Open GroundSdk Demo app
#. Select the DRONES tab
#. Your drone should appear in the list, select it
#. Click on CONNECT

 .. image:: media/demo.png
    :alt: GroundSdk Demo

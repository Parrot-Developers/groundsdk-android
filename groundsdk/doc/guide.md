[Guide](guide.md)
[Javadoc](javadoc/)

GroundSdk Quick Start Guide
=========

> _This is a preview version of GroundSdk. All API may be subject to changes!_

To run the Demo Application:

- connect your android device through USB,
- in `demo` directory, run `./gradlew installDebug`.

To open the Demo Application project:

- start AndroidStudio,
- select import Project, and pick the `build.gradle` file located in the `demo` directory.

Overview
--------

GroundSdk is composed of 3 modules:

- __groundsdk__: SDK API and its implementation,
- __arsdkengine__: SDK engines based on ARSDK,
- __sdkcore__: native code wrapper.

Application should only use `groundsdk` module.
Furthermore, all classes and interfaces located inside the `internal` package of `groundsdk` should not be used by the
application.

Main entry point is the class [[GroundSdk]], which provides API to access [[Drone]] and [[RemoteControl]] objects.
`groundsdk` maintains a list of known and available drones and remote control.

Application can obtain an instance of GroundSdk by creating a [new session](GroundSdk#newSession).
Groundsdk internal engine is started when the first GroundSdk session is created and stopped when the last GroundSdk
session is [closed](GroundSdk#close).

Drone and RemoteControl classes
-------------------------------

The [[Drone]] class represents a drone of any [model](Drone#getModel).
It is uniquely identified by a persistent [UID](Drone#getUid) and has a [name](Drone#getName\(\)) and a
[state](Drone#getState\(\)).

The [[RemoteControl]] class represent a remote control (for example, MPP) of any [model](RemoteControl#getModel).
It is uniquely identified by a persistent [UID](RemoteControl#getUid), and has a [name](RemoteControl#getName\(\)) and
a [state](RemoteControl#getState\(\)).

Those 2 classes are also containers of `component` objects representing parts of the drone or remote control.
A component has properties storing the current state and info of the represented element and provides methods to act on
it.

There are 3 types of `components`:

- __[[Instrument]]__: components that provides telemetry information.
Can be [obtained](Instrument.Provider#getInstrument\(Class\)) from a drone or remote control.
- __[[Peripheral]]__: components for accessory functions.
Can be [obtained](Peripheral.Provider#getPeripheral\(Class\)) from a drone or remote control.
- __[[PilotingItf]]__: components that allows to pilot the drone.
Can be [obtained](PilotingItf.Provider#getPilotingItf\(Class\)) from a drone.

Each drone and remote control have a subset of all available components. For example if a drone has a GPS it will
contains the [[Gps]] instrument.

Some components are available when the drone is disconnected, some only when the drone or remote control is connected.

Facilities
----------

[[GroundSdk]] provides a set of global [services](Facility) that the application can
[access](GroundSdk#getFacility\(Class\)).
An example of facility is [[com.parrot.drone.groundsdk.facility.FirmwareManager]] that handle the download of firmware
updates from a cloud server.

One of the main facility is [[AutoConnection]]. This facility connects a remote control or a drone as soon as
it is available.

Notifications
-------------

Application can receive a notification when the state or properties of a component change, by using access method that
accept an [observer](Ref.Observer) and return a [reference](Ref).

Such methods are:

- [getInstrument](Instrument.Provider#getInstrument\(Class,Ref.Observer\)),
- [getPeripheral](Peripheral.Provider#getPeripheral\(Class,Ref.Observer\)),
- [getPilotingItf](PilotingItf.Provider#getPilotingItf\(Class,Ref.Observer\)),
- [getFacility](GroundSdk#getFacility\(Class,Ref.Observer\)).

The registered observer is called when the component becomes available, when its state or properties change, and when
it becomes unavailable.

The returned reference can be used to [unregister](Ref#close) the observer. The reference is linked to the groundsdk
session from which it has been requested; it is also closed automatically when the session gets
[closed](GroundSdk#close).

Once the reference is closed, the observer is unregistered and won't receive any further change notifications.

Configuration
-------------

GroundSdk can be configured by adding specific declarations in the application resources.

Below is groundsdk `config.xml`. Any config value may be overridden by the application in its own
`config.xml` file.

```
<resources>
    <!-- Application key -->
    <string name="gsdk_application_key"/>

    <!-- Tells whether connection using local wifi is enabled -->
    <bool name="gsdk_wifi_enabled">true</bool>

    <!-- Tells whether connection using usb device (i.e. Sky Controller) is enabled -->
    <bool name="gsdk_usb_enabled">true</bool>

    <!-- Tells whether connection using "Usb Debug Bridge" is enabled -->
    <bool name="gsdk_usb_debug_enabled">false</bool>

    <!-- Tells whether connection using ble is enabled (Ble support is deprecated) -->
    <bool name="gsdk_ble_enabled">false</bool>

    <!-- Tells whether dev tool box is enabled -->
    <bool name="gsdk_dev_toolbox_enabled">false</bool>

    <!-- Tells whether drone settings ares stored locally and sent to the drone when connecting. Values are:
         OFF: don't store offline settings
         MODEL: store settings, values are shared for all devices of the same model -->
    <string name="gsdk_offline_settings_mode">MODEL</string>

    <!-- Tells whether GroundSdk USB/RC accessory bootstrap activity is enabled. -->
    <bool name="gsdk_rc_accessory_bootstrap_activity_enabled">true</bool>

    <!-- Tells whether GPS ephemeris synchronization is enabled. -->
    <bool name="gsdk_ephemeris_sync_enabled">true</bool>
    <!-- Tells whether crash report synchronization is enabled. -->
    <bool name="gsdk_crash_report_enabled">true</bool>

    <!-- Space quota for crash report synchronization, in bytes. Value 0 disables quota.
         Quota is checked when groundsdk first starts. Eldest crash report files are deleted until the total size
         of all collected crash report files is lower than this quota. -->
    <integer name="gsdk_crash_report_quota">0</integer>

    <!-- Tells whether device firmware synchronization is enabled. -->
    <bool name="gsdk_firmware_enabled">true</bool>

    <!-- Alternate firmware server to use. Empty to use the default server. -->
    <string name="gsdk_firmware_server"/>

    <!-- Tells whether blackbox synchronization is enabled. -->
    <bool name="gsdk_blackbox_enabled">true</bool>

    <!-- Space quota for blackbox synchronization, in bytes. Value 0 disables quota.
         Quota is checked when groundsdk first starts. Eldest blackbox files are deleted until the total size
         of all collected blackbox files is lower than this quota. -->
    <integer name="gsdk_blackbox_quota">0</integer>

    <!-- Tells whether flight data synchronization is enabled. -->
    <bool name="gsdk_flight_data_enabled">true</bool>

    <!-- Space quota for flight data synchronization, in bytes. Value 0 disables quota.
         Quota is checked when groundsdk first starts. Eldest flight data files are deleted until the total size
         of all collected flight data files is lower than this quota. -->
    <integer name="gsdk_flight_data_quota">0</integer>

    <!-- Tells whether flight log synchronization is enabled. -->
    <bool name="gsdk_flight_log_enabled">true</bool>

    <!-- Space quota for flight log synchronization, in bytes. Value 0 disables quota.
         Quota is checked when groundsdk first starts. Eldest flight log files are deleted until the total size
         of all collected flight log files is lower than this quota. -->
    <integer name="gsdk_flight_log_quota">0</integer>

    <!-- Tells whether video decoding is enabled. -->
    <bool name="gsdk_video_decoding_enabled">true</bool>

    <!-- Defines the set of device models to be supported.
         Leaving this value empty commands GroundSdk to support all known device models; otherwise, GroundSdk will
         explicitly ignore any device whose model is not listed in this array.
         Each item in the array must be the name from either the Drone.Model or the RemoteControl.Model enum, in
         upper-case. For instance:
         <item>ANAFI_4K</item>
         <item>SKYCONTROLLER_3</item>
         ... -->
    <string-array name="gsdk_supported_devices"/>

    <!-- Tells whether automatic devices connection will be started automatically each time the first GroundSdk session
         is started. -->
    <bool name="gsdk_auto_connection_at_startup">false</bool>

    <!-- Tells whether wifi access point country will automatically be selected by reverse geocoding system location.
         If true:
         - the wifi country setting is changed each time a new country is detected from the current location, and it
         cannot be changed from the API
         - the environment is forced to outdoor, and it cannot be changed from the API. -->
    <bool name="gsdk_auto_select_wifi_country">true</bool>

    <!-- Default country code that will be returned by the reverse geocoder.
         Empty to not return a default country code. -->
    <string name="gsdk_reverse_geocoder_default_country_code"/>

    <!-- Maximum size for the in-memory cache gsdk maintains for media thumbnails, in bytes. -->
    <integer name="gsdk_media_thumbnail_cache_size">0</integer>

</resources>
```

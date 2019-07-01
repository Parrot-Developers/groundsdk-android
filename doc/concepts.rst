Concepts
========

GroundSdk is composed of 3 modules:

* **groundsdk**: SDK API and its implementation,
* **arsdkengine**: SDK engines based on ARSDK,
* **sdkcore**: native code wrapper.

The application should only use groundsdk module.
Furthermore, all classes and interfaces located inside the **internal** package of groundsdk module should not be used by the application.

Main entry point is the class **GroundSdk**, which provides API to access **Drone** and **RemoteControl** objects.
GroundSdk maintains a list of known and available drones and remote controls.

The application can obtain an instance of GroundSdk by requesting a new session. It may create as many sessions as deemed appropriate; however each session must also be closed when not needed anymore.
Groundsdk internal engine is started when the first GroundSdk session is created and stopped when the last GroundSdk session is closed.

Drone and RemoteControl classes
-------------------------------

The **Drone** class represents a drone of any model.
It is uniquely identified by a persistent UID and has a name and a current state.

Similarly, the **RemoteControl** class represent a remote control of any model.
It is uniquely identified by a persistent UID, and has a name and a current state.

Those 2 classes are also containers of **component** objects representing parts of the drone or remote control.
A component has properties storing the current state and information of the represented element and provides methods to act on it.

There are 3 types of components:

* **Instrument**: components that provides telemetry information,
* **Peripheral**: components for accessory functions,
* **PilotingItf**: components that allows to pilot the drone (and as such are not available for remote controls).

Each drone and remote control features a subset of all available components. For example if a drone has a GPS it will contains the **Gps** instrument.
Some components are available when the device (drone or remote control) is disconnected, some only when the device is connected.

Facilities
----------

GroundSdk also provides a set of global services that the application can access. Such services are represented by the **Facility** class.

An example of facility is **FirmwareManager** that handle the download of firmware updates from a cloud server.
Another example of facility is **AutoConnection**. This facility connects a remote control or a drone as soon as it is available.

Notifications
-------------

The application can receive a notification when the state or properties of a component or facility change, by using accessor method that accept an observer object, represented by **Ref.Observer** interface, and return a reference to the component, through the **Ref** class.

The registered observer is called when the component becomes available, when its state or properties change, and when it becomes unavailable.

The returned reference can be used to unregister the observer. The reference is linked to the GroundSdk session from which it has been requested; it is also closed automatically when the session gets closed.
Once the reference is closed, the observer is unregistered and won't receive any further change notifications.

Configuration
-------------

GroundSdk can be configured by adding specific declarations in the application resources.

Below is groundsdk ``config.xml``. Any config value may be overridden by the application in its own ``config.xml`` file.

.. literalinclude:: config.xml
    :language: xml
    :lines: 1,33-

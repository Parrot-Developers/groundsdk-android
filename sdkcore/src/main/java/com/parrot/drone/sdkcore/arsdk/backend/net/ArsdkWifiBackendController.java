/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.drone.sdkcore.arsdk.backend.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.net.mdnssdmin.MdnsSdMin;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Objects;

import javax.net.SocketFactory;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_NET;

/**
 * Wifi backend controller.
 */
public final class ArsdkWifiBackendController extends ArsdkBackendController {

    /**
     * Instantiates a new Arsdk WIFI backend controller.
     *
     * @param context            android context
     * @param discoverableModels list of discoverable models
     *
     * @return a new Arsdk WIFI backend controller instance, suitable for use on the current device
     */
    @NonNull
    public static ArsdkBackendController create(@NonNull Context context, @ArsdkDevice.Type int[] discoverableModels) {
        return new ArsdkWifiBackendController(context, discoverableModels);
    }

    /** Connectivity manager used to register network callbacks. */
    private final ConnectivityManager mConnectivityManager;

    /** Multicast lock. */
    private final WifiManager.MulticastLock mMulticastLock;

    /** Wifi lock. */
    private final WifiManager.WifiLock mWifiLock;

    /** current network, null if there is no current network. */
    private Network mNetwork;

    /** Backend. */
    private ArsdkNetBackend mBackend;

    /** Discovery. */
    private ArsdkNetDiscoveryMdnsSd mDiscovery;

    /** list of Wifi discoverable models. */
    @ArsdkDevice.Type
    private final int[] mDiscoverableModels;

    /**
     * Constructor.
     *
     * @param context            android context.
     * @param discoverableModels list of discoverable models
     */
    private ArsdkWifiBackendController(@NonNull Context context, @ArsdkDevice.Type int[] discoverableModels) {
        mDiscoverableModels = discoverableModels;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock("WifiInterface");
        mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WifiInterface");
    }

    /**
     * Retrieves the socket factory to used to create sockets bound to the network managed by this controller.
     *
     * @return the bound socket factory for this controller, or {@code null} if bound sockets are not supported by this
     *         controller
     */
    @Nullable
    public SocketFactory getSocketFactory() {
        return mNetwork == null ? null : mNetwork.getSocketFactory();
    }

    @Override
    protected void onStart() {
        if (ULog.d(TAG_NET)) {
            ULog.d(TAG_NET, "Starting wifi backend controller");
        }
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
        mConnectivityManager.registerNetworkCallback(networkRequestBuilder.build(), mNetworkCallback);
    }

    @Override
    protected void onStop() {
        if (ULog.d(TAG_NET)) {
            ULog.d(TAG_NET, "Stopping wifi backend controller");
        }
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        if (mNetwork != null) {
            onNetworkLost();
            mNetwork = null;
        }
    }

    /**
     * Called when network is available, creates and starts the net backend and discovery.
     */
    private void onNetworkAvailable() {
        if (mArsdkCore == null) {
            // should never happen, callback are removed when sopped
            return;
        }
        // get the interface of the current network
        LinkProperties linkProperties = mConnectivityManager.getLinkProperties(mNetwork);
        NetworkInterface networkInterface = null;
        if (linkProperties != null) {
            String interfaceName = linkProperties.getInterfaceName();
            if (interfaceName != null) {
                try {
                    networkInterface = NetworkInterface.getByName(linkProperties.getInterfaceName());
                } catch (SocketException e) {
                    ULog.e(TAG_NET, "Error getting interface name", e);
                }
            }
        }

        if (networkInterface != null) {
            if (ULog.d(TAG_NET)) {
                ULog.d(TAG_NET, "Wifi Network available, starting backend and discovery");
            }
            mMulticastLock.acquire();
            mWifiLock.acquire();
            // create backend and discovery
            mBackend = new ArsdkNetBackend(this, mArsdkCore, mSocketListener);
            mDiscovery = new ArsdkNetDiscoveryMdnsSd(mArsdkCore, mBackend, mDiscoverableModels,
                    new MdnsSdMin.NetConfig(networkInterface, mNetwork));
            mDiscovery.start();
        }
    }

    /**
     * Called when network becomes unavailable, stops and destroys the net backend and discovery.
     */
    private void onNetworkLost() {
        if (ULog.d(TAG_NET)) {
            ULog.d(TAG_NET, "Wifi Network lost, stopping backend and discovery");
        }
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        if (mMulticastLock.isHeld()) {
            mMulticastLock.release();
        }
        if (mDiscovery != null) {
            mDiscovery.stop();
            mDiscovery.destroy();
        }
        if (mBackend != null) {
            mBackend.destroy();
        }
        mDiscovery = null;
        mBackend = null;
    }

    /** Binds sockets created in native code to the current network. */
    private final ArsdkNetBackend.Listener mSocketListener = new ArsdkNetBackend.Listener() {

        @Override
        public void onSocketCreated(int socketFd) {
            if (ULog.d(TAG_NET)) {
                ULog.d(TAG_NET, "Binding socket to network [fd: " + socketFd + "]");
            }
            assert mNetwork != null;
            ParcelFileDescriptor parcelFd = ParcelFileDescriptor.adoptFd(socketFd);
            FileDescriptor fd = parcelFd.getFileDescriptor();
            try {
                mNetwork.bindSocket(fd);
            } catch (IOException e) {
                ULog.w(TAG_NET, "Could not bind native socket to network [fd: " + socketFd + "]", e);
            } finally {
                parcelFd.detachFd();
            }
        }
    };

    /**
     * Connectivity manager network callback.
     */
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(Network network) {
            if (mArsdkCore == null) {
                return;
            }
            mArsdkCore.dispatchToPomp(() -> {
                if (mNetwork == null) {
                    mNetwork = network;
                    onNetworkAvailable();
                }
            });
        }

        @Override
        public void onLost(Network network) {
            if (mArsdkCore == null) {
                return;
            }
            mArsdkCore.dispatchToPomp(() -> {
                if (Objects.equals(mNetwork, network)) {
                    mNetwork = null;
                    onNetworkLost();
                }
            });
        }
    };
}

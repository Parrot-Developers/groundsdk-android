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

package com.parrot.drone.groundsdk.arsdkengine.persistence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.ulog.ULog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/**
 * Defines a storage entry by associating a storage key and parsing and serialization functions to write and read the
 * specific value stored in a {@link PersistentStore.Dictionary} at that key.
 *
 * @param <T> type of stored value
 */
public abstract class StorageEntry<T> {

    /**
     * Creates a new storage entry instance for a string value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<String> ofString(@NonNull String key) {
        return of(key, Function.identity(), Function.identity());
    }


    /**
     * Creates a new storage entry instance for an integer value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<Integer> ofInteger(@NonNull String key) {
        return StorageEntry.<Integer, Number>of(key, Number::intValue, Function.identity());
    }

    /**
     * Creates a new storage entry instance for a long value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<Long> ofLong(@NonNull String key) {
        return StorageEntry.<Long, Number>of(key, Number::longValue, Function.identity());
    }

    /**
     * Creates a new storage entry instance for a double value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<Double> ofDouble(@NonNull String key) {
        return StorageEntry.<Double, Number>of(key, Number::doubleValue, Function.identity());
    }

    /**
     * Creates a new storage entry instance for a boolean value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<Boolean> ofBoolean(@NonNull String key) {
        return of(key, Function.identity(), Function.identity());
    }

    /**
     * Creates a new storage entry instance for a double range value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<DoubleRange> ofDoubleRange(@NonNull String key) {
        return of(key, Converter::parseDoubleRange, Converter::serializeDoubleRange);
    }

    /**
     * Creates a new storage entry instance for an integer range value.
     *
     * @param key key to the value in the storage
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static StorageEntry<IntegerRange> ofIntegerRange(@NonNull String key) {
        return of(key, Converter::parseIntegerRange, Converter::serializeIntegerRange);
    }

    /**
     * Creates a new storage entry instance for an enum value.
     *
     * @param key  key to the value in the storage
     * @param type class of the stored enum type
     * @param <E>  stored enum type
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static <E extends Enum<E>> StorageEntry<E> ofEnum(@NonNull String key, @NonNull Class<E> type) {
        return of(key,
                (String serializedEnum) -> Converter.parseEnum(serializedEnum, type),
                Converter::serializeEnum);
    }

    /**
     * Creates a new storage entry instance for an enum set.
     *
     * @param key  key to the value in the storage
     * @param type class of the stored enum type
     * @param <E>  stored enum type
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static <E extends Enum<E>> StorageEntry<EnumSet<E>> ofEnumSet(@NonNull String key, @NonNull Class<E> type) {
        return of(key,
                (JSONArray serializedEnumSet) -> Converter.parseEnumSet(serializedEnumSet, type),
                Converter::serializeEnumSet);
    }

    /**
     * Creates a new storage entry instance for a map of enum to enum.
     *
     * @param key       key to the value in the storage
     * @param keyType   class of the stored map key type
     * @param valueType class of the stored map value type
     * @param <K>       stored map key type
     * @param <V>       stored map value type
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static <K extends Enum<K>, V extends Enum<V>> StorageEntry<EnumMap<K, V>> ofEnumMap(
            @NonNull String key, @NonNull Class<K> keyType, @NonNull Class<V> valueType) {
        return of(key,
                (JSONObject serializedEnumMap) -> Converter.parseEnumMap(serializedEnumMap, keyType, valueType),
                Converter::serializeEnumMap);
    }

    /**
     * Creates a new storage entry instance for a map of enum to double.
     *
     * @param key     key to the value in the storage
     * @param keyType class of the stored map key type
     * @param <K>     stored map key type
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static <K extends Enum<K>> StorageEntry<EnumMap<K, Double>> ofEnumToDoubleMap(
            @NonNull String key, @NonNull Class<K> keyType) {
        return of(key,
                (JSONObject serializedEnumMap) -> Converter.parseEnumToDoubleMap(serializedEnumMap, keyType),
                Converter::serializeEnumToDoubleMap);
    }

    /**
     * Creates a new storage entry instance for a map of enum to double range.
     *
     * @param key     key to the value in the storage
     * @param keyType class of the stored map key type
     * @param <K>     stored map key type
     *
     * @return a new {@code StorageEntry} instance
     */
    @NonNull
    public static <K extends Enum<K>> StorageEntry<EnumMap<K, DoubleRange>> ofEnumToDoubleRangeMap(
            @NonNull String key, @NonNull Class<K> keyType) {
        return of(key,
                (JSONObject serializedEnumMap) -> Converter.parseEnumToDoubleRangeMap(serializedEnumMap, keyType),
                Converter::serializeEnumToDoubleRangeMap);
    }

    /** Key to the stored value in storage. */
    @NonNull
    private final String mKey;

    /**
     * Constructor for subclassing.
     *
     * @param key stored value key
     */
    protected StorageEntry(@NonNull String key) {
        mKey = key;
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when parsing errors can be considered unimportant and the stored value can be considered
     * absent instead.
     *
     * @param store storage dictionary to load the value from, may be {@code null}
     *
     * @return the stored value, if any, or {@code null} if none, or the provided {@code store} is {@code null}, or
     *         parsing failed
     *
     * @see #loadOrThrow(PersistentStore.Dictionary)
     * @see #load(PersistentStore.Dictionary, Supplier)
     * @see #load(PersistentStore.Dictionary, Object)
     */
    @Nullable
    public final T load(@Nullable PersistentStore.Dictionary store) {
        Object serializedObject = store == null ? null : store.getObject(mKey);
        if (serializedObject != null) {
            try {
                return parse(serializedObject);
            } catch (IllegalArgumentException e) {
                ULog.w(TAG, "Storage Entry: error parsing stored value: " + serializedObject, e);
            }
        }
        return null;
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when parsing error must be handled in a custom, appropriate way by the caller.
     *
     * @param store storage dictionary to load the value from, may be {@code null}
     *
     * @return the stored value, if any, or {@code null} if none, or the provided {@code store} is {@code null}
     *
     * @throws IllegalArgumentException in case a value is stored but could not be parsed successfully
     * @see #load(PersistentStore.Dictionary)
     * @see #loadOrThrow(PersistentStore.Dictionary, Supplier)
     * @see #load(PersistentStore.Dictionary, Object)
     */
    @Nullable
    public final T loadOrThrow(@Nullable PersistentStore.Dictionary store) {
        Object serializedObject = store == null ? null : store.getObject(mKey);
        return serializedObject == null ? null : parse(serializedObject);
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when the fallback value needs to be computed and/or instantiated. For static fallback
     * values, always prefer {@link #load(PersistentStore.Dictionary, Object)}, which will most likely be less
     * expansive.
     * <p>
     * This method is for use when parsing errors can be considered unimportant and the stored value can be replaced by
     * some fallback value instead.
     *
     * @param store            storage dictionary to load the value from, may be {@code null}
     * @param fallbackSupplier called to provide a fallback in case the value is absent, could not be loaded, or the
     *                         provided {@code store} is {@code null}. This supplier <strong>MUST NOT</strong> return a
     *                         {@code null} value
     *
     * @return the stored value, if any, or the result of {@code fallbackSupplier.get()} if none, or the provided {@code
     *         store} is {@code null}, or parsing failed
     *
     * @see #loadOrThrow(PersistentStore.Dictionary, Supplier)
     * @see #load(PersistentStore.Dictionary)
     * @see #load(PersistentStore.Dictionary, Object)
     */
    @NonNull
    public final T load(@Nullable PersistentStore.Dictionary store, @NonNull Supplier<? extends T> fallbackSupplier) {
        T value = load(store);
        if (value == null) {
            value = fallbackSupplier.get();
        }
        if (value == null) {
            throw new IllegalArgumentException("fallbackSupplier must not return null");
        }
        return value;
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when the fallback value needs to be computed and/or instantiated. For static fallback
     * values, always prefer {@link #load(PersistentStore.Dictionary, Object)}, which will most likely be less
     * expansive.
     * <p>
     * This method is for use when parsing error must be handled in a custom, appropriate way by the caller.
     *
     * @param store            storage dictionary to load the value from, may be {@code null}
     * @param fallbackSupplier called to provide a fallback in case the value is absent or the provided {@code store} is
     *                         {@code null}. This supplier <strong>MUST NOT</strong> return a {@code null} value
     *
     * @return the stored value, if any, or the result of {@code fallbackSupplier.get()} if none, or the provided {@code
     *         store} is {@code null}
     *
     * @throws IllegalArgumentException in case a value is stored but could not be parsed successfully
     * @see #load(PersistentStore.Dictionary, Supplier)
     * @see #loadOrThrow(PersistentStore.Dictionary)
     * @see #loadOrThrow(PersistentStore.Dictionary, Object)
     */
    @NonNull
    public final T loadOrThrow(@Nullable PersistentStore.Dictionary store,
                               @NonNull Supplier<? extends T> fallbackSupplier) {
        T value = loadOrThrow(store);
        if (value == null) {
            value = fallbackSupplier.get();
        }
        if (value == null) {
            throw new IllegalArgumentException("fallbackSupplier must not return null");
        }
        return value;
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when the fallback is a static value. For fallback values that need to be computed and/or
     * instantiated, always prefer {@link #load(PersistentStore.Dictionary, Supplier)} which lazily invokes the fallback
     * supplier only if really needed.
     * <p>
     * This method is for use when parsing errors can be considered unimportant and the stored value can be replaced by
     * some fallback value instead.
     *
     * @param store    storage dictionary to load the value from, may be {@code null}
     * @param fallback fallback to return case the value is absent, could not be loaded, or the provided {@code store}
     *                 is {@code null}.
     *
     * @return the stored value, if any, or {@code fallback} if none, or the provided {@code store} is {@code null}, or
     *         parsing failed
     *
     * @see #loadOrThrow(PersistentStore.Dictionary, Object)
     * @see #load(PersistentStore.Dictionary)
     * @see #load(PersistentStore.Dictionary, Supplier)
     */
    @NonNull
    public final T load(@Nullable PersistentStore.Dictionary store, @NonNull T fallback) {
        T value = load(store);
        return value == null ? fallback : value;
    }

    /**
     * Loads the value from the given store.
     * <p>
     * This method is for use when the fallback is a static value. For fallback values that need to be computed and/or
     * instantiated, always prefer {@link #load(PersistentStore.Dictionary, Supplier)} which lazily invokes the fallback
     * supplier only if really needed.
     * <p>
     * This method is for use when parsing error must be handled in a custom, appropriate way by the caller.
     *
     * @param store    storage dictionary to load the value from, may be {@code null}
     * @param fallback fallback to return case the value is absent or the provided {@code store} is {@code null}.
     *
     * @return the stored value, if any, or {@code fallback} if none, or the provided {@code store} is {@code null}
     *
     * @throws IllegalArgumentException in case a value is stored but could not be parsed successfully
     * @see #load(PersistentStore.Dictionary, Object)
     * @see #loadOrThrow(PersistentStore.Dictionary)
     * @see #loadOrThrow(PersistentStore.Dictionary, Supplier)
     */
    @NonNull
    public final T loadOrThrow(@Nullable PersistentStore.Dictionary store, @NonNull T fallback) {
        T value = loadOrThrow(store);
        return value == null ? fallback : value;
    }

    /**
     * Saves the given value to the given store.
     * <p>
     * Does nothing unless provided {@code value} and {@code store} are non-{@code null}.
     *
     * @param store storage dictionary to save the value into, may be {@code null}
     * @param value value to store, may be {@code null}
     */
    public final void save(@Nullable PersistentStore.Dictionary store, @Nullable T value) {
        if (store != null && value != null) {
            store.put(mKey, serialize(value)).commit();
        }
    }

    /**
     * Creates the stored value from its storage json representation.
     * <p>
     * The {@code serializedObject} must be a valid json entity, see {@link JSONObject#put(String, Object)} for
     * restrictions.
     *
     * @param serializedObject json representation of the value to parse
     *
     * @return the parsed value
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    protected abstract T parse(@NonNull Object serializedObject);

    /**
     * Serializes the given value to its storage json representation.
     *
     * @param object value to serialize
     *
     * @return the serialized json representation of the value
     */
    @NonNull
    protected abstract Object serialize(@NonNull T object);

    /**
     * Creates a new storage entry.
     *
     * @param key        storage entry key
     * @param parser     function used to parse the storage json representation to the appropriate value
     * @param serializer function used to serialize a value to its storage json representation
     * @param <T>        type of the stored value
     * @param <U>        type of raw JSON value read from store (JSONObject, JSONArray, String, Boolean, Number)
     *
     * @return a new {@code StorageEntry} instance
     */
    private static <T, U> StorageEntry<T> of(@NonNull String key, @NonNull Function<? super U, ? extends T> parser,
                                             @NonNull Function<? super T, ? extends U> serializer) {
        return new StorageEntry<T>(key) {

            @SuppressWarnings("unchecked") // trust the caller
            @NonNull
            @Override
            protected T parse(@NonNull Object serializedObject) {
                return parser.apply((U) serializedObject);
            }

            @NonNull
            @Override
            protected Object serialize(@NonNull T object) {
                return serializer.apply(object);
            }
        };
    }
}

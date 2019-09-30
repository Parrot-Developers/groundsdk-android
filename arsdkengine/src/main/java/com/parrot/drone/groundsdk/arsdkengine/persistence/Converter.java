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

import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.ulog.ULog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_STORAGE;

/**
 * Utility class providing various parsing and serialization methods for common values.
 */
public final class Converter {

    /**
     * Serializes a double range.
     *
     * @param range double range to serialize
     *
     * @return a JSONArray representation of the given range, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static JSONArray serializeDoubleRange(@NonNull DoubleRange range) {
        JSONArray serializedArray = new JSONArray();
        try {
            serializedArray.put(range.getLower());
            serializedArray.put(range.getUpper());
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return serializedArray;
    }

    /**
     * Parses a JSONArray representation of a double range.
     *
     * @param serializedRange JSONArray representation of the double range to parse
     *
     * @return the corresponding double range
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    public static DoubleRange parseDoubleRange(@NonNull JSONArray serializedRange) {
        try {
            return DoubleRange.of(serializedRange.getDouble(0), serializedRange.getDouble(1));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serializes an integer range.
     *
     * @param range integer range to serialize
     *
     * @return a JSONArray representation of the given range, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static JSONArray serializeIntegerRange(@NonNull IntegerRange range) {
        return new JSONArray()
                .put(range.getLower())
                .put(range.getUpper());
    }

    /**
     * Parses a JSONArray representation of an integer range.
     *
     * @param serializedRange JSONArray representation of the integer range to parse
     *
     * @return the corresponding integer range
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    public static IntegerRange parseIntegerRange(@NonNull JSONArray serializedRange) {
        try {
            return IntegerRange.of(serializedRange.getInt(0), serializedRange.getInt(1));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serializes an enum.
     *
     * @param value enum value to serialize
     * @param <E>   type of the enum value
     *
     * @return a String representation of the given enum, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <E extends Enum<E>> String serializeEnum(@NonNull E value) {
        return value.name();
    }

    /**
     * Parses a string representation of an enum.
     *
     * @param serializedEnum string representation of the enum to parse
     * @param type           class of the enum type
     * @param <E>            type of the enum
     *
     * @return the corresponding enum value
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <E extends Enum<E>> E parseEnum(@NonNull String serializedEnum, @NonNull Class<E> type) {
        return Enum.valueOf(type, serializedEnum);
    }

    /**
     * Serializes an enum set.
     *
     * @param set enum set to serialize
     * @param <E> type of enums in the set
     *
     * @return a JSONArray representation of the given enum set, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <E extends Enum<E>> JSONArray serializeEnumSet(@NonNull Set<E> set) {
        return serializeCollection(set, Converter::serializeEnum);
    }

    /**
     * Parses a JSONArray representation of an enum set.
     *
     * @param serializedEnumSet JSONArray representation of the enum set to parse
     * @param type              class of enum type in the set
     * @param <E>               type of enum in the set
     *
     * @return the corresponding enum set
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <E extends Enum<E>> EnumSet<E> parseEnumSet(@NonNull JSONArray serializedEnumSet,
                                                              @NonNull Class<E> type) {
        return parseCollection(serializedEnumSet,
                () -> EnumSet.noneOf(type),
                (serializedEnum) -> parseEnum((String) serializedEnum, type));
    }

    /**
     * Serializes a map of enums to enums.
     *
     * @param map enum map to serialize
     * @param <K> type of enums used as map keys
     * @param <V> type of enums used as map values
     *
     * @return a JSONObject representation of the given enum map, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <K extends Enum<K>, V extends Enum<V>> JSONObject serializeEnumMap(@NonNull EnumMap<K, V> map) {
        return serializeMap(map, Converter::serializeEnum, Converter::serializeEnum);
    }

    /**
     * Parses a JSONObject representation of a map of enums to enums.
     *
     * @param serializedEnumMap JSONObject representation of the enum map to parse
     * @param keyType           class of enum type used as map keys
     * @param valueType         class of enum type used as map values
     * @param <K>               type of enum used as map keys
     * @param <V>               type of enum used as map values
     *
     * @return the corresponding enum map
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <K extends Enum<K>, V extends Enum<V>> EnumMap<K, V> parseEnumMap(
            @NonNull JSONObject serializedEnumMap, @NonNull Class<K> keyType, @NonNull Class<V> valueType) {
        return parseMap(serializedEnumMap,
                () -> new EnumMap<>(keyType),
                (serializedKey) -> parseEnum(serializedKey, keyType),
                (serializedValue) -> parseEnum((String) serializedValue, valueType));
    }

    /**
     * Serializes a map of enums to doubles.
     *
     * @param map enum map to serialize
     * @param <K> type of enums used as map keys
     *
     * @return a JSONObject representation of the given enum map, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <K extends Enum<K>> JSONObject serializeEnumToDoubleMap(@NonNull EnumMap<K, Double> map) {
        return serializeMap(map, Converter::serializeEnum, Function.identity());
    }

    /**
     * Parses a JSONObject representation of a map of enums to doubles.
     *
     * @param serializedEnumMap JSONObject representation of the enum map to parse
     * @param keyType           class of enum type used as map keys
     * @param <K>               type of enum used as map keys
     *
     * @return the corresponding enum map
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <K extends Enum<K>> EnumMap<K, Double> parseEnumToDoubleMap(
            @NonNull JSONObject serializedEnumMap, @NonNull Class<K> keyType) {
        return parseMap(serializedEnumMap,
                () -> new EnumMap<>(keyType),
                (serializedKey) -> parseEnum(serializedKey, keyType),
                Number::doubleValue);
    }

    /**
     * Serializes a map of enums to double ranges.
     *
     * @param map enum map to serialize
     * @param <K> type of enums used as map keys
     *
     * @return a JSONObject representation of the given enum map, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <K extends Enum<K>> JSONObject serializeEnumToDoubleRangeMap(@NonNull EnumMap<K, DoubleRange> map) {
        return serializeMap(map, Converter::serializeEnum, Converter::serializeDoubleRange);
    }

    /**
     * Parses a JSONObject representation of a map of enums to double ranges.
     *
     * @param serializedEnumMap JSONObject representation of the enum map to parse
     * @param keyType           class of enum type used as map keys
     * @param <K>               type of enum used as map keys
     *
     * @return the corresponding enum map
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <K extends Enum<K>> EnumMap<K, DoubleRange> parseEnumToDoubleRangeMap(
            @NonNull JSONObject serializedEnumMap, @NonNull Class<K> keyType) {
        return parseMap(serializedEnumMap,
                () -> new EnumMap<>(keyType),
                (serializedKey) -> parseEnum(serializedKey, keyType),
                (serializedValue) -> parseDoubleRange((JSONArray) serializedValue));
    }

    /**
     * Serializes a collection of values.
     *
     * @param collection      collection to serialize
     * @param valueSerializer function that serialize collection elements
     * @param <T>             type of elements in the collection
     *
     * @return a JSONArray representation of the given collection, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <T> JSONArray serializeCollection(@NonNull Collection<T> collection,
                                                    @NonNull Function<T, ?> valueSerializer) {
        JSONArray serializedArray = new JSONArray();
        for (T value : collection) {
            serializedArray.put(valueSerializer.apply(value));
        }
        return serializedArray;
    }

    /**
     * Parses a JSONArray representation of a collection of values.
     *
     * @param serializedCollection JSONArray representation of the collection to parse
     * @param collectionFactory    function that creates the destination collection
     * @param elementParser        function that parses each element's representation
     * @param <T>                  type of elements in the collection
     * @param <C>                  type of the returned collection
     * @param <SE>                 type of serialized elements
     *
     * @return the corresponding collection
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <T, C extends Collection<T>, SE> C parseCollection(@NonNull JSONArray serializedCollection,
                                                                     @NonNull Supplier<C> collectionFactory,
                                                                     @NonNull Function<SE, T> elementParser) {
        try {
            C collection = collectionFactory.get();
            for (int i = 0, N = serializedCollection.length(); i < N; i++) {
                try {
                    // trust the caller, otherwise log the failure and drop the item
                    @SuppressWarnings("unchecked")
                    SE serializedElement = (SE) serializedCollection.get(i);
                    collection.add(elementParser.apply(serializedElement));
                } catch (IllegalArgumentException | ClassCastException e) {
                    if (ULog.w(TAG_STORAGE)) {
                        ULog.w(TAG_STORAGE, "Could not parse array element [index: " + i + "] in: "
                                            + serializedCollection, e);
                    }
                }
            }
            return collection;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Serializes a map of keys to values.
     *
     * @param map             map to serialize
     * @param keySerializer   function that serialize map keys as strings
     * @param valueSerializer function that serialize map values
     * @param <K>             type of map keys
     * @param <V>             type of map values
     *
     * @return a JSONObject representation of the given map, suitable for insertion in a {@link
     *         PersistentStore.Dictionary}.
     */
    @NonNull
    public static <K, V> JSONObject serializeMap(@NonNull Map<K, V> map, @NonNull Function<K, String> keySerializer,
                                                 @NonNull Function<V, ?> valueSerializer) {
        JSONObject serializedMap = new JSONObject();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            String serializedKey = keySerializer.apply(entry.getKey());
            Object serializedValue = valueSerializer.apply(entry.getValue());
            try {
                serializedMap.put(serializedKey, serializedValue);
            } catch (JSONException e) {
                throw new AssertionError(e); // never happens
            }
        }
        return serializedMap;
    }

    /**
     * Parses a JSONObject representation of a map of keys to values.
     *
     * @param serializedMap JSONObject representation of the map to parse
     * @param mapFactory    function that creates the destination map
     * @param keyParser     function that parses each key's string representation
     * @param valueParser   function that parses each value's string representation
     * @param <K>           type of map keys
     * @param <V>           type of map values
     * @param <M>           type of the returned map
     * @param <SV>          type of serialized values
     *
     * @return the corresponding map
     *
     * @throws IllegalArgumentException in case parsing failed
     */
    @NonNull
    public static <K, V, M extends Map<K, V>, SV> M parseMap(@NonNull JSONObject serializedMap,
                                                             @NonNull Supplier<M> mapFactory,
                                                             @NonNull Function<String, K> keyParser,
                                                             @NonNull Function<SV, V> valueParser) {
        try {
            M map = mapFactory.get();
            for (Iterator<String> keyIter = serializedMap.keys(); keyIter.hasNext(); ) {
                String serializedKey = keyIter.next();

                try {
                    K key = keyParser.apply(serializedKey);
                    // trust the caller, otherwise log the failure and drop the item
                    @SuppressWarnings("unchecked")
                    SV serializedValue = (SV) serializedMap.get(serializedKey);
                    V value = valueParser.apply(serializedValue);
                    map.put(key, value);
                } catch (IllegalArgumentException | ClassCastException e) {
                    if (ULog.w(TAG_STORAGE)) {
                        ULog.w(TAG_STORAGE, "Could not parse map element [key: " + serializedKey + "] in: "
                                            + serializedMap, e);
                    }
                }
            }
            return map;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private Converter() {
    }
}

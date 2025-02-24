/*
 * Copyright 2012-2018 Chronicle Map Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.map;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.util.SerializableFunction;
import net.openhft.chronicle.hash.ChronicleHash;
import net.openhft.chronicle.hash.serialization.SizedReader;
import net.openhft.chronicle.hash.serialization.SizedWriter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * {@code ChronicleMap} provides concurrent access to a <i>Chronicle Map key-value store</i> from a
 * JVM process.
 * <p>
 * For information on <ul> <li>how to construct a {@code ChronicleMap}</li> <li>{@code
 * ChronicleMap} flavors and properties</li> <li>available configurations</li> </ul> see {@link
 * ChronicleMapBuilder} documentation.
 * <p>
 * Functionally this interface defines some methods supporting garbage-free off-heap programming:
 * {@link #getUsing(Object, Object)}, {@link #acquireUsing(Object, Object)}.
 * <p>
 * Roughly speaking, {@code ChronicleMap} compares keys and values by their binary serialized
 * form, that shouldn't necessary be the same equality relation as defined by built-in {@link
 * Object#equals(Object)} method, which is prescribed by general {@link Map} contract.
 * <p>
 * Note that {@code ChronicleMap} extends {@link Closeable}, don't forget to {@linkplain #close()
 * close} map when it is no longer needed.
 *
 * @param <K> the map key type
 * @param <V> the map value type
 * @see ChronicleMapBuilder#create()
 * @see ChronicleMapBuilder#createPersistedTo(File)
 */
public interface ChronicleMap<K, V> extends ConcurrentMap<K, V>,
        ChronicleHash<K, MapEntry<K, V>, MapSegmentContext<K, V, ?>,
                ExternalMapQueryContext<K, V, ?>> {

    /**
     * Delegates to {@link ChronicleMapBuilder#of(Class, Class)} for convenience.
     *
     * @param keyClass   class of the key type of the Chronicle Map to create
     * @param valueClass class of the value type of the Chronicle Map to create
     * @param <K>        the key type of the Chronicle Map to create
     * @param <V>        the value type of the Chronicle Map to create
     * @return a new {@code ChronicleMapBuilder} for the given key and value classes
     */
    static <K, V> ChronicleMapBuilder<K, V> of(Class<K> keyClass, Class<V> valueClass) {
        return ChronicleMapBuilder.of(keyClass, valueClass);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key.
     * <p>
     * If the value class allows reusing, consider {@link #getUsing(Object, Object)} method
     * instead of this to reduce garbage creation. Read <a
     * href="https://github.com/OpenHFT/Chronicle-Map/blob/ea/docs/CM_Tutorial.adoc#single-key-queries">the section about usage
     * patterns in the Chronicle Map 3 Tutorial</a> for more.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped after this method call, or {@code
     * null} if no value is mapped
     * @see #getUsing(Object, Object)
     */
    @Override
    V get(Object key);

    /**
     * Returns the value to which the specified key is mapped, read to the provided {@code value}
     * object, if possible, or returns {@code null}, if this map contains no mapping for the key.
     * <p>
     * If the specified key is present in the map, the value data is read to the provided {@code
     * value} object via value reader's {@link SizedReader#read(net.openhft.chronicle.bytes.Bytes, long, Object)
     * read(StreamingDataInput, size, value)} method. If the value deserializer is able to reuse the
     * given {@code value} object, calling this method instead of {@link #get(Object)} could help to
     * reduce garbage creation.
     * <p>
     * The provided {@code value} object is allowed to be {@code null}, in this case {@code
     * map.getUsing(key, null)} call is semantically equivalent to simple {@code map.get(key)}
     * call.
     *
     * @param key        the key whose associated value is to be returned
     * @param usingValue the object to read value data in, if possible
     * @return the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key
     * @see #get(Object)
     * @see #acquireUsing(Object, Object)
     * @see ChronicleMapBuilder#valueMarshallers(SizedReader, SizedWriter)
     */
    V getUsing(K key, V usingValue);

    /**
     * Acquire a value for a key, creating if absent.
     * <p>
     * If the specified key is absent in the map, {@linkplain
     * ChronicleMapBuilder#defaultValueProvider(DefaultValueProvider) default value provider} is
     * called. Then this object is put to this map for the specified key.
     * <p>
     * Then, either if the key was initially absent in the map or already present, the value is
     * deserialized just as during {@link #getUsing(Object, Object) getUsing(key, usingValue)} call,
     * passed the same {@code key} and {@code usingValue} as into this method call. This means, as
     * in {@link #getUsing}, {@code usingValue} could safely be {@code null}, in this case a new
     * value instance is created to deserialize the data.
     * <p>
     * In code, {@code acquireUsing} is specified as :
     * <pre>{@code
     * V acquireUsing(K key, V usingValue) {
     *     if (!containsKey(key))
     *         put(key, defaultValue(key));
     *     return getUsing(key, usingValue);
     * }}</pre>
     * <p>
     *
     * Where {@code defaultValue(key)} returns {@link
     * ChronicleMapBuilder#defaultValueProvider(DefaultValueProvider) defaultValueProvider}.
     * <p>
     * If the {@code ChronicleMap} is off-heap updatable, i. e. created via {@link
     * ChronicleMapBuilder} builder (values are {@link Byteable}), there is one more option of what
     * to do if the key is absent in the map. By default, value bytes are just zeroed out, no
     * default value, either provided for key or constant, is put for the absent key.
     * <p>
     * Unless value type is a Byteable or a value-type (e.g. {@link net.openhft.chronicle.core.values.LongValue}),
     * it's strictly advised to set {@link ChronicleMapBuilder#defaultValueProvider(DefaultValueProvider)
     * defaultValueProvider} explicitly. The value may be deserialized from a non-initialized memory region,
     * potentially causing marshalling errors.
     *
     * @param key        the key whose associated value is to be returned
     * @param usingValue the object to read value data in, if present. Can be null
     * @return value to which the given key is mapping after this call, either found or created
     * @see #getUsing(Object, Object)
     */
    V acquireUsing(@NotNull K key, V usingValue);

    /**
     * Acquires an update lock and a value for a key.
     * <p>
     * Lock is released when returned {@link net.openhft.chronicle.core.io.Closeable} object is closed.
     * This method is effectively equivalent to {@link #acquireUsing(Object, Object)} except for the
     * update lock management policy: {@link #acquireUsing(Object, Object)} releases the lock right away.
     * <p>
     * If the specified key is absent in the map, {@linkplain
     * ChronicleMapBuilder#defaultValueProvider(DefaultValueProvider) default value provider} is
     * called. Then this object is put to this map for the specified key.
     * <p>
     * Unless value is a Byteable or a value-type (e.g. {@link net.openhft.chronicle.core.values.LongValue}),
     * it's strictly advised to set {@link ChronicleMapBuilder#defaultValueProvider(DefaultValueProvider)
     * defaultValueProvider} explicitly. The value may be deserialized from a non-initialized memory region,
     * potentially causing marshalling errors.
     * <p>
     * Also, if value is not a Byteable or a value-type, changes on {@code usingValue} are
     * not propagated to the map memory right away. Updated {@code usingValue} is written to the map
     * when the control object is closed, before releasing the update lock.
     *
     * @param key the key whose associated value is to be returned
     * @param usingValue the object to read value data in, if present. Can be null
     * @see #acquireUsing(Object, Object)
     * @return Lock control object that releases the update lock on close.
     */
    @NotNull
    net.openhft.chronicle.core.io.Closeable acquireContext(@NotNull K key, @NotNull V usingValue);

    /**
     * Returns the result of application of the given function to the value to which the given key
     * is mapped. If there is no mapping for the key, {@code null} is returned from {@code
     * getMapped()} call without application of the given function. This method is primarily useful
     * when accessing {@code ChronicleMap} implementation which delegates it's requests to some
     * remote node (server) and pulls the result through serialization/deserialization path, and
     * probably network. In this case, when you actually need only a part of the map value's state
     * (e. g. a single field) it's cheaper to extract it on the server side and transmit lesser
     * bytes.
     *
     * @param key      the key whose associated value is to be queried
     * @param function a function to transform the value to the actually needed result,
     *                 which should be smaller than the map value
     * @param <R>      the result type
     * @return the result of applying the function to the mapped value, or {@code null} if there
     * is no mapping for the key
     */
    <R> R getMapped(K key, @NotNull SerializableFunction<? super V, R> function);

    /**
     * Exports all the entries to a {@link File} storing them in JSON format, an attempt is
     * made where possible to use standard java serialisation and keep the data human readable, data
     * serialized using the custom serialises are converted to a binary format which is not human
     * readable but this is only done if the Keys or Values are not {@link Serializable}.
     * This method can be used in conjunction with {@link ChronicleMap#putAll(File)} and is
     * especially useful if you wish to import/export entries from one chronicle map into another.
     * This import and export of the entries can be performed even when the versions of ChronicleMap
     * differ. This method is not performant and as such we recommend it is not used in performance
     * sensitive code.
     *
     * @param toFile the file to store all the entries to, the entries will be stored in JSON
     *               format
     * @throws IOException its not possible store the data to {@code toFile}
     * @see ChronicleMap#putAll(File)
     */
    void getAll(File toFile) throws IOException;

    /**
     * Imports all the entries from a {@link File}, the {@code fromFile} must be created
     * using or the same format as {@link ChronicleMap#get(Object)}, this method behaves
     * similar to {@link Map#put(Object, Object)} where existing
     * entries are overwritten. A write lock is only held while each individual entry is inserted
     * into the map, not over all the entries in the {@link File}
     *
     * @param fromFile the file containing entries ( in JSON format ) which will be deserialized and
     *                 {@link Map#put(Object, Object)} into the map
     * @throws IOException its not possible read the {@code fromFile}
     * @see ChronicleMap#getAll(File)
     */
    void putAll(File fromFile) throws IOException;

    /**
     * @return the class of {@code <V>}
     */
    Class<V> valueClass();

    /**
     * @return the value Class or UnresolvedType if unknown.
     */
    Type valueType();

    /**
     * WARNING: This is an expensive operation which can take milli-seconds.
     *
     * @return the amount of free space in the map as a percentage. When the free space gets low ( around 5-25% ) the map will automatically expand. The
     * number of times it can automatically expand is based on the {@code net.openhft.chronicle.map.ChronicleMapBuilder#maxBloatFactor}. If the map
     * expands you will see an increase in the available free space. NOTE: It is not possible to expand the chronicle map manually.
     * @see net.openhft.chronicle.map.ChronicleMap#remainingAutoResizes as these operations are related.
     */
    default short percentageFreeSpace() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * WARNING: This is a detailed however expensive operation which can take milliseconds
     * @return an array of how full each segment is
     */
    default SegmentStats[] segmentStats() {
        throw new UnsupportedOperationException("todo");
    }

    class SegmentStats extends SelfDescribingMarshallable {
        long usedBytes;
        long sizeInBytes;
        int tiers;

        public int tiers() {
            return tiers;
        }

        public long usedBytes() {
            return usedBytes;
        }

        public long sizeInBytes() {
            return sizeInBytes;
        }
    }

    /**
     * @return the number of times in the future the map can expand its capacity of each segment ( by expending its capacity we mean expending the maximum number of possible entries that
     * can be stored into the map), the map will expand automatically. However, there is an upper limit to the number of times the map can expand.
     * This limit is set via the {@code net.openhft.chronicle.map.ChronicleMapBuilder#maxBloatFactor} if the {@code remainingAutoResizes} drops to zero,
     * then the map is no longer able to expand, if subsequently, the free space ( see  {@link net.openhft.chronicle.map.ChronicleMap#percentageFreeSpace})
     * in the map becomes low ( around 5% ), the map will not be able to take more entries and will fail with an {@code
     * java.lang.IllegalStateException} for production systems it is recommended you periodically monitor the remainingAutoResizes and
     * {@link net.openhft.chronicle.map.ChronicleMap#percentageFreeSpace}.
     */
    default int remainingAutoResizes() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * The maximum number of times, the chronicle map is allowed to grow in size beyond
     * the configured number of entries.
     * <p>
     * The default maximum bloat factor factor is {@code 1.0} - i. e. "no bloat is expected".
     * <p>
     * It is strongly advised not to configure {@code maxBloatFactor} to more than {@code 10.0},
     * almost certainly, you either should configure {@code ChronicleHash}es completely differently,
     * or this data store doesn't fit to your case.
     *
     * @return maxBloatFactor the maximum number ot times, the chronicle map is alowed to be resized
     */
    default double maxBloatFactor() {
        throw new UnsupportedOperationException("todo");
    }
}

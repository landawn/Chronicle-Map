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

import net.openhft.chronicle.hash.Data;
import net.openhft.chronicle.map.replication.MapRemoteOperations;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"rawtypes", "unchecked"})
final class DefaultSpi implements MapMethods, MapEntryOperations, MapRemoteOperations,
        DefaultValueProvider {
    static final DefaultSpi DEFAULT_SPI = new DefaultSpi();

    static <K, V, R> MapMethods<K, V, R> mapMethods() {
        return DEFAULT_SPI;
    }

    static <K, V, R> MapEntryOperations<K, V, R> mapEntryOperations() {
        return DEFAULT_SPI;
    }

    static <K, V, R> MapRemoteOperations<K, V, R> mapRemoteOperations() {
        return DEFAULT_SPI;
    }

    static <K, V> DefaultValueProvider<K, V> defaultValueProvider() {
        return DEFAULT_SPI;
    }

    @Override
    public Data defaultValue(@NotNull MapAbsentEntry absentEntry) {
        return absentEntry.defaultValue();
    }
}

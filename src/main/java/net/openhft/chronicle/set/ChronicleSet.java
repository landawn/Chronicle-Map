/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.set;

import net.openhft.chronicle.hash.ChronicleHash;

import java.util.Set;

public interface ChronicleSet<K>
        extends Set<K>, ChronicleHash<K, SetEntry<K>, SetSegmentContext<K, ?>,
        ExternalSetQueryContext<K, ?>> {

    static <K> ChronicleSetBuilder<K> of(Class<K> keyClass) {
        return ChronicleSetBuilder.of(keyClass);
    }
}

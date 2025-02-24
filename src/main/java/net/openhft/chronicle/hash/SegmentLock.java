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

package net.openhft.chronicle.hash;

import net.openhft.chronicle.hash.locks.InterProcessReadWriteUpdateLock;

/**
 * {@link InterProcessReadWriteUpdateLock} of a segment in {@code ChronicleHash}.
 * <p>
 * In Chronicle-Map off-heap design, locks (and concurrency) are per-segment.
 *
 * @see ChronicleHashBuilder#minSegments(int)
 * @see ChronicleHashBuilder#actualSegments(int)
 */
public interface SegmentLock extends InterProcessReadWriteUpdateLock {

    /**
     * Returns the index of the accessed segment.
     */
    int segmentIndex();
}

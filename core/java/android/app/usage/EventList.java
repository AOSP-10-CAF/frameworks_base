/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.usage;

import java.util.ArrayList;

/**
 * A container to keep {@link UsageEvents.Event usage events} in non-descending order of their
 * {@link UsageEvents.Event#mTimeStamp timestamps}.
 *
 * @hide
 */
public class EventList {

    private final ArrayList<UsageEvents.Event> mEvents;

    /**
     * Create a new event list with default capacity
     */
    public EventList() {
        mEvents = new ArrayList<>();
    }

    /**
     * Returns the size of the list
     * @return the number of events in the list
     */
    public int size() {
        return mEvents.size();
    }

    /**
     * Removes all events from the list
     */
    public void clear() {
        mEvents.clear();
    }

    /**
     * Returns the {@link UsageEvents.Event event} at the specified position in this list.
     * @param index the index of the event to return, such that {@code 0 <= index < size()}
     * @return The {@link UsageEvents.Event event} at position {@code index}
     */
    public UsageEvents.Event get(int index) {
        return mEvents.get(index);
    }

    /**
     * Inserts the given {@link UsageEvents.Event event} into the list while keeping the list sorted
     * based on the event {@link UsageEvents.Event#mTimeStamp timestamps}.
     *
     * @param event The event to insert
     */
    public void insert(UsageEvents.Event event) {
        final int size = mEvents.size();
        // fast case: just append if this is the latest event
        if (size == 0 || event.mTimeStamp >= mEvents.get(size - 1).mTimeStamp) {
            mEvents.add(event);
            return;
        }
        // To minimize number of elements being shifted, insert at the first occurrence of the next
        // greatest timestamp in the list.
        final int insertIndex = firstIndexOnOrAfter(event.mTimeStamp + 1);
        mEvents.add(insertIndex, event);
    }

    /**
     * Finds the index of the first event whose timestamp is greater than or equal to the given
     * timestamp.
     *
     * @param timeStamp The timestamp for which to search the list.
     * @return The smallest {@code index} for which {@code (get(index).mTimeStamp >= timeStamp)} is
     * {@code true}, or {@link #size() size} if no such {@code index} exists.
     */
    public int firstIndexOnOrAfter(long timeStamp) {
        final int size = mEvents.size();
        int result = size;
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            final long midTimeStamp = mEvents.get(mid).mTimeStamp;
            if (midTimeStamp >= timeStamp) {
                hi = mid - 1;
                result = mid;
            } else {
                lo = mid + 1;
            }
        }
        return result;
    }

    /**
     * Remove events of certain type on or after a timestamp.
     * @param type The type of event to remove.
     * @param timeStamp the timeStamp on or after which to remove the event.
     */
    public void removeOnOrAfter(int type, long timeStamp) {
        for (int i = mEvents.size() - 1; i >= 0; i--) {
            UsageEvents.Event event = mEvents.get(i);
            if (event.mTimeStamp < timeStamp) {
                break;
            }
            if (event.mEventType == type) {
                mEvents.remove(i);
            }
        }
    }
}

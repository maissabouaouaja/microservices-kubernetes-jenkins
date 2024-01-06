/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.discovery.util;

import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.patterns.PolledMeter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A collection of gauges that represent different threshold levels over which measurement is mapped to.
 * Value 1 denotes a lowest threshold level that is reached.
 * For example eureka client registry data staleness defines thresholds 30s, 60s, 120s, 240s, 480s. Delay of 90s
 * would be mapped to gauge values {30s=0, 60s=1, 120=0, 240s=0, 480s=0, unlimited=0}.
 *
 * @author Tomasz Bak
 */
public class ThresholdLevelsMetric {

    public static final ThresholdLevelsMetric NO_OP_METRIC = new ThresholdLevelsMetric() {
        @Override
        public void update(long delayMs) {
        }
    };

    private final long[] levels;
    private final AtomicLong[] gauges;

    public ThresholdLevelsMetric() {
        levels = null;
        gauges = null;
    }

    public ThresholdLevelsMetric(Object owner, String prefix, long[] levels) {
        this.levels = levels;
        this.gauges = new AtomicLong[levels.length];
        for (int i = 0; i < levels.length; i++) {
            String name = prefix + String.format("%05d", levels[i]);
            gauges[i] = PolledMeter.using(Spectator.globalRegistry()).withName(name)
                .withTag("class", owner.getClass().getName()).monitorValue(new AtomicLong());
        }
    }

    public void update(long delayMs) {
        long delaySec = delayMs / 1000;
        long matchedIdx;
        if (levels[0] > delaySec) {
            matchedIdx = -1;
        } else {
            matchedIdx = levels.length - 1;
            for (int i = 0; i < levels.length - 1; i++) {
                if (levels[i] <= delaySec && delaySec < levels[i + 1]) {
                    matchedIdx = i;
                    break;
                }
            }
        }
        for (int i = 0; i < levels.length; i++) {
            if (i == matchedIdx) {
                gauges[i].set(1L);
            } else {
                gauges[i].set(0L);
            }
        }
    }
}

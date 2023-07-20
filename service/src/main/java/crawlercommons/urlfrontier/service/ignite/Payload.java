// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.ignite;

import java.io.Serializable;

class Payload implements Serializable {
    long nextFetchDate;
    byte[] payload;

    public Payload(long nextFetchDate, byte[] bs) {
        this.nextFetchDate = nextFetchDate;
        this.payload = bs;
    }
}

/**
 * Licensed to Crawler-Commons under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crawlercommons.urlfrontier.service.memory;

import java.io.Serializable;
import java.time.Instant;

import com.google.protobuf.InvalidProtocolBufferException;

import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;

/**
 * simpler than the objects from gRPC + sortable and have equals based on URL
 * only. The metadata key values are compressed into a single byte array.
 **/
class InternalURL implements Comparable<InternalURL>, Serializable {

	public long nextFetchDate;
	public String url;
	public byte[] serialised;

	// this is set when the URL is sent for processing
	// so that a subsequent call to getURLs does not send it again
	public long heldUntil = -1;

	private InternalURL() {
	}

	/*
	 * Returns the key if any, whether it is a discovered URL or not and an internal
	 * object to represent it
	 **/
	public static Object[] from(URLItem i) {
		InternalURL iu = new InternalURL();
		URLInfo info;
		Boolean disco = Boolean.TRUE;
		if (i.hasDiscovered()) {
			info = i.getDiscovered().getInfo();
			iu.nextFetchDate = Instant.now().getEpochSecond();
		} else {
			KnownURLItem known = i.getKnown();
			info = known.getInfo();
			iu.nextFetchDate = known.getRefetchableFromDate();
			disco = Boolean.FALSE;
		}
		// keep the whole original serialization into memory
		iu.serialised = info.toByteArray();
		iu.url = info.getUrl();
		return new Object[] { info.getKey(), disco, iu };
	}

	@Override
	public int compareTo(InternalURL arg0) {
		int comp = Long.compare(nextFetchDate, arg0.nextFetchDate);
		if (comp == 0) {
			return url.compareTo(arg0.url);
		}
		return comp;
	}

	@Override
	public boolean equals(Object obj) {
		return url.equals(((InternalURL) obj).url);
	}

	void setHeldUntil(long t) {
		heldUntil = t;
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	public URLInfo toURLInfo(String key) throws InvalidProtocolBufferException {
		URLInfo unfrozen = URLInfo.parseFrom(serialised);
		return URLInfo.newBuilder().setKey(key).setUrl(url).putAllMetadata(unfrozen.getMetadataMap()).build();
	}

}
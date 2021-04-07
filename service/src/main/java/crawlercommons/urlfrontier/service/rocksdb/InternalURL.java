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

package crawlercommons.urlfrontier.service.rocksdb;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;

/** Transitional object **/
class InternalURL {

	public long nextFetchDate;
	public String url;

	// key for the queue this URL is attached to
	public String Qkey;

	// this is set when the URL is sent for processing
	// so that a subsequent call to getURLs does not send it again
	public long heldUntil = -1;

	private Map<String, StringList> metadata;

	private boolean discovered = false;

	private InternalURL() {
	}

	/*
	 * Returns the key if any, whether it is a discovered URL or not and an internal
	 * object to represent it
	 **/
	public static InternalURL from(URLItem i) {
		InternalURL iu = new InternalURL();
		URLInfo info;
		iu.discovered = Boolean.TRUE;
		if (i.hasDiscovered()) {
			info = i.getDiscovered().getInfo();
			iu.nextFetchDate = Instant.now().getEpochSecond();
		} else {
			KnownURLItem known = i.getKnown();
			info = known.getInfo();
			iu.nextFetchDate = known.getRefetchableFromDate();
			iu.discovered = Boolean.FALSE;
		}
		iu.metadata = info.getMetadataMap();
		iu.Qkey = info.getKey();
		iu.url = info.getUrl();
		return iu;
	}

	void setHeldUntil(long t) {
		heldUntil = t;
	}

	public boolean isDiscovered() {
		return discovered;
	}

	public URLInfo fromBytes(byte[] bytes) {
		String asString = new String(bytes, StandardCharsets.UTF_8);

		// TODO parse the string to extract the URL, Q key and metadata

		return URLInfo.newBuilder().build();
	}

	public byte[] asBytes() {
		// put all the info into a string and get the bytes from them
		StringBuilder sb = new StringBuilder();

		sb.append(url).append("\t").append(Qkey);

		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

}
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
import java.util.HashMap;
import java.util.Map;

import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;

/**
 * Transitional object - makes it easier to expose the info from the protobuf
 * objects
 **/
class InternalURL {

	private InternalURL() {
	}

	public String url;

	// key for the queue this URL is attached to
	public String Qkey;

	public long nextFetchDate;

	private Map<String, StringList> metadata;

	public boolean discovered = false;

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

	public boolean isDiscovered() {
		return discovered;
	}

	public static URLInfo fromBytes(byte[] bytes) {
		String input = new String(bytes, StandardCharsets.UTF_8);

		// parse the string to extract the URL, Q key and metadata
		// bit rubbish but could be improved later

		String[] tokens = input.split("\t");
		if (tokens.length < 1)
			return null;

		String url = tokens[0];
		String Qkey = tokens[1];

		Map<String, StringList.Builder> metadata = new HashMap<>();

		for (int i = 2; i < tokens.length; i++) {
			String token = tokens[i];
			// split into key & value
			int firstequals = token.indexOf("=");
			String value = null;
			String key = token;
			if (firstequals != -1) {
				key = token.substring(0, firstequals);
				value = token.substring(firstequals + 1);
				StringList.Builder builder = metadata.computeIfAbsent(key, k -> StringList.newBuilder());
				builder.addValues(value);
			}
		}

		URLInfo.Builder bbuilder = URLInfo.newBuilder().setKey(Qkey).setUrl(url);

		metadata.forEach((k, v) -> {
			bbuilder.putMetadata(k, v.build());
		});

		return bbuilder.build();
	}

	public byte[] asBytes() {
		// put all the info into a string and get the bytes from them
		StringBuilder sb = new StringBuilder();

		sb.append(url).append("\t").append(Qkey);

		metadata.forEach((k, v) -> {
			v.getValuesList().forEach(s -> sb.append("\t").append(k).append("=").append(s));
		});

		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

}
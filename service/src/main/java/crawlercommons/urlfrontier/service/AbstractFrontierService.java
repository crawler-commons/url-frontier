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

package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import io.grpc.stub.StreamObserver;

public abstract class AbstractFrontierService extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

	private boolean active = true;
	
	private int defaultDelayForQueues = 1;
	
	public int getDefaultDelayForQueues() {
		return defaultDelayForQueues;
	}

	public void setDefaultDelayForQueues(int defaultDelayForQueues) {
		this.defaultDelayForQueues = defaultDelayForQueues;
	}

	protected boolean isActive() {
		return active;
	}
	
	@Override
	public void setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request,
			StreamObserver<Empty> responseObserver) {
		active = request.getState();
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

}

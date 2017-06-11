/*
 * Copyright 2017 nthstage
 *
 *See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nthstage.log4j2extn.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MapMessage;

/**
 * @author nthstage
 *
 */
public class SolrAppenderTest {
	private static Logger logger = LogManager.getLogger(SolrAppenderTest.class);
	public static void main(String[] args) {
		//logger.debug("Hello");
		Map<String, String> logMap = new HashMap<>();
		logMap.put("host", "myappserver"+new Random().nextInt(3));
		logMap.put("userId", "user"+new Random().nextInt(10));
		logMap.put("service", "service_"+new Random().nextInt(5));
		logMap.put("duration", ""+new Random(10).nextInt(1000));
		logMap.put("message", "completed");
		logger.debug(new MapMessage(logMap));
	}
}

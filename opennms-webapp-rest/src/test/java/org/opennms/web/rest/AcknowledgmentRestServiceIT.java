/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.test.rest.AbstractSpringJerseyRestTestCase;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-svclayer.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-jersey.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class AcknowledgmentRestServiceIT extends AbstractSpringJerseyRestTestCase {
	@Autowired
	private TransactionTemplate m_template;

	@Autowired
	private DatabasePopulator m_databasePopulator;

	@Override
	protected void afterServletStart() {
		m_template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				m_databasePopulator.populateDatabase();
			}
		});
	}

	@Test
	@JUnitTemporaryDatabase
	public void testAcknowlegeNotification() throws Exception {
	    final Pattern p = Pattern.compile("^.*<answeredBy>(.*?)</answeredBy>.*$", Pattern.DOTALL & Pattern.MULTILINE);
	    sendData(POST, MediaType.APPLICATION_FORM_URLENCODED, "/acks", "notifId=1&action=ack");
	    String xml = sendRequest(GET, "/notifications/1", new HashMap<String,String>(), 200);
	    Matcher m = p.matcher(xml);
	    assertTrue(m.matches());
	    assertTrue(m.group(1).equals("admin"));
        sendData(POST, MediaType.APPLICATION_FORM_URLENCODED, "/acks", "notifId=1&action=unack");
        xml = sendRequest(GET, "/notifications/1", new HashMap<String,String>(), 200);
        m = p.matcher(xml);
        assertFalse(m.matches());
	}

	@Test
	@JUnitTemporaryDatabase
	public void testAcknowlegeAlarm() throws Exception {
	    final Pattern p = Pattern.compile("^.*<ackTime>(.*?)</ackTime>.*$", Pattern.DOTALL & Pattern.MULTILINE);
	    sendData(POST, MediaType.APPLICATION_FORM_URLENCODED, "/acks", "alarmId=1&action=ack");
	    String xml = sendRequest(GET, "/alarms/1", new HashMap<String,String>(), 200);
	    Matcher m = p.matcher(xml);
	    assertTrue(m.matches());
	    assertTrue(m.group(1).length() > 0);
        sendData(POST, MediaType.APPLICATION_FORM_URLENCODED, "/acks", "alarmId=1&action=unack");
        xml = sendRequest(GET, "/alarms/1", new HashMap<String,String>(), 200);
        m = p.matcher(xml);
        assertFalse(m.matches());
	}
}
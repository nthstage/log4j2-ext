/*
 * Copyright 2017 the original author or authors
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
package com.nthstage.log4j2extn.appender.solr;


import java.sql.SQLException;

import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Configuration element for {@link JdbcAppender}. If you want to use the {@link JdbcAppender} but none of the provided
 * connection sources meet your needs, you can simply create your own connection source.
 * 
 * @author nthstage
 * 
 */
public interface SolrConnectionSource {
    /**
     * This should return a new connection every time it is called.
     *
     * @return the SQL connection object.
     * @throws SQLException if a database error occurs.
     */
    SolrClient getSolrClient() throws SolrServerException;

    /**
     * All implementations must override {@link Object#toString()} to provide information about the connection
     * configuration (obscuring passwords with one-way hashes).
     *
     * @return the string representation of this connection source.
     */
    @Override
    String toString();
}

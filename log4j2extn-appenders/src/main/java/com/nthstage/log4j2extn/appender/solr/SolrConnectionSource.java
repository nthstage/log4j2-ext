package com.nthstage.log4j2extn.appender.solr;


import java.sql.SQLException;

import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Configuration element for {@link JdbcAppender}. If you want to use the {@link JdbcAppender} but none of the provided
 * connection sources meet your needs, you can simply create your own connection source.
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

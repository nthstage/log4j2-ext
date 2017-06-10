package com.nthstage.log4j2extn.appender.db.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
/**
 * Configuration element for {@link SOLRAppender}. The {@link SolrServer} API is used to connect to the solr database.   
 *
 */
public interface SolrConnectionSource {
	/**
	 * This should return a new solr server instance every time it is called.
	 * @return the SolrServer object
	 * @throws SolrServerException
	 */
	SolrClient getSolrClient() throws SolrServerException;
	
	 /**
     * All implementations must override {@link Object#toString()} to provide information about the Solr connection
     * configuration.
     *
     * @return the string representation of this Solr connection source.
     */
	@Override
	public String toString();
}

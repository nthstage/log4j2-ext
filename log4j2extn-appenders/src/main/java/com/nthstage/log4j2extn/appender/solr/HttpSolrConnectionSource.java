package com.nthstage.log4j2extn.appender.solr;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.common.SolrException;

/**
 * A {@link SolrConnectionSource} connection source that uses a {@link HttpSolrClient} to connect to the SOLR server.
 */
@Plugin(name = "HttpSlorClientSource", category = "Core", elementType = "solrConnectionSource", printObject = true)
public final class HttpSolrConnectionSource implements SolrConnectionSource {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final SolrClient solrClient;
    private final String description;

    private HttpSolrConnectionSource(final SolrClient solrClient) {
        this.solrClient = solrClient;
        this.description = "solrClient{ name=" + HttpSolrConnectionSource.class.getName() + ", value=" + solrClient + " }";
    }

    public SolrClient getSolrClient() throws SolrException {
        return this.solrClient;
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a solr connection source within the plugin manager.
     *
     * @param solrServerHost The host url of the SOLR Server
     * @return the created Solr connection source.
     */
    @PluginFactory
    public static SolrConnectionSource createConnectionSource(@PluginAttribute("solrServerHost") final String solrServerHost,
    		@PluginAttribute("solrCore") final String solrCore, 
    		@PluginElement ("sslConfiguration") final SslConfiguration sslConfiguration) {
        if (Strings.isEmpty(solrServerHost)) {
            LOGGER.error("No Solr Server host url provided.");
            return null;
        }
        String solrUrl = solrServerHost.trim();
        if(Strings.isNotEmpty(solrCore)) {
        	if(!solrUrl.endsWith("/")) {
        		solrUrl+="/";
        	}
        	solrUrl+=solrCore.trim();
        }
        try {
        	SolrClient solrClient = new SolrClientCache().getHttpSolrClient(solrUrl);
            if (solrClient == null) {
                LOGGER.error("No SolrClinet from SolrClientCache for host "+solrUrl);
                return null;
            }
            
            return new HttpSolrConnectionSource(solrClient);
        } catch (final SolrException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }
}

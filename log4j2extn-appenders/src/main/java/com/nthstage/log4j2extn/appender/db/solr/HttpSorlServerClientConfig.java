package com.nthstage.log4j2extn.appender.db.solr;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

/**
 * A {@link SOLRAppender} connection source that uses a {@link SolrConnectionSource} to connect to the solr database.
 * 
 */

@Plugin(name = "HttpSorlServerClient", category = "Core", elementType = "solrConnectionSource", printObject = true)
public class HttpSorlServerClientConfig implements SolrConnectionSource
{
	private static final Logger LOGGER = StatusLogger.getLogger();
	private final SolrClient solrClient;
	private final String description;
	
	private HttpSorlServerClientConfig(final SolrClient solrClient, final String serverUrl) {
		this.solrClient = solrClient;
		this.description = "HttpSorlServerClient{  serverUrl" + serverUrl + " }";
	}
	
	@Override
    public String toString() {
        return this.description;
    }
	
	/**
	 * 
	 * @param serverUrl
	 * @param maxRetries
	 * @param connectionTimeout
	 * @param socketTimeout
	 * @param maxConnections
	 * @param followRedirects
	 * @param allowCompression
	 * @return
	 */
	@PluginFactory
	public static SolrConnectionSource createSolrConnectionSource(
			@PluginAttribute("serverUrl") final String serverUrl,
			@PluginAttribute("maxRetries") final String maxRetries,
			@PluginAttribute("connectionTimeout") final String connectionTimeout,
			@PluginAttribute("socketTimeout") final String socketTimeout,
			@PluginAttribute("maxConnections") final String maxConnections,
			@PluginAttribute("followRedirects") final String followRedirects,
			@PluginAttribute("allowCompression") final String allowCompression) {
		
		if (Strings.isEmpty(serverUrl)) {
			LOGGER.error("No solr server url provided.");
	        return null;
	        }
		
		
		
		final int maxRetriesInt = AbstractAppender.parseInt(maxRetries, 0);
		final int connectionTimeoutInt = AbstractAppender.parseInt(connectionTimeout, 5000);
		final int socketTimeoutInt = AbstractAppender.parseInt(socketTimeout, 1000);
		final int maxConnectionsInt = AbstractAppender.parseInt(maxConnections, 10);
		
        final boolean followRedirectsFlag = Booleans.parseBoolean(followRedirects, true);
        final boolean allowCompressionFlag = Booleans.parseBoolean(allowCompression, true);
        
        
        HttpRequestRetryHandler retryHandler = new StandardHttpRequestRetryHandler(maxRetriesInt, false);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeoutInt)
                .setConnectTimeout(connectionTimeoutInt)
                .build();
        
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if(followRedirectsFlag) {
        	httpClientBuilder.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE);
        }
        httpClientBuilder.setRetryHandler(retryHandler);
        httpClientBuilder.setDefaultRequestConfig(requestConfig);	
        httpClientBuilder.setMaxConnTotal(maxConnectionsInt);
        
        HttpClient httpClient =	httpClientBuilder.build();
        
        
        final SolrClient solrClient = new HttpSolrClient.Builder(serverUrl)
        		.withHttpClient(httpClient)
        		.allowCompression(allowCompressionFlag).build();
        
		return new HttpSorlServerClientConfig(solrClient, serverUrl);
	}

	public SolrClient getSolrClient() throws SolrServerException {
		return solrClient;
	}
	
}

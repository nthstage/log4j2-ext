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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import org.apache.solr.common.SolrException;

/**
 * A {@link SolrConnectionSource} connection source that uses a {@link HttpSolrClient} to connect to the SOLR server.
 * 
 * @author nthstage
 *
 */
@Plugin(name = "PoolingHttpSolrConnectionSource", category = "Core", elementType = "solrConnectionSource", printObject = true)
public final class PoolingHttpSolrConnectionSource implements SolrConnectionSource {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final SolrClient solrClient;
    private final String description;

    private PoolingHttpSolrConnectionSource(final SolrClient solrClient) {
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
        	RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        	registryBuilder.register("http", new PlainConnectionSocketFactory());
        	if(sslConfiguration!=null) {
        		SSLSocketFactory sslSocketFactory = sslConfiguration.getSslSocketFactory();
        		HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
        		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslSocketFactory, hostnameVerifier);
        		registryBuilder.register("https", sslsf);
        	}
        	
        	Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        	PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager(registry);
        	
        	HttpClient httpClient = HttpClientBuilder.create().setConnectionManager(httpConnectionManager).build();
        	
            final SolrClient solrClient = new HttpSolrClient.Builder(solrUrl).withHttpClient(httpClient).build();
            if (solrClient == null) {
                LOGGER.error("No SolrClinet from SolrClientCache for host "+solrUrl);
                return null;
            }
            
            return new PoolingHttpSolrConnectionSource(solrClient);
        } catch (final SolrException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }
}

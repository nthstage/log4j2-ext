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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * This Appender writes logging events to a relational database using SOLR Client API. It takes a list of
 * {@link FieldConfig}s with which it determines how to save the event data into the appropriate fields in the SOLR schema.
 * A {@link SolrConnectionSource} plugin instance instructs the appender (and {@link SolrClientManager}) how to connect to
 * the Solr. This appender can be reconfigured at run time.
 *
 * @see FieldConfig
 * @see SolrConnectionSource
 * 
 * @author nthstage
 *
 */
@Plugin(name = "SOLR", category = "Core", elementType = "appender", printObject = true)
public final class SolrAppender extends AbstractAppender  {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    
    private final String description;
    private SolrClientManager manager;
    private SolrAppender(final String name, final Filter filter, final boolean ignoreExceptions,
                         final SolrClientManager manager) {
    	super(name, filter, null, ignoreExceptions);
    	this.manager = manager;
        this.description = this.getName() + "{ manager=" + manager + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }
	

    @Override
    public final void append(final LogEvent event) {
    	this.readLock.lock();
        try {
            this.manager.write(event);
        } catch (final LoggingException e) {
            LOGGER.error("Unable to write to SOLR [{}] for appender [{}].", this.manager.getName(),
                    this.getName(), e);
            throw e;
        } catch (final Exception e) {
            LOGGER.error("Unable to write to SOLR [{}] for appender [{}].", this.manager.getName(),
                    this.getName(), e);
            throw new AppenderLoggingException("Unable to write to SOLR in appender: " + e.getMessage(), e);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public final void start() {
        if (this.manager == null) {
            LOGGER.error("No Manager set for the appender named [{}].", this.getName());
        }
        super.start();
        if (this.manager!= null) {
            this.manager.startup();
        }
    }

    @Override
    public final void stop() {
        super.stop();
        if (this.manager != null) {
            this.manager.release();
        }
    }
    
    /**
     * Factory method for creating a SOLR appender within the plugin manager.
     *
     * @param name The name of the appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param filter The filter, if any, to use.
     * @param solrConnectionSource The Solr Client connection source.
     * @param bufferSize If an integer greater than 0, this causes the appender to buffer log events and flush whenever
     *                   the buffer reaches this size.
     * @param fieldConfigs Information about the columns that log event data should be inserted into and how to insert
     *                      that data.
     * @return a new SOLR appender.
     */
    @PluginFactory
    public static SolrAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("SolrConnectionSource") final SolrConnectionSource solrConnectionSource,
            @PluginAttribute("bufferSize") final String bufferSize,
            @PluginElement("FieldConfigs") final FieldConfig[] fieldConfigs) {

        final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        final StringBuilder managerName = new StringBuilder("solrAppender{ description=").append(name)
                .append(", bufferSize=").append(bufferSizeInt).append(", connectionSource=")
                .append(solrConnectionSource.toString()).append(", columns=[ ");

        int i = 0;
        for (final FieldConfig field : fieldConfigs) {
            if (i++ > 0) {
                managerName.append(", ");
            }
            managerName.append(field.toString());
        }

        managerName.append(" ] }");

        final SolrClientManager manager = SolrClientManager.getSlorClientManager(managerName.toString(), bufferSizeInt, solrConnectionSource, fieldConfigs);
        if (manager == null) {
            return null;
        }

        return new SolrAppender(name, filter, ignoreExceptions, manager);
    }
}

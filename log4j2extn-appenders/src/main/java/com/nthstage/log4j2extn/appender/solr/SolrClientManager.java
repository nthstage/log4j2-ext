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

import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.common.SolrInputDocument;

/**
 * Manager for SlorClinet objects
 * 
 * @author nthstage
 *
 */
public class SolrClientManager extends AbstractManager implements Flushable{

	private final ArrayList<LogEvent> buffer;
    private final int bufferSize;
    private final SolrConnectionSource solrConnectionSource;
    private final List<Field> fields;

    private boolean running = false;
    private SolrClient solrClient;
    
	protected SolrClientManager(String name, int bufferSizeInt,
			SolrConnectionSource solrConnectionSource, List<Field> fields ) {
        super(name);
        this.bufferSize = bufferSizeInt;
        this.buffer = new ArrayList<>(bufferSize + 1);
        this.solrConnectionSource = solrConnectionSource;
        this.fields = fields;
    }

    /**
     * This method is called within the appender when the appender is started. If it has not already been called, it
     * calls {@link #startupInternal()} and catches any exceptions it might throw.
     */
    public final synchronized void startup() {
        if (!this.isRunning()) {
            try {
                this.connectAndStart();
                this.running = true;
            } catch (final Exception e) {
                logError("could not perform database startup operations", e);
            }
        }
    }

    /**
     * Indicates whether the manager is currently connected {@link #startup()} has been called and {@link #shutdown()}
     * has not been called).
     *
     * @return {@code true} if the manager is connected.
     */
    public final boolean isRunning() {
        return this.running;
    }

    /**
     * Connects to the database and starts a transaction (if applicable). With buffering enabled, this is called when
     * flushing the buffer begins, before the first call to {@link #writeInternal}. With buffering disabled, this is
     * called immediately before every invocation of {@link #writeInternal}.
     */
    protected void connectAndStart() {
    	try {
			this.solrClient = this.solrConnectionSource.getSolrClient();
		} catch (SolrServerException e) {
			new AppenderLoggingException(
					"Cannot write logging event or flush buffer; SOLR manager cannot connect to the solr database.", e
			);
		}
    }


    protected void writeInternal(LogEvent event) {		
		SolrInputDocument document = new SolrInputDocument();
		
		if(!this.isRunning() || this.solrClient == null) {
			throw new AppenderLoggingException(
					"cannot write logging event; SOLR manager not connected to the solr database.");
		}
		
		for (final Field field : this.fields) {
			if(field.literalValue!=null){
				document.addField(field.fieldName, field.literalValue);
			} else if (field.isEventTimestamp) {
            	document.addField(field.fieldName, event.getTimeMillis());
            } else {
            	document.addField(field.fieldName, field.layout.toSerializable(event));
            }
		}
		try {
			solrClient.add(document);
		} catch (SolrServerException e) {
			throw new AppenderLoggingException("Failed to insert record for log event in SOLR manager: " +
                    e.getMessage(), e);
		} catch (IOException e) {
			throw new AppenderLoggingException("Failed to insert record for log event in SOLR manager: " +
                    e.getMessage(), e);
		}catch (RemoteSolrException e) {
			if(solrClient!=null) {
				try {
					Closer.close(solrClient);
				} catch (IOException ioee) {
					
				} finally {
					this.solrClient = null;
				}
			}
			throw new AppenderLoggingException("Failed to insert record for log event in SOLR manager: " +
                    e.getMessage(), e);
		}
	}

	
	protected void commitAndClose() {
		try {
			if(solrClient!=null) {
				solrClient.commit();
			}
		} catch (SolrServerException e) {
			throw new AppenderLoggingException("Failed to commit solr transaction logging event or flushing buffer.", e);
		} catch (IOException e) {
			throw new AppenderLoggingException("Failed to commit solr transaction logging event or flushing buffer.", e);
		}
		finally {
			try {
				Closer.close(solrClient);
			} catch (IOException e) {
				
			} finally {
				this.solrClient = null;
			}
			
		}
	}

    /**
     * This method is called automatically when the buffer size reaches its maximum or at the beginning of a call to
     * {@link #shutdown()}. It can also be called manually to flush events to the database.
     */
    @Override
    public final synchronized void flush() {
        if (this.isRunning() && this.buffer.size() > 0) {
            this.connectAndStart();
            try {
                for (final LogEvent event : this.buffer) {
                    this.writeInternal(event);
                }
            } finally {
                this.commitAndClose();
                // not sure if this should be done when writing the events failed
                this.buffer.clear();
            }
        }
    }

    /**
     * This method manages buffering and writing of events.
     *
     * @param event The event to write to the database.
     */
    public final synchronized void write(final LogEvent event) {
        if (this.bufferSize > 0) {
            this.buffer.add(event);
            if (this.buffer.size() >= this.bufferSize || event.isEndOfBatch()) {
                this.flush();
            }
        } else {
            this.connectAndStart();
            try {
                this.writeInternal(event);
            } finally {
                this.commitAndClose();
            }
        }
    }
    
	@Override
    public final void releaseSub() {
		this.flush();
		 if (this.isRunning()) {
			 try {
	                this.commitAndClose();
	            } catch (final Exception e) {
	                logWarn("caught exception while performing database shutdown operations", e);
	            } finally {
	                this.running = false;
	            }
		}
    }
	
    @Override
    public final String toString() {
        return this.getName();
    }
    
	public static SolrClientManager getSlorClientManager(String name, int bufferSizeInt,
			SolrConnectionSource solrConnectionSource, FieldConfig[] fieldConfigs) {
		final List<Field> fields = new ArrayList<Field>();
        for (final FieldConfig config : fieldConfigs) {
            if (config.getLiteralValue() == null) {
            	fields.add(new Field(config.getFieldName(), config.getLayout(), config.isEventTimestamp(), config.getLiteralValue()));
            }
        }
		return new SolrClientManager(name, bufferSizeInt, solrConnectionSource, fields);
	} 
	
	private static class Field {
		 private final String fieldName;
		 private final PatternLayout layout;
	     private final boolean isEventTimestamp;
	     private final String literalValue;
	     
		 private Field(final String fieldName, final PatternLayout layout, final boolean isEventDate, final String literalValue) {
			  this.fieldName=fieldName;
		      this.layout = layout;
		      this.isEventTimestamp = isEventDate;
		      this.literalValue = literalValue;
		  }
		 
	 }
}

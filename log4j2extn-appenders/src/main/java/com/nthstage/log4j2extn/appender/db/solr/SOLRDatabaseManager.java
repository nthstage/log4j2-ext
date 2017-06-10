package com.nthstage.log4j2extn.appender.db.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.common.SolrInputDocument;

public class SOLRDatabaseManager extends AbstractDatabaseManager {
	private static final SOLRDatabaseManagerFactory FACTORY = new SOLRDatabaseManagerFactory();
	
	private final List<Field> fields;
	private final SolrConnectionSource solrConnectionSource;
	
	private SolrClient solrClient;
	
	private SOLRDatabaseManager(final String name, final int bufferSize, final SolrConnectionSource solrConnectionSource,
								final List<Field> fields) {
		super(name, bufferSize);
		this.solrConnectionSource = solrConnectionSource;
		this.fields = fields;
	}

	@Override
	protected void startupInternal() throws Exception {

	}

	@Override
	protected void shutdownInternal() throws Exception {
		solrClient.close();
	}

	@Override
	protected void connectAndStart() {
		try {
			this.solrClient = this.solrConnectionSource.getSolrClient();
		} catch (SolrServerException e) {
			new AppenderLoggingException(
					"Cannot write logging event or flush buffer; SOLR manager cannot connect to the solr database.", e
			);
		}
	}

	@Override
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
					solrClient.close();
				} catch (IOException e1) {
				}
			}
			throw new AppenderLoggingException("Failed to insert record for log event in SOLR manager: " +
                    e.getMessage(), e);
		}
	}

	@Override
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
			this.solrClient = null;
		}
	}
	
	public static SOLRDatabaseManager getSOLRDatabaseManager(final String name, final int bufferSize,
															 final SolrConnectionSource solrConnectionSource,
															 final FieldConfig[] fieldConfigs) {
		return AbstractDatabaseManager.getManager(name, new FactoryData(bufferSize,solrConnectionSource,fieldConfigs), FACTORY);
	}
	
	private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
		private final FieldConfig[] fieldConfigs;
		private final SolrConnectionSource solrConnectionSource;
		
		
        protected FactoryData(final int bufferSize, final SolrConnectionSource solrConnectionSource, final FieldConfig[] fieldConfigs) {
            super(bufferSize);
            this.solrConnectionSource = solrConnectionSource;
            this.fieldConfigs = fieldConfigs;            
        }
    }
	
	 private static final class SOLRDatabaseManagerFactory implements ManagerFactory<SOLRDatabaseManager, FactoryData> {
		
		public SOLRDatabaseManager createManager(String name, FactoryData data) {
			final List<Field> fields = new ArrayList<Field>();
            for (final FieldConfig config : data.fieldConfigs) {
                if (config.getLiteralValue() == null) {
                	fields.add(new Field(config.getFieldName(), config.getLayout(), config.isEventTimestamp(), config.getLiteralValue()));
                }
            }
            
			return new SOLRDatabaseManager(name, data.getBufferSize(),data.solrConnectionSource, fields);
		}
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

package com.nthstage.log4j2extn.appender.db.solr;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;

@Plugin(name = "SOLR", category = "Core", elementType = "appender", printObject = true)
public class SOLRAppender extends AbstractDatabaseAppender<SOLRDatabaseManager> {
	private final String description;

    private SOLRAppender(final String name, final Filter filter, final boolean ignoreExceptions,
                         final SOLRDatabaseManager manager) {
        super(name, filter, ignoreExceptions, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
        SOS.print("In side SOLR construct");
    }

    @Override
    public String toString() {
        return this.description;
    }
	
    
    @PluginFactory
    public static SOLRAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("SolrConnectionSource") final SolrConnectionSource solrConnectionSource,
            @PluginAttribute("bufferSize") final String bufferSize,
            @PluginElement("fieldConfigs") final FieldConfig[] fieldConfigs) {
    	
    	final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        
        final StringBuilder managerName = new StringBuilder("solrManager{ description=").append(name)
                .append(", bufferSize=").append(bufferSizeInt).append(", SolrConnectionSource=")
                .append(solrConnectionSource.toString()).append(", fields=[ ");

                int i = 0;
                for (final FieldConfig field : fieldConfigs) {
                    if (i++ > 0) {
                        managerName.append(", ");
                    }
                    managerName.append(field.toString());
                }

                managerName.append(" ] }");
        
        final SOLRDatabaseManager manager = SOLRDatabaseManager.getSOLRDatabaseManager(managerName.toString(), bufferSizeInt,solrConnectionSource,fieldConfigs);
        
        if (manager == null) {
            return null;
        }
        
    	return new SOLRAppender(name, filter, ignoreExceptions, manager);
    }

}
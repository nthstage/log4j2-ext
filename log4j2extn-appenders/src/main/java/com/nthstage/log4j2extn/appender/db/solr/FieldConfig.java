package com.nthstage.log4j2extn.appender.db.solr;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;


/**
 * A configuration element used to configure which event properties are logged as which field of the SOLR input document.
 */
@Plugin(name = "Field", category = "Core", printObject=true)
public class FieldConfig {
	private static final Logger LOGGER = StatusLogger.getLogger();
	
	private final String fieldName;
	private final PatternLayout layout;
	private final String literalValue;
	private final boolean eventTimestamp;
	
	private FieldConfig(final String fieldName, final PatternLayout layout, final String literalValue, final boolean eventDate) {
		
		this.fieldName = fieldName;
		this.layout = layout;
		this.literalValue = literalValue;
		this.eventTimestamp = eventDate;
	}
	
	public String getFieldName() {
		return this.fieldName;
	}
	
	public PatternLayout getLayout() {
        return this.layout;
    }

    public String getLiteralValue() {
        return this.literalValue;
    }

    public boolean isEventTimestamp() {
        return this.eventTimestamp;
    }
	
    @Override
    public String toString() {
        return "{ name=" + this.fieldName + ", layout=" + this.layout + ", literal=" + this.literalValue
                + ", timestamp=" + this.eventTimestamp + " }";
    }
    
    /**
     * Factory method for creating a field config within the plugin manager.
     *
     * @param config The configuration object
     * @param name The name of the field as it exists within the solr document.
     * @param pattern The {@link PatternLayout} pattern to insert in this field. Mutually exclusive with
     *                {@code literalValue!=null} and {@code eventTimestamp=true}
     * @param literalValue The literal value to insert into the field as-is without any quoting or escaping. Mutually
     *                     exclusive with {@code pattern!=null} and {@code eventTimestamp=true}.
     * @param eventTimestamp If {@code "true"}, indicates that this field is a date-time field in which the event
     *                       timestamp should be inserted. Mutually exclusive with {@code pattern!=null} and
     *                       {@code literalValue!=null}.
     * @return the created field config.
     */
    @PluginFactory
    public static FieldConfig createFieldConfig(
            @PluginConfiguration final Configuration config,
            @PluginAttribute("name") final String name,
            @PluginAttribute("pattern") final String pattern,
            @PluginAttribute("literal") final String literalValue,
            @PluginAttribute("isEventTimestamp") final String eventTimestamp) {
        if (Strings.isEmpty(name)) {
            LOGGER.error("The field config is not valid because it does not contain a field name.");
            return null;
        }

        final boolean isPattern = Strings.isNotEmpty(pattern);
        final boolean isLiteralValue = Strings.isNotEmpty(literalValue);
        final boolean isEventTimestamp = Boolean.parseBoolean(eventTimestamp);

        if ((isPattern && isLiteralValue) || (isPattern && isEventTimestamp) || (isLiteralValue && isEventTimestamp)) {
            LOGGER.error("The pattern, literal, and isEventTimestamp attributes are mutually exclusive.");
            return null;
        }

        if (isEventTimestamp) {
            return new FieldConfig(name, null, null, true);
        }
        if (isLiteralValue) {
            return new FieldConfig(name, null, literalValue, false);
        }
        if (isPattern) {
        	PatternLayout layout = PatternLayout.newBuilder().withPattern(pattern).withConfiguration(config).build();
        	return new FieldConfig(name, layout, null,false
            );
        }

        LOGGER.error("To configure a field you must specify a pattern or literal or set isEventDate to true.");
        return null;
    }
}

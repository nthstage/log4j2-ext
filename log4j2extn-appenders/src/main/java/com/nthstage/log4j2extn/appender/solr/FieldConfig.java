package com.nthstage.log4j2extn.appender.solr;

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
 * A configuration element used to configure which event properties are logged to which columns in the database table.
 */
@Plugin(name = "Field", category = "Core", printObject = true)
public final class FieldConfig {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String fieldName;
    private final PatternLayout layout;
    private final String literalValue;
    private final boolean eventTimestamp;
    //private final boolean unicode;
    //private final boolean clob;

    private FieldConfig(final String fieldName, final PatternLayout layout, final String literalValue,
                         final boolean eventTimestamp)//, final boolean unicode, final boolean clob) 
    {
        this.fieldName = fieldName;
        this.layout = layout;
        this.literalValue = literalValue;
        this.eventTimestamp = eventTimestamp;
        //this.unicode = unicode;
        //this.clob = clob;
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

   /* public boolean isUnicode() {
        return this.unicode;
    }

    public boolean isClob() {
        return this.clob;
    }*/

    @Override
    public String toString() {
        return "{ name=" + this.fieldName + ", layout=" + this.layout + ", literal=" + this.literalValue
                + ", timestamp=" + this.eventTimestamp + " }";
    }

    /**
     * Factory method for creating a column config within the plugin manager.
     *
     * @param config The configuration object
     * @param name The name of the database column as it exists within the database table.
     * @param pattern The {@link PatternLayout} pattern to insert in this column. Mutually exclusive with
     *                {@code literalValue!=null} and {@code eventTimestamp=true}
     * @param literalValue The literal value to insert into the column as-is without any quoting or escaping. Mutually
     *                     exclusive with {@code pattern!=null} and {@code eventTimestamp=true}.
     * @param eventTimestamp If {@code "true"}, indicates that this column is a date-time column in which the event
     *                       timestamp should be inserted. Mutually exclusive with {@code pattern!=null} and
     *                       {@code literalValue!=null}.
     * @param unicode If {@code "true"}, indicates that the column is a Unicode String.
     * @param clob If {@code "true"}, indicates that the column is a character LOB (CLOB).
     * @return the created column config.
     */
    @PluginFactory
    public static FieldConfig createColumnConfig(
            @PluginConfiguration final Configuration config,
            @PluginAttribute("name") final String name,
            @PluginAttribute("pattern") final String pattern,
            @PluginAttribute("literal") final String literalValue,
            @PluginAttribute("isEventTimestamp") final String eventTimestamp) {
        if (Strings.isEmpty(name)) {
            LOGGER.error("The column config is not valid because it does not contain a column name.");
            return null;
        }

        final boolean isPattern = Strings.isNotEmpty(pattern);
        final boolean isLiteralValue = Strings.isNotEmpty(literalValue);
        final boolean isEventTimestamp = Boolean.parseBoolean(eventTimestamp);
        //final boolean isUnicode = Booleans.parseBoolean(unicode, true);
        //final boolean isClob = Boolean.parseBoolean(clob);

        if ((isPattern && isLiteralValue) || (isPattern && isEventTimestamp) || (isLiteralValue && isEventTimestamp)) {
            LOGGER.error("The pattern, literal, and isEventTimestamp attributes are mutually exclusive.");
            return null;
        }

        if (isEventTimestamp) {
            return new FieldConfig(name, null, null, true);//, false, false);
        }
        if (isLiteralValue) {
            return new FieldConfig(name, null, literalValue, false);//, false, false);
        }
        if (isPattern) {
            final PatternLayout layout =
                PatternLayout.newBuilder()
                    .withPattern(pattern)
                    .withConfiguration(config)
                    .withAlwaysWriteExceptions(false)
                    .build();
            return new FieldConfig(name, layout, null, false);//, isUnicode, isClob);
        }

        LOGGER.error("To configure a column you must specify a pattern or literal or set isEventDate to true.");
        return null;
    }
}
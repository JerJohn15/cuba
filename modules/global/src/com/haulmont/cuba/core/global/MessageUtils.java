/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 24.04.2009 10:00:09
 * $Id$
 */
package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.impl.BigDecimalDatatype;
import com.haulmont.chile.core.datatypes.impl.DateDatatype;
import com.haulmont.chile.core.datatypes.impl.IntegerDatatype;
import com.haulmont.chile.core.datatypes.impl.LongDatatype;
import com.haulmont.chile.core.model.*;
import com.haulmont.cuba.core.entity.annotation.LocalizedValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.TemporalType;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

/**
 * Utility class to get localized messages by references defined in XML descriptors
 */
public class MessageUtils {

    private static Log log = LogFactory.getLog(MessageUtils.class);

    /**
     * Prefix pointing that the string is actually a key in a localized messages pack
     */
    public static final String MARK = "msg://";

    private static String messagePack;

    private static char defaultDecimalSeparator;
    private static char defaultGroupingSeparator;
    private static String defaultNumberFormat;

    static {
        DecimalFormat format = new DecimalFormat();
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        defaultDecimalSeparator = symbols.getDecimalSeparator();
        defaultGroupingSeparator = symbols.getGroupingSeparator();
        defaultNumberFormat = format.toPattern();
    }

    public static void setMessagePack(String messagePack) {
        MessageUtils.messagePack = messagePack;
    }

    /**
     * Returns main message pack set for the application
     */
    public static String getMessagePack() {
        return messagePack;
    }

    /**
     * Returns global date format set for the application in the main message pack
     */
    public static String getDateFormat() {
        return MessageProvider.getMessage(messagePack, "dateFormat");
    }
    
    /**
     * Returns global date-time format set for the application in the main message pack
     */
    public static String getDateTimeFormat() {
        return MessageProvider.getMessage(messagePack, "dateTimeFormat");
    }

    /**
     * Returns global DecimalSeparator char set for the application in the main message pack
     */
    public static char getNumberDecimalSeparator() {
        String s = MessageProvider.getMessage(messagePack, "numberDecimalSeparator");
        if ("numberDecimalSeparator".equals(s)) {
            return defaultDecimalSeparator;
        }
        return s.charAt(0);
    }

    /**
     * Returns global GroupingSeparator char set for the application in the main message pack
     */
    public static char getNumberGroupingSeparator() {
        String s = MessageProvider.getMessage(messagePack, "numberGroupingSeparator");
        if ("numberGroupingSeparator".equals(s)) {
            return defaultGroupingSeparator;
        }
        return s.charAt(0);
    }

    public static String getIntegerFormat() {
        String s = MessageProvider.getMessage(messagePack, "integerFormat");
        if ("integerFormat".equals(s)) {
            return defaultNumberFormat;
        }
        return s;
    }

    public static String getLongFormat() {
        String s = MessageProvider.getMessage(messagePack, "longFormat");
        if ("longFormat".equals(s)) {
            return defaultNumberFormat;
        }
        return s;
    }

    public static String getDoubleFormat() {
        String s = MessageProvider.getMessage(messagePack, "doubleFormat");
        if ("doubleFormat".equals(s)) {
            return defaultNumberFormat;
        }
        return s;
    }

    public static String getBigDecimalFormat() {
        String s = MessageProvider.getMessage(messagePack, "bigDecimalFormat");
        if ("bigDecimalFormat".equals(s)) {
            return defaultNumberFormat;
        }
        return s;
    }

    /**
     * Get localized message by reference provided in full format
     * @param ref reference to message in the following format: <code>msg://message_pack/message_id</code>
     * @return localized message or input string itself if it doesn't begin with <code>msg://</code>
     */
    public static String loadString(String ref) {
        return loadString(null, ref);
    }

    /**
     * Get localized message by reference provided in full or brief format
     * @param messagesPack messages pack to use if the second parameter is in brief format
     * @param ref reference to message in the following format:
     * <ul>
     * <li>Full: <code>msg://message_pack/message_id</code>
     * <li>Brief: <code>msg://message_id</code>, in this case first parameter is taken into account
     * </ul>
     * @return localized message or input string itself if it doesn't begin with <code>msg://</code>
     */
    public static String loadString(String messagesPack, String ref) {
        if (ref.startsWith(MARK)) {
            String path = ref.substring(6);
            final String[] strings = path.split("/");
            if (strings.length == 1 && messagesPack != null) {
                ref = MessageProvider.getMessage(messagesPack, strings[0]);
            } else if (strings.length == 2) {
                ref = MessageProvider.getMessage(strings[0], strings[1]);
            } else {
                throw new UnsupportedOperationException("Unsupported resource string format: " + ref);
            }
        }
        return ref;
    }

    /**
     * Get localized name of an entity. Messages pack should be placed in the packet of entity.
     */
    public static String getEntityCaption(MetaClass metaClass) {
        String className = metaClass.getJavaClass().getName();
        int i = className.lastIndexOf('.');
        if (i > -1)
            className = className.substring(i + 1);

        return MessageProvider.getMessage(metaClass.getJavaClass(), className);
    }

    /**
     * Get localized name of an entity property. Messages pack should be placed in the packet of entity.
     */
    public static String getPropertyCaption(MetaClass metaClass, String propertyName) {
        MetaPropertyPath propertyPath = metaClass.getPropertyPath(propertyName);
        return getPropertyCaption(propertyPath.getMetaProperty());
    }

    /**
     * Get localized name of an entity property. Messages pack should be placed in the packet of entity.
     */
    public static String getPropertyCaption(MetaProperty property) {
        Class<?> declaringClass = property.getDeclaringClass();
        String className = declaringClass.getName();
        int i = className.lastIndexOf('.');
        if (i > -1)
            className = className.substring(i + 1);

        return MessageProvider.getMessage(declaringClass, className + "." + property.getName());
    }

    /**
     * Get message key of an entity property. Messages pack part of the key points to the packet of entity.
     */
    public static String getMessageRef(MetaClass metaClass, String propertyName) {
        MetaProperty property = metaClass.getProperty(propertyName);
        if (property == null) {
            throw new RuntimeException("Property " + propertyName + " is wrong for metaclass " + metaClass);
        }
        return getMessageRef(property);
    }

    /**
     * Get message key of an entity property. Messages pack part of the key points to the packet of entity.
     */
    public static String getMessageRef(MetaProperty property) {
        Class<?> declaringClass = property.getDeclaringClass();
        String className = declaringClass.getName();
        String packageName= "";
        int i = className.lastIndexOf('.');
        if (i > -1) {
            packageName = className.substring(0, i);
            className = className.substring(i + 1);
        }

        return MARK + packageName + "/" + className + "." + property.getName();
    }

    /**
     * Get localized value of an attribute based on {@link LocalizedValue} annotation
     * @param attribute attribute name
     * @param instance entity instance
     * @return localized value or the value itself, if the value is null or the message pack can not be inferred
     */
    public static String getLocValue(String attribute, Instance instance) {
        String value = instance.getValue(attribute);
        if (value == null)
            return null;

        String mp = inferMessagePack(attribute, instance);
        if (mp == null)
            return value;
        else
            return MessageProvider.getMessage(mp, value);
    }

    /**
     * Returns message pack inferred from {@link LocalizedValue} annotation
     * @param attribute attribute name
     * @param instance entity instance
     * @return inferred message pack or null
     */
    public static String inferMessagePack(String attribute, Instance instance) {
        MetaClass metaClass = instance.getMetaClass();
        MetaProperty property = metaClass.getProperty(attribute);
        LocalizedValue annotation = property.getAnnotatedElement().getAnnotation(LocalizedValue.class);
        if (annotation != null) {
            if (!StringUtils.isBlank(annotation.messagePack()))
                return annotation.messagePack();
            else if (!StringUtils.isBlank(annotation.messagePackExpr())) {
                try {
                    return instance.getValueEx(annotation.messagePackExpr());
                } catch (Exception e) {
                    log.error("Unable to infer message pack from expression: " + annotation.messagePackExpr(), e);
                }
            }
        }
        return null;
    }

    public static String format(Object value, MetaProperty property) {
        if (value == null)
            return null;

        Range range = property.getRange();
        if (range.isDatatype()) {
            Datatype datatype = range.asDatatype();
            if (datatype.getName().equals(DateDatatype.NAME)) {
                String formatStr;
                TemporalType tt = (TemporalType) property.getAnnotations().get("temporal");
                if (TemporalType.DATE.equals(tt)) {
                    formatStr = MessageUtils.getDateFormat();
                } else {
                    formatStr = MessageUtils.getDateTimeFormat();
                }
                return new SimpleDateFormat(formatStr).format(value);
            } else {
                Class datatypeClass = datatype.getJavaClass();
                if (Number.class.isAssignableFrom(datatypeClass)) {
                    String pattern;
                    if (datatype.getName().equals(IntegerDatatype.NAME)) {
                        pattern = MessageUtils.getIntegerFormat();
                    } else if (datatype.getName().equals(BigDecimalDatatype.NAME)) {
                        pattern = MessageUtils.getBigDecimalFormat();
                    } else if (datatype.getName().equals(LongDatatype.NAME)) {
                        pattern = MessageUtils.getLongFormat();
                    } else {
                        pattern = MessageUtils.getDoubleFormat();
                    }

                    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                    symbols.setDecimalSeparator(MessageUtils.getNumberDecimalSeparator());
                    symbols.setGroupingSeparator(MessageUtils.getNumberGroupingSeparator());

                    DecimalFormat format = new DecimalFormat(pattern, symbols);
                    return format.format(value);
                }
                return value.toString();
            }
        } else if (range.isEnum()) {
            String nameKey = value.getClass().getSimpleName() + "." + value.toString();
            return MessageProvider.getMessage(value.getClass(), nameKey);
        } else {
            if (value instanceof Instance)
                return ((Instance) value).getInstanceName();
            else
                return value.toString();
        }

    }
}

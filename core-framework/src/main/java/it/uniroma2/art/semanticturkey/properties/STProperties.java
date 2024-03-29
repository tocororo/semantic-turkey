package it.uniroma2.art.semanticturkey.properties;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 */
@STProperties.BasicPropertiesConstraints
public interface STProperties {

    @Constraint(validatedBy = BasicPropertyConstraintValidator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface BasicPropertiesConstraints {

        String message() default "Invalid properties";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

    }

    class BasicPropertyConstraintValidator implements ConstraintValidator<Annotation, STProperties> {

        private boolean allowIncomplete;

        public void setAllowIncomplete(boolean allowIncomplete) {
            this.allowIncomplete = allowIncomplete;
        }

        @Override
        public void initialize(Annotation constraintAnnotation) {
            // nothing todo
        }

        @Override
        public boolean isValid(STProperties value, ConstraintValidatorContext context) {
            Collection<String> pars = value.getProperties();

            boolean isValid = true;

            for (String p : pars) {
                Object v;
                boolean isRequired;
                Optional<EnumerationDescription> optEnumDes;
                try {
                    v = value.getPropertyValue(p);
                    isRequired = value.isRequiredProperty(p);
                    optEnumDes = value.getEnumeration(p);
                } catch (PropertyNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (!allowIncomplete && isRequired) {
                    if (v == null) {
                        context.buildConstraintViolationWithTemplate("Required property shall be non null")
                                .addNode(p).addConstraintViolation();
                        isValid = false;
                    } else if (v instanceof Collection<?>) {
                        if (((Collection<?>) v).isEmpty()) {
                            context.buildConstraintViolationWithTemplate(
                                    "Required property shall not be an empty collection").addNode(p)
                                    .addConstraintViolation();
                            isValid = false;
                        }
                    } else if (v instanceof Map<?, ?>) {
                        if (((Map<?, ?>) v).isEmpty()) {
                            context.buildConstraintViolationWithTemplate(
                                    "Required property shall not be an empty map").addNode(p)
                                    .addConstraintViolation();
                            isValid = false;
                        }
                    } else if (v instanceof String) {
                        if (StringUtils.isAllBlank((String) v)) {
                            context.buildConstraintViolationWithTemplate(
                                    "Required property shall not be a blank string").addNode(p)
                                    .addConstraintViolation();
                            isValid = false;
                        }
                    }
                }

                if (v != null) {
                    if (optEnumDes.isPresent()) {
                        EnumerationDescription enumDes = optEnumDes.get();
                        if (!enumDes.isOpen()) {
                            String vAsString = Objects.toString(v);
                            if (!enumDes.getValues().contains(vAsString)) {
                                context.buildConstraintViolationWithTemplate(
                                        "Provided value " + v.toString() + " not allowed").addNode(p)
                                        .addConstraintViolation();
                                isValid = false;
                            }
                        }
                    }
                }

                if (v instanceof Collection<?>) {
                    int i = 0;
                    for (Object item : (Collection<?>) v) {
                        if (isNullish(item)) {
                            context.buildConstraintViolationWithTemplate(
                                    "The collection contains a nullish value at index " + i).addNode(p)
                                    .addConstraintViolation();
                            isValid = false;
                        }
                        i++;
                    }
                }

            }

            return isValid;
        }

        private static boolean isNullish(Object v) {
            return v == null || (v instanceof String && StringUtils.isAllBlank((String) v))
                    || (v instanceof Collection<?> && ((Collection<?>) v).stream()
                    .anyMatch(BasicPropertyConstraintValidator::isNullish))
                    || (v instanceof Map<?, ?> && ((Map<?, ?>) v).values().stream()
                    .anyMatch(BasicPropertyConstraintValidator::isNullish));
        }

    }

    /**
     * @return a humanly understandable short name representing the type of this property set
     */
    String getShortName();

    /**
     * An optional HTML description of this type of propertySet
     *
     * @return an HTML description of this type of propertySet. It may be <code>null</code>, if no description
     * is available
     */
    default String getHTMLDescription() {
        return null;
    }

    /**
     * An optional HTML warning related to this type of propertySet
     *
     * @return An optional HTML warning related to this type of propertySet. It may be <code>null</code>, if
     * no warning is available
     */
    default String getHTMLWarning() {
        return null;
    }

    /**
     * returns all the properties of the class implementing this interface, which have been annotated as
     * {@link STProperty}
     *
     * @return
     */
    default Collection<String> getProperties() {

        // Stores this class and all of its super classes
        List<Class<?>> classes = new ArrayList<>();
        Class<?> cls = this.getClass();
        do {
            classes.add(cls);
            cls = cls.getSuperclass();
        } while (cls != null);

        // Reverse the list of classes, so that super classes come first
        Collections.reverse(classes);

        // Collect all properties, from super classes to sub classes
        Collection<String> properties = new ArrayList<String>();

        for (Class<?> aCls : classes) {
            Field[] fields = aCls.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(STProperty.class))
                    properties.add(field.getName());
            }
        }
        return properties;
    }

    /**
     * gets the value of a property previously set on this set of properties
     *
     * @param id
     * @return
     * @throws PropertyNotFoundException
     */
    default Object getPropertyValue(String id) throws PropertyNotFoundException {
        try {
            Field property = this.getClass().getField(id);
            Object returnedValue = property.get(this);
            return returnedValue;
        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        } catch (IllegalArgumentException e) {
            throw new PropertyNotFoundException(e);
        } catch (IllegalAccessException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    default Type getPropertyType(String id) throws PropertyNotFoundException {
        return getPropertyAnnotatedType(id).getType();
    }

    default AnnotatedType getPropertyAnnotatedType(String id) throws PropertyNotFoundException {
        try {
            Field property = this.getClass().getField(id);
            return property.getAnnotatedType();
        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        } catch (IllegalArgumentException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    default void setPropertyValue(String id, Object value) throws WrongPropertiesException {

        Field prop = null;
        try {
            prop = this.getClass().getField(id);
            if (value != null) {
                value = checkAndConvertPropertyValue(id, value, getPropertyType(id));
            }
            prop.set(this, value);
        } catch (SecurityException e) {
            throw new WrongPropertiesException(e);
        } catch (NoSuchFieldException e) {
            throw new WrongPropertiesException("there is no property called: " + id);
        } catch (IllegalArgumentException e) {
            throw new WrongPropertiesException(e);
        } catch (IllegalAccessException e) {
            throw new WrongPropertiesException(e);
        } catch (PropertyNotFoundException e) {
            throw new WrongPropertiesException(e);
        }
    }

    default Object checkAndConvertPropertyValue(String id, Object value, Type propType)
            throws IllegalArgumentException, PropertyNotFoundException {
        if ((value.getClass() == String.class) && (propType != String.class)) {
            value = convertToPropertValue(propType, value);
        }

        Optional<EnumerationDescription> enumerationHolder = getEnumeration(id);
        if (enumerationHolder.isPresent()) {
            EnumerationDescription enumeration = enumerationHolder.get();

            if (!enumeration.isOpen()) {
                Object tempValue = value;

                enumeration.getValues().stream().filter(v -> v.equals(tempValue.toString())).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("Value \"" + tempValue.toString()
                                + "\" is not assignable to property \"" + id + "\""));
            }
        }
        return value;
    }

    /**
     * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
     * <code>properties</code>
     *
     * @param props
     * @throws WrongPropertiesException
     */
    default void setProperties(Properties props) throws WrongPropertiesException {
        Enumeration<?> propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String propName = propNames.nextElement().toString();
            setPropertyValue(propName, props.getProperty(propName));
        }
    }

    /**
     * invokes {@link #setPropertyValue(String, Object)} on each of the property/value pairs found in
     * <code>propertyFile</code>
     *
     * @param propertyFile
     * @throws WrongPropertiesException
     * @throws IOException
     */
    default void loadProperties(File propertyFile) throws WrongPropertiesException, IOException {
        Properties props = new java.util.Properties();
        FileReader fileReader = new FileReader(propertyFile);
        props.load(fileReader);
        setProperties(props);
        fileReader.close();
    }

    /**
     * stores the properties in this instance in file <code>propertyFile</code>
     *
     * @param propertyFile
     * @throws IOException
     * @throws WrongPropertiesException
     */
    default void storeProperties(File propertyFile) throws IOException, WrongPropertiesException {
        Properties props = new java.util.Properties();
        try (FileWriter fileWriter = new FileWriter(propertyFile)) {
            storeProperties(props);
            props.store(fileWriter, "list of properties");
        }
    }

    /**
     * stores the properties in this instance in the given {@link Properties} object <code>properties</code>
     *
     * @param properties
     * @throws IOException
     * @throws WrongPropertiesException
     */
    default void storeProperties(Properties properties) throws IOException, WrongPropertiesException {
        Collection<String> pars = getProperties();
        try {
            for (String par : pars) {
                Object value = getPropertyValue(par);
                if (value == null)
                    continue; // skip null values
                properties.setProperty(par, value.toString());
            }
        } catch (PropertyNotFoundException e) {
            throw new WrongPropertiesException(e);
        }

    }

    /**
     * get the expected type of content for the property. Can be used by external tools to drive the
     * acquisition of that value or to check
     *
     * @param parID
     * @return
     * @throws PropertyNotFoundException
     */
    default String getPropertyContentType(String parID) throws PropertyNotFoundException {
        try {
            Field field = this.getClass().getField(parID);

            if (!field.isAnnotationPresent(ContentType.class))
                return null;

            ContentType annotation = field.getAnnotation(ContentType.class);
            return annotation.value();

        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    default boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
        try {
            Field field = this.getClass().getField(parID);

            if (field.isAnnotationPresent(Required.class))
                return true;
            else
                return false;

        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    /**
     * tells if this set of properties needs to be explicitly set by the user or if it can be used in its
     * default settings
     *
     * @return
     */
    default boolean hasRequiredProperties() {
        Field[] fields = this.getClass().getFields();
        for (int i = 0; i < fields.length; i++)
            if (fields[i].isAnnotationPresent(Required.class))
                return true;
        return false;
    }

    /**
     * this method returns the description of a property
     *
     * @param id
     * @return
     * @throws PropertyNotFoundException
     */
    default String getPropertyDescription(String id) throws PropertyNotFoundException {
        try {
            Field field = this.getClass().getField(id);
            if (field.isAnnotationPresent(STProperty.class))
                return ((STProperty) field.getAnnotation(STProperty.class)).description();
            else
                throw new PropertyNotFoundException("Property: " + id + " not found");
        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    /**
     * this method returns the displayName of a property. If not provided, return the property name
     *
     * @param id
     * @return
     * @throws PropertyNotFoundException
     */
    default String getPropertyDisplayName(String id) throws PropertyNotFoundException {
        try {
            Field field = this.getClass().getField(id);
            if (field.isAnnotationPresent(STProperty.class)) {
                String propValue = ((STProperty) field.getAnnotation(STProperty.class)).displayName();
                if (propValue.equals("")) { // not provided, empty string is the default
                    return id;
                } else {
                    return propValue;
                }
            } else
                throw new PropertyNotFoundException("Property: " + id + " not found");
        } catch (SecurityException e) {
            throw new PropertyNotFoundException(e);
        } catch (NoSuchFieldException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    default Object convertToPropertValue(Type type, Object value) {
        if (type == Boolean.class || type == boolean.class) {
            value = Boolean.parseBoolean((String) value);
        } else if (type == Long.class || type == long.class) {
            value = Long.parseLong((String) value);
        } else if (type == Integer.class || type == int.class) {
            value = Integer.parseInt((String) value);
        } else if (type == Double.class || type == double.class) {
            value = Double.parseDouble((String) value);
        } else if (TypeUtils.isAssignable(type, Value.class)) {
            value = NTriplesUtil.parseValue((String) value, SimpleValueFactory.getInstance());
            if (!((Class<?>) type).isInstance(value)) {
                throw new IllegalArgumentException("The provided value \"" + value
                        + "\" is not a serialization of an RDF4J " + ((Class<?>) type).getSimpleName());
            }

        } else if (TypeUtils.isAssignable(type, Enum.class)) {
            value = EnumUtils.getEnum((Class)type, (String)value);
        }
        return value;
    }

    default String getStringRepresentation() {
        Collection<String> pars = getProperties();
        StringBuffer stringed = new StringBuffer(
                "STProperties [" + this.getClass().getCanonicalName() + "\n");
        for (String par : pars) {
            String value;
            try {
                value = getPropertyValue(par).toString();
            } catch (PropertyNotFoundException e) {
                value = "propNotFound!";
            }
            stringed.append(par + ": " + value + "\n");
        }
        stringed.append("]");
        return stringed.toString();
    }

    /**
     * Tells if the property with given {@code id} is enumerated: either its type is an enum (see {@link #getPropertyType(String)}) or it is annotated with {@link Enumeration}.
     *
     * @param id
     * @return
     * @throws PropertyNotFoundException
     */
    default boolean isEnumerated(String id) throws PropertyNotFoundException {
        try {
            if (TypeUtils.isAssignable(getPropertyType(id), Enum.class)) return true;

            Field field = this.getClass().getField(id);
            return field.isAnnotationPresent(it.uniroma2.art.semanticturkey.properties.Enumeration.class);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    /**
     * Returns the enumeration (if any) associated with the property with given {@code id}.
     *
     * @param id
     * @return
     */
    default Optional<EnumerationDescription> getEnumeration(String id) throws PropertyNotFoundException {
        Type propertyType = getPropertyType(id);
        if (TypeUtils.isAssignable(propertyType, Enum.class)) {
            return Optional.of(new EnumerationDescription(Arrays.stream(((Class) propertyType).getEnumConstants()).map(Object::toString).collect(Collectors.toList()), false));
        } else {
            try {

                Field field = this.getClass().getField(id);
                return Optional
                        .ofNullable(
                                field.getAnnotation(it.uniroma2.art.semanticturkey.properties.Enumeration.class))
                        .map(EnumerationDescription::fromAnnotation);
            } catch (NoSuchFieldException | SecurityException e) {
                return Optional.empty();
            }
        }
    }

    /**
     * Returns the annotations (if any) associated with the property with given {@code id}.
     *
     * @param id
     * @return
     * @throws PropertyNotFoundException
     */
    default Annotation[] getAnnotations(String id) throws PropertyNotFoundException {
        getPropertyDescription(id); // just used to check the existence of the property
        try {
            return this.getClass().getField(id).getAnnotations();
        } catch (NoSuchFieldException | SecurityException e) {
            throw new PropertyNotFoundException(e);
        }
    }

    /**
     * Deep copy properties from <code>fromProps</code> into <code>toProps</code>. Merge is performed in place in the
     * argument <code>toProps</code>, possibly sharing resources from the argument <code>fromProps</code>.
     *
     * @param toProps
     * @param fromProps
     * @param <T>
     */
    public static <T extends STProperties> void deepMerge(T toProps, T fromProps) {
        try {
            for (String propName : toProps.getProperties()) {
                Object fromPropertyValue = fromProps.getPropertyValue(propName);
                if (fromPropertyValue != null) {
                    Type propType = toProps.getPropertyType(propName);
                    if (TypeUtils.isAssignable(propType, STProperties.class)) {
                        Object toPropertyValue = toProps.getPropertyValue(propName);
                        Object mergedValue;
                        if (toPropertyValue == null) {
                            mergedValue = fromPropertyValue;
                        } else {
                            deepMerge((STProperties)toPropertyValue, (STProperties)fromPropertyValue);
                            mergedValue = (STProperties)toPropertyValue;
                        }
                        toProps.setPropertyValue(propName, mergedValue);
                    } else {
                        toProps.setPropertyValue(propName, fromPropertyValue);
                    }
                }
            }
        } catch (PropertyNotFoundException | WrongPropertiesException e) {
            throw new RuntimeException(e);
        }
    }

}

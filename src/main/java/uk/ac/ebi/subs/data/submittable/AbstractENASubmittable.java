package uk.ac.ebi.subs.data.submittable;

import uk.ac.ebi.subs.data.component.Attribute;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.ena.annotation.ENAControlledValueAttribute;
import uk.ac.ebi.subs.ena.annotation.ENAField;
import uk.ac.ebi.subs.ena.annotation.ENAFieldAttribute;
import uk.ac.ebi.subs.ena.annotation.ENAValidation;
import uk.ac.ebi.subs.ena.validation.AttributeRequiredValidationResult;
import uk.ac.ebi.subs.ena.validation.InvalidAttributeValue;
import uk.ac.ebi.subs.ena.validation.SingleAttributeValidationResult;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by neilg on 03/03/2017.
 */
public abstract class AbstractENASubmittable<T extends BaseSubmittable> implements ENASubmittable<T>  {
    private static final String MULTIPLE_VALUES_ERROR_MESSAGE = "Multiple values found for attribute %s.";
    static final String ATTRIBUTE_VALUE_REQUIRED_ERROR_MESSAGE = "Value for attribute %s is required.";
    private static final String INVALID_VALUE_ERROR_MESSAGE = "Invalid value for attribute %s value must be one of %s.";
    public static final String USI_TEAM_PREFIX = "@USI-";

    private Submittable baseSubmittable;
    private List<SingleValidationResult> validationResultList = new ArrayList<>();
    Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();


    public AbstractENASubmittable(Submittable baseSubmittable) throws IllegalAccessException {
        setBaseSubmittable(baseSubmittable);
    }

    public AbstractENASubmittable() {
        this.baseSubmittable = createNewSubmittable();
    }

    public void setBaseSubmittable(Submittable baseSubmittable) throws IllegalAccessException {
        this.baseSubmittable = baseSubmittable;
        serialiseAttributes();
    }


    Optional<Attribute> getExistingAttribute(String attributeName) {
        List<Attribute> attributeList = getAttributes();
        return attributeList.stream().filter(attribute -> (attribute.getName() != null && attribute.getName().equalsIgnoreCase(attributeName))).findFirst();
    }

    private int getAttributeCount(String attributeName) {
        final List<Attribute> attributeList = getAttributes();
        if (attributeList == null) {
            return 0;
        } else {
            return (int)attributeList.stream().filter(attribute -> (attribute.getName() != null && attribute.getName().equalsIgnoreCase(attributeName))).count();
        }
    }

    public List<String> getAttributeValueList (String attributeName) {
        List<String> attributeValueList = new ArrayList<>();
        for (Attribute attribute : getAttributes()) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) {
                attributeValueList.add(attribute.getValue());
            }
        }
        return attributeValueList;
    }

    void deleteAttribute(Attribute attribute) {
        getAttributes().remove(attribute);
    }

    public void serialiseAttributes () throws IllegalAccessException {
        if (this.getClass().isAnnotationPresent(ENAValidation.class)) {
            ENAValidation enaValidation = getEnaValidation();

            if (getId() == null )
                setId(UUID.randomUUID().toString());
            parseAttributes(enaValidation);
            parseControlledAttributes(enaValidation);
            serialiseFields(this.getClass(), this);
            if (validationResultList.isEmpty()) {
                for (Attribute attribute : attributeMap.values()) {
                    deleteAttribute(attribute);
                }
            }
        }
    }

    ENAValidation getEnaValidation() {
        return this.getClass().getAnnotation(ENAValidation.class);
    }

    private void parseAttributes(ENAValidation enaValidation) {
        for (ENAFieldAttribute enaFieldAttribute : enaValidation.value()) {
            String fieldName;
            if (!enaFieldAttribute.fieldName().equals(ENAFieldAttribute.NO_FIELD)) {
                fieldName = enaFieldAttribute.fieldName();
            }
             else {
                fieldName = enaFieldAttribute.attributeName();
            }
            parseField(enaFieldAttribute, fieldName);
        }

        for (ENAFieldAttribute enaFieldAttribute : enaValidation.value()) {
            String fieldName = null;
            if (!enaFieldAttribute.attributeFieldName().equals(ENAFieldAttribute.NO_FIELD)) {
                if (attributeMap.containsKey(enaFieldAttribute.attributeFieldName().toUpperCase())) {
                    fieldName = attributeMap.get(enaFieldAttribute.attributeFieldName().toUpperCase()).getValue();
                    parseField(enaFieldAttribute, fieldName);
                }
            }
        }
    }

    private void parseField(ENAFieldAttribute enaFieldAttribute, String fieldName) {
        String attributeName = enaFieldAttribute.attributeName();
        final int attributeCount = getAttributeCount(attributeName);

        if (attributeCount > 1) {
            validationResultList.add(new SingleAttributeValidationResult(this, attributeName));
        } else if (attributeCount == 0 && enaFieldAttribute.required()) {
            validationResultList.add(new AttributeRequiredValidationResult(this,attributeName));
        } else if (attributeCount > 0 ) {
            final Optional<Attribute> attribute = getExistingAttribute(attributeName);
            attributeMap.put(fieldName.toUpperCase(),attribute.get());
        }
    }

    private void parseControlledAttributes (ENAValidation enaValidation) {
        for (ENAControlledValueAttribute enaControlledValueAttribute : enaValidation.enaControlledValueAttributes()) {
            String attributeName = enaControlledValueAttribute.attributeName();
            List<String> allowedValueList = Arrays.asList(enaControlledValueAttribute.allowedValues());
            final Optional<Attribute> optionalAttribute = getExistingAttribute(attributeName);
            if (optionalAttribute.isPresent())
                parseControlledValue(optionalAttribute.get(), allowedValueList);
        }
    }

    private boolean parseControlledValue(Attribute attribute, List<String> allowedValueList) {
        if (!allowedValueList.contains(attribute.getValue())) {
            validationResultList.add(
                    new InvalidAttributeValue(
                            this,
                            attribute.getValue(),
                            attribute.getName(),
                            allowedValueList.toArray(new String[0])));
            return false;
        } else {
            return true;
        }
    }

    private void serialiseFields(Class<?> aClass, Object obj) throws IllegalAccessException {

        final Field[] fields = aClass.getDeclaredFields();

        for (Field field : fields ) {
            if (field.isAnnotationPresent(ENAField.class)) {
                final ENAField enaField = field.getAnnotation(ENAField.class);
                if (attributeMap.containsKey(enaField.fieldName().toUpperCase())) {
                    final Attribute attribute = attributeMap.get(enaField.fieldName().toUpperCase());
                    if (enaField.values().length > 0) {
                        if (!parseControlledValue(attribute,Arrays.asList(enaField.values()))) {
                            break;
                        }
                    }
                    field.set(obj, attribute.getValue());
                    deleteAttribute(attribute);
                }
            }

        }
    }

    public void deSerialiseAttributes () throws IllegalAccessException {
        deSerialiseFields(this.getClass(), this);
    }

    private void deSerialiseFields (Class<?> aClass, Object obj) throws IllegalAccessException {
        Map<String,String> fieldMap = new HashMap<>();
        Map<String,String> attributefieldMap = new HashMap<>();

        if (this.getClass().isAnnotationPresent(ENAValidation.class)) {
            ENAValidation enaValidation = getEnaValidation();
            for (ENAFieldAttribute enaFieldAttribute : enaValidation.value()) {
                String fieldName = null;
                if (enaFieldAttribute.fieldName().equals(ENAFieldAttribute.NO_FIELD)) {
                    fieldName = enaFieldAttribute.attributeName();
                } else {
                    fieldName = enaFieldAttribute.fieldName();
                }
                if (!enaFieldAttribute.attributeFieldName().equals(ENAFieldAttribute.NO_FIELD)) {
                    attributefieldMap.put(fieldName,enaFieldAttribute.attributeFieldName());
                } else {
                    fieldMap.put(fieldName,enaFieldAttribute.attributeName());
                }
            }
        }

        final Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields ) {
            if (field.isAnnotationPresent(ENAField.class)) {
                final ENAField enaField = field.getAnnotation(ENAField.class);
                final Object o = field.get(obj);
                if ( o != null ) {
                    if (enaField.attributeName() != null && attributefieldMap.get(enaField.attributeName()) != null) {
                        Attribute attribute1 = new Attribute();
                        attribute1.setName(attributefieldMap.get(enaField.attributeName()));
                        attribute1.setValue(enaField.fieldName());
                        getAttributes().add(attribute1);
                        Attribute attribute2 = new Attribute();
                        attribute2.setName(enaField.attributeName());
                        attribute2.setValue(o.toString());
                        getAttributes().add(attribute2);
                    } else {
                        Attribute attribute = new Attribute();
                        attribute.setName(enaField.fieldName());
                        attribute.setValue(o.toString());
                        getAttributes().add(attribute);
                    }
                }
            } else if (field.getType().isMemberClass()) {
                deSerialiseFields(field.getType(),field.get(obj));
            }

        }
    }


    @Override
    public String getId() {
        return baseSubmittable.getId();
    }

    @Override
    public void setId(String id) {
        baseSubmittable.setId(id);
    }

    @Override
    public String getAccession() {
        return baseSubmittable.getAccession();
    }

    @Override
    public void setAccession(String accession) {
        baseSubmittable.setAccession(accession);
    }

    @Override
    public String getAlias() {
        return getENAAlias(baseSubmittable.getAlias(),getTeamName());
    }

    @Override
    public void setAlias(String alias) {
        baseSubmittable.setAlias(removeENAAlias(alias));
    }

    @Override
    public Team getTeam() {
        return baseSubmittable.getTeam();
    }

    @Override
    public void setTeam(Team team) {
        baseSubmittable.setTeam(team);
    }

    @Override
    public String getTitle() {
        return baseSubmittable.getTitle();
    }

    @Override
    public void setTitle(String title) {
        baseSubmittable.setTitle(title);
    }

    @Override
    public String getDescription() {
        return baseSubmittable.getDescription();
    }

    @Override
    public void setDescription(String description) {
        baseSubmittable.setDescription(description);
    }

    /**
     * Return null for a empty list to prevent moxy from creating an empty attributes element as the schema doesn't allow this
     */
    @Override
    public List<Attribute> getAttributes() {
        return baseSubmittable.getAttributes();
    }

    @Override
    public void setAttributes(List<Attribute> attributes) {
        baseSubmittable.setAttributes(attributes);
    }

    @Override
    public boolean isAccessioned() {
        return baseSubmittable.isAccessioned();
    }

    public String getTeamName() {
        Team team = getTeam();
        if (team != null)
            return team.getName();
        else return null;
    }

    public void setTeamName(String teamName) {
        Team team = new Team();
        team.setName(teamName);
        setTeam(team);
    }

    @Override
    public T getBaseObject() {
        return (T)baseSubmittable;
    }

    @Override
    public List<Attribute> getAttributesXML() {
        if (baseSubmittable.getAttributes().isEmpty()) {
            return null;
        } else {
            return baseSubmittable.getAttributes();
        }
    }

    @Override
    public void setAttributesXML(List<Attribute> attributeList) {
        baseSubmittable.setAttributes(attributeList);
    }

    public List<SingleValidationResult> getValidationResultList() {
        return validationResultList;
    }

    @Override
    public boolean isValid() {
        return validationResultList.isEmpty();
    }

    public static String getENAAlias (String alias, String teamName) {
        if (alias != null)
            return alias + USI_TEAM_PREFIX + teamName;
        else return null;
    }

    public static String removeENAAlias (String alias) {
        if (alias.indexOf(USI_TEAM_PREFIX) > 0)
            return alias.substring(0, alias.lastIndexOf("@"));
        else {
            return alias;
        }
    }

}

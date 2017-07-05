package uk.ac.ebi.subs.ena.annotation;

import java.lang.annotation.*;

/**
 * Annotation that is used to tag a member of a class that extends ENASubmittable .
 * Used to copy values from the submittables attributes to the member value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ENAAttributes.class)
public @interface ENAAttribute {
    /**
     * The name of the attribute who's value will be copied over
     * @return
     */
    String name();
    boolean required() default false;
    String [] allowedValues() default {};
}

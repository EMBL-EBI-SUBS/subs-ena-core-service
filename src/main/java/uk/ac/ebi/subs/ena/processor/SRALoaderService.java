package uk.ac.ebi.subs.ena.processor;

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.subs.data.submittable.ENASubmittable;

import java.io.IOException;
import java.sql.Connection;

/**
 * Created by neilg on 12/04/2017.
 */
public interface SRALoaderService<T extends ENASubmittable> {
    void executeSRALoader (T enaSubmittable, String submissionAlias, Connection connection) throws Exception;
    String executeSRALoader(String submissionXML, String submittableXML, Connection connection) throws Exception;
    ValidationResult getValidationResult();
}

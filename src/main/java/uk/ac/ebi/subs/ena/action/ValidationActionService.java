package uk.ac.ebi.subs.ena.action;

import org.springframework.stereotype.Service;
import uk.ac.ebi.ena.sra.xml.SubmissionType;

@Service
public class ValidationActionService implements ActionService<Boolean> {

    public SubmissionType.ACTIONS.ACTION createActionXML(Boolean validation) {
        final SubmissionType.ACTIONS.ACTION action = SubmissionType.ACTIONS.ACTION.Factory.newInstance();
        action.addNewVALIDATE();
        return action;
    }
}

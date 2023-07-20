package owt.training.apachecamel.route;

import ca.uhn.hl7v2.model.v24.message.ORU_R01;
import ca.uhn.hl7v2.model.v24.segment.PID;
import org.apache.camel.builder.RouteBuilder;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class FhirRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("file:{{application.fhir.input}}")
                .messageHistory()
                .routeId("read-fhir-patient-data")
                .log("Converting ${file:name}.")
                // unmarshall file to hl7 message
                .unmarshal().hl7()
                .process(exchange -> {
                    System.out.println("Mapping from a HLV2 patient to r4 patient ...");
                    ORU_R01 msg = exchange.getIn().getBody(ORU_R01.class);
                    final PID pid = msg.getPATIENT_RESULT().getPATIENT().getPID();
                    String surname = pid.getPatientName()[0].getFamilyName().getFn1_Surname().getValue();
                    String name = pid.getPatientName()[0].getGivenName().getValue();
                    String patientId = msg.getPATIENT_RESULT().getPATIENT().getPID().getPatientID().getCx1_ID().getValue();
                    Patient patient = new Patient();
                    patient.addName().addGiven(name);
                    patient.getNameFirstRep().setFamily(surname);
                    patient.setId(patientId);
                    exchange.getIn().setBody(patient);
                })
//                // marshall to JSON
                .marshal().fhirJson("{{application.fhir.version}}")
                .convertBodyTo(String.class)
                .log("Inserting Patient: ${body}")
                .to("jms:queue:HELLO.WORLD");
    }
}

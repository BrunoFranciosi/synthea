package org.mitre.synthea.export;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.MoneyDt;
import ca.uhn.fhir.model.dstu2.composite.NarrativeDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt.Repeat;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
import ca.uhn.fhir.model.dstu2.resource.CarePlan.Activity;
import ca.uhn.fhir.model.dstu2.resource.CarePlan.ActivityDetail;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Encounter.Hospitalization;
import ca.uhn.fhir.model.dstu2.resource.Immunization;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder.DosageInstruction;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Patient.Communication;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.CarePlanActivityStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.CarePlanStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ClaimTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticReportStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import ca.uhn.fhir.model.dstu2.valueset.GoalStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.MaritalStatusCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.NameUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.dstu2.valueset.UseEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.IntegerDt;
import ca.uhn.fhir.model.primitive.PositiveIntDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.XhtmlDt;
import ca.uhn.fhir.util.XmlUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Point;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.CarePlan;
import org.mitre.synthea.world.concepts.HealthRecord.Claim;
import org.mitre.synthea.world.concepts.HealthRecord.ClaimItem;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.concepts.HealthRecord.Report;

public class FhirDstu2 {
  // HAPI FHIR warns that the context creation is expensive, and should be performed
  // per-application, not per-record
  private static final FhirContext FHIR_CTX = FhirContext.forDstu2();

  private static final String SNOMED_URI = "http://snomed.info/sct";
  private static final String LOINC_URI = "http://loinc.org";
  private static final String RXNORM_URI = "http://www.nlm.nih.gov/research/umls/rxnorm";
  private static final String CVX_URI = "http://hl7.org/fhir/sid/cvx";
  private static final String DISCHARGE_URI = "http://www.nubc.org/patient-discharge";
  private static final String SYNTHEA_EXT = "http://synthetichealth.github.io/synthea/";

  private static final Map raceEthnicityCodes = loadRaceEthnicityCodes();
  private static final Map languageLookup = loadLanguageLookup();

  @SuppressWarnings("rawtypes")
  private static Map loadRaceEthnicityCodes() {
    String filename = "race_ethnicity_codes.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  @SuppressWarnings("rawtypes")
  private static Map loadLanguageLookup() {
    String filename = "language_lookup.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Convert the given Person into a JSON String, containing a FHIR Bundle of the Person and the
   * associated entries from their health record.
   * 
   * @param person
   *          Person to generate the FHIR JSON for
   * @param stopTime
   *          Time the simulation ended
   * @return String containing a JSON representation of a FHIR Bundle containing the Person's health
   *         record
   */
  public static String convertToFHIR(Person person, long stopTime) {
    Bundle bundle = new Bundle();
    bundle.setType(BundleTypeEnum.COLLECTION);

    Entry personEntry = basicInfo(person, bundle, stopTime);

    for (Encounter encounter : person.record.encounters) {
      Entry encounterEntry = encounter(personEntry, bundle, encounter);

      for (HealthRecord.Entry condition : encounter.conditions) {
        condition(personEntry, bundle, encounterEntry, condition);
      }

      for (Observation observation : encounter.observations) {
        observation(personEntry, bundle, encounterEntry, observation);
      }

      for (Procedure procedure : encounter.procedures) {
        procedure(personEntry, bundle, encounterEntry, procedure);
      }

      for (Medication medication : encounter.medications) {
        medication(personEntry, bundle, encounterEntry, medication);
      }

      for (HealthRecord.Entry immunization : encounter.immunizations) {
        immunization(personEntry, bundle, encounterEntry, immunization);
      }

      for (Report report : encounter.reports) {
        report(personEntry, bundle, encounterEntry, report);
      }

      for (CarePlan careplan : encounter.careplans) {
        careplan(personEntry, bundle, encounterEntry, careplan);
      }

      // one claim per encounter
      encounterClaim(personEntry, bundle, encounterEntry, encounter.claim);
    }

    String bundleJson = FHIR_CTX.newJsonParser().setPrettyPrint(true)
        .encodeResourceToString(bundle);

    return bundleJson;
  }

  /**
   * Map the given Person to a FHIR Patient resource, and add it to the given Bundle.
   * 
   * @param person
   *          The Person
   * @param bundle
   *          The Bundle to add to
   * @param stopTime
   *          Time the simulation ended
   * @return The created Entry
   */
  private static Entry basicInfo(Person person, Bundle bundle, long stopTime) {
    Patient patientResource = new Patient();

    patientResource.addIdentifier().setSystem("https://github.com/synthetichealth/synthea")
        .setValue((String) person.attributes.get(Person.ID));

    patientResource.addIdentifier()
        .setType(IdentifierTypeCodesEnum.MR)
        .setSystem("http://hospital.smarthealthit.org")
        .setValue((String) person.attributes.get(Person.ID));

    patientResource.addIdentifier()
        .setType(IdentifierTypeCodesEnum.SOCIAL_BENEFICIARY_IDENTIFIER)
        .setSystem("http://hl7.org/fhir/sid/us-ssn")
        .setValue((String) person.attributes.get(Person.IDENTIFIER_SSN));

    if (person.attributes.get(Person.IDENTIFIER_DRIVERS) != null) {
      patientResource.addIdentifier()
          .setType(IdentifierTypeCodesEnum.DL)
          .setSystem("urn:oid:2.16.840.1.113883.4.3.25")
          .setValue((String) person.attributes.get(Person.IDENTIFIER_DRIVERS));
    }

    ExtensionDt raceExtension = new ExtensionDt();
    raceExtension.setUrl("http://hl7.org/fhir/StructureDefinition/us-core-race");
    String race = (String) person.attributes.get(Person.RACE);
    
    ExtensionDt ethnicityExtension = new ExtensionDt();
    ethnicityExtension.setUrl("http://hl7.org/fhir/StructureDefinition/us-core-ethnicity");
    String ethnicity = (String) person.attributes.get(Person.ETHNICITY);
    
    if (race.equals("hispanic")) {
      race = "other";
      ethnicity = "hispanic";
    } else {
      ethnicity = "nonhispanic";
    }
    
    String raceDisplay;    
    switch (race) {
      case "white":
        raceDisplay = "White";
        break;
      case "black":
        raceDisplay = "Black or African American";
        break;
      case "asian":
        raceDisplay = "Asian";
        break;
      case "native":
        raceDisplay = "American Indian or Alaska Native";
        break;
      default: // Hispanic or Other (Put Hawaiian and Pacific Islander here for now)
        raceDisplay = "Other";
        break;
    }
    
    String ethnicityDisplay;
    if (ethnicity.equals("hispanic")) {
      ethnicityDisplay = "Hispanic or Latino";
    } else {
      ethnicityDisplay = "Not Hispanic or Latino";
    }
    
    Code raceCode = new Code(
        "http://hl7.org/fhir/v3/Race",
        (String) raceEthnicityCodes.get(race),
        raceDisplay);
    
    Code ethnicityCode = new Code(
        "http://hl7.org/fhir/v3/Ethnicity",
        (String) raceEthnicityCodes.get(ethnicity),
        ethnicityDisplay);
    
    raceExtension.setValue(mapCodeToCodeableConcept(raceCode, "http://hl7.org/fhir/v3/Race"));
    ethnicityExtension.setValue(mapCodeToCodeableConcept(ethnicityCode, "http://hl7.org/fhir/v3/Ethnicity"));

    patientResource.addUndeclaredExtension(raceExtension);
    patientResource.addUndeclaredExtension(ethnicityExtension);

    String firstLanguage = (String) person.attributes.get(Person.FIRST_LANGUAGE);
    Map languageMap = (Map) languageLookup.get(firstLanguage);
    Code languageCode = new Code((String) languageMap.get("system"),
        (String) languageMap.get("code"), (String) languageMap.get("display"));
    List<Communication> communication = new ArrayList<Communication>();
    Communication language = new Communication();
    language
        .setLanguage(mapCodeToCodeableConcept(languageCode, (String) languageMap.get("system")));
    communication.add(language);
    patientResource.setCommunication(communication);

    HumanNameDt name = patientResource.addName();
    name.setUse(NameUseEnum.OFFICIAL);
    name.addGiven((String) person.attributes.get(Person.FIRST_NAME));
    List<StringDt> officialFamilyNames = new ArrayList<StringDt>();
    officialFamilyNames.add(new StringDt((String) person.attributes.get(Person.LAST_NAME)));
    name.setFamily(officialFamilyNames);
    if (person.attributes.get(Person.NAME_PREFIX) != null) {
      name.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
    }
    if (person.attributes.get(Person.NAME_SUFFIX) != null) {
      name.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
    }
    if (person.attributes.get(Person.MAIDEN_NAME) != null) {
      HumanNameDt maidenName = patientResource.addName();
      maidenName.setUse(NameUseEnum.MAIDEN);
      maidenName.addGiven((String) person.attributes.get(Person.FIRST_NAME));
      List<StringDt> maidenFamilyNames = new ArrayList<StringDt>();
      maidenFamilyNames.add(new StringDt((String) person.attributes.get(Person.MAIDEN_NAME)));
      maidenName.setFamily(maidenFamilyNames);
      if (person.attributes.get(Person.NAME_PREFIX) != null) {
        maidenName.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
      }
      if (person.attributes.get(Person.NAME_SUFFIX) != null) {
        maidenName.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
      }
    }

    ExtensionDt mothersMaidenNameExtension = new ExtensionDt();
    mothersMaidenNameExtension
        .setUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    String mothersMaidenName = (String) person.attributes.get(Person.NAME_MOTHER);
    mothersMaidenNameExtension.setValue(new StringDt(mothersMaidenName));
    patientResource.addUndeclaredExtension(mothersMaidenNameExtension);

    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    patientResource.setBirthDate(new DateDt(new Date(birthdate)));

    Point coord = (Point) person.attributes.get(Person.COORDINATE);

    AddressDt addrResource = patientResource.addAddress();
    addrResource.addLine((String) person.attributes.get(Person.ADDRESS))
        .setCity((String) person.attributes.get(Person.CITY))
        .setPostalCode((String) person.attributes.get(Person.ZIP))
        .setState((String) person.attributes.get(Person.STATE)).setCountry("US");

    ExtensionDt geolocationExtension = new ExtensionDt();
    geolocationExtension.setUrl("http://hl7.org/fhir/StructureDefinition/geolocation");
    ExtensionDt latitudeExtension = new ExtensionDt();
    ExtensionDt longitudeExtension = new ExtensionDt();
    latitudeExtension.setUrl("latitude");
    longitudeExtension.setUrl("longitude");
    latitudeExtension.setValue(new DecimalDt(coord.getY()));
    longitudeExtension.setValue(new DecimalDt(coord.getX()));
    geolocationExtension.addUndeclaredExtension(latitudeExtension);
    geolocationExtension.addUndeclaredExtension(longitudeExtension);
    addrResource.addUndeclaredExtension(geolocationExtension);

    AddressDt birthplace = new AddressDt();
    birthplace.setCity((String) person.attributes.get(Person.BIRTHPLACE)).setState("MA")
        .setCountry("US");
    ExtensionDt birthplaceExtension = new ExtensionDt();
    birthplaceExtension.setUrl("http://hl7.org/fhir/StructureDefinition/birthPlace");
    birthplaceExtension.setValue(birthplace);
    patientResource.addUndeclaredExtension(birthplaceExtension);

    if (person.attributes.get(Person.MULTIPLE_BIRTH_STATUS) != null) {
      patientResource.setMultipleBirth(
          new IntegerDt((int) person.attributes.get(Person.MULTIPLE_BIRTH_STATUS)));
    } else {
      patientResource.setMultipleBirth(new BooleanDt(false));
    }

    patientResource.addTelecom().setSystem(ContactPointSystemEnum.PHONE)
        .setUse(ContactPointUseEnum.HOME).setValue((String) person.attributes.get(Person.TELECOM));

    String maritalStatus = ((String) person.attributes.get(Person.MARITAL_STATUS));
    if (maritalStatus != null) {
      patientResource.setMaritalStatus(MaritalStatusCodesEnum.forCode(maritalStatus.toUpperCase()));
    } else {
      patientResource.setMaritalStatus(MaritalStatusCodesEnum.S);
    }

    if (!person.alive(stopTime)) {
      patientResource.setDeceased(convertFhirDateTime(person.record.death, true));
    }

    String generatedBySynthea = "Generated by <a href=\"https://github.com/synthetichealth/synthea\">Synthea</a>."
        + "Version identifier: " + Utilities.SYNTHEA_VERSION + " . " 
        + "  Person seed: " + person.seed 
        + "  Population seed: " + person.populationSeed;

    patientResource.setText(new NarrativeDt(
        new XhtmlDt(generatedBySynthea), NarrativeStatusEnum.GENERATED));

    // DALY and QALY values
    // we only write the last(current) one to the patient record
    Double dalyValue = (Double) person.attributes.get("most-recent-daly");
    Double qalyValue = (Double) person.attributes.get("most-recent-qaly");
    if (dalyValue != null) {
      ExtensionDt dalyExtension = new ExtensionDt();
      dalyExtension.setUrl(SYNTHEA_EXT + "disability-adjusted-life-years");
      DecimalDt daly = new DecimalDt(dalyValue);
      dalyExtension.setValue(daly);
      patientResource.addUndeclaredExtension(dalyExtension);

      ExtensionDt qalyExtension = new ExtensionDt();
      qalyExtension.setUrl(SYNTHEA_EXT + "quality-adjusted-life-years");
      DecimalDt qaly = new DecimalDt(qalyValue);
      qalyExtension.setValue(qaly);
      patientResource.addUndeclaredExtension(qalyExtension);
    }

    return newEntry(bundle, patientResource);
  }

  /**
   * Map the given Encounter into a FHIR Encounter resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          Entry for the Person
   * @param bundle
   *          The Bundle to add to
   * @param encounter
   *          The current Encounter
   * @return The added Entry
   */
  private static Entry encounter(Entry personEntry, Bundle bundle, Encounter encounter) {
    ca.uhn.fhir.model.dstu2.resource.Encounter encounterResource =
        new ca.uhn.fhir.model.dstu2.resource.Encounter();

    encounterResource.setStatus(EncounterStateEnum.FINISHED);
    if (encounter.codes.isEmpty()) {
      // wellness encounter
      encounterResource.addType().addCoding().setCode("185349003")
          .setDisplay("Encounter for check up").setSystem(SNOMED_URI);
    } else {
      Code code = encounter.codes.get(0);
      encounterResource.addType(mapCodeToCodeableConcept(code, SNOMED_URI));
    }

    encounterResource.setClassElement(EncounterClassEnum.forCode(encounter.type));
    long encounterEnd = encounter.stop > 0 ? encounter.stop
        : encounter.start + TimeUnit.MINUTES.toMillis(15);

    encounterResource.setPeriod(new PeriodDt().setStart(new DateTimeDt(new Date(encounter.start)))
        .setEnd(new DateTimeDt(new Date(encounterEnd))));

    if (encounter.reason != null) {
      encounterResource.addReason().addCoding().setCode(encounter.reason.code)
          .setDisplay(encounter.reason.display).setSystem(SNOMED_URI);
    }

    if (encounter.provider != null) {
      String providerFullUrl = null;

      for (Entry entry : bundle.getEntry()) {
        if ((entry.getResource().getResourceName().equals("Organization"))
            && (entry.getResource().getId().equals(encounter.provider.getResourceID()))) {
          providerFullUrl = entry.getFullUrl();
          break;
        }
      }

      if (providerFullUrl != null) {
        encounterResource.setServiceProvider(new ResourceReferenceDt(providerFullUrl));
      } else {
        Entry providerOrganization = provider(bundle, encounter.provider);
        encounterResource
            .setServiceProvider(new ResourceReferenceDt(providerOrganization.getFullUrl()));
      }
    } else { // no associated provider, patient goes to ambulatory provider
      Patient patient = (Patient) personEntry.getResource();
      List<ResourceReferenceDt> careProvider = patient.getCareProvider();

      if (careProvider.size() > 0) {
        IdDt generalPractitionerReference = patient.getCareProvider().get(0).getReference();

        for (Entry entry : bundle.getEntry()) {
          if ((entry.getResource().getResourceName().equals("Organization"))
              && generalPractitionerReference.equals("urn:uuid:" + entry.getResource().getId())) {
            encounterResource
                .setServiceProvider(new ResourceReferenceDt(generalPractitionerReference));
          }
        }
      }
    }

    if (encounter.discharge != null) {
      Hospitalization hospitalization = new Hospitalization();
      Code dischargeDisposition = new Code(DISCHARGE_URI, encounter.discharge.code,
          encounter.discharge.display);
      hospitalization
          .setDischargeDisposition(mapCodeToCodeableConcept(dischargeDisposition, DISCHARGE_URI));
      encounterResource.setHospitalization(hospitalization);
    }

    return newEntry(bundle, encounterResource);
  }

  /**
   * Create an entry for the given Claim, which references a Medication.
   * 
   * @param personEntry
   *          Entry for the person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter
   * @param claim
   *          the Claim object
   * @param medicationEntry
   *          The Entry for the Medication object, previously created
   * @return the added Entry
   */
  private static Entry medicationClaim(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Claim claim, Entry medicationEntry) {
    ca.uhn.fhir.model.dstu2.resource.Claim claimResource =
        new ca.uhn.fhir.model.dstu2.resource.Claim();
    ca.uhn.fhir.model.dstu2.resource.Encounter encounterResource =
        (ca.uhn.fhir.model.dstu2.resource.Encounter) encounterEntry
        .getResource();

    // assume institutional claim
    claimResource.setType(ClaimTypeEnum.INSTITUTIONAL); // TODO review claim type
    
    claimResource.setUse(UseEnum.COMPLETE);

    claimResource.setPatient(new ResourceReferenceDt(personEntry.getFullUrl()));
    claimResource.setOrganization(encounterResource.getServiceProvider());

    // add prescription.
    claimResource.setPrescription(new ResourceReferenceDt(medicationEntry.getFullUrl()));

    return newEntry(bundle, claimResource);
  }

  /**
   * Create an entry for the given Claim, associated to an Encounter.
   * 
   * @param personEntry
   *          Entry for the person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter
   * @param claim
   *          the Claim object
   * @return the added Entry
   */
  private static Entry encounterClaim(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Claim claim) {
    ca.uhn.fhir.model.dstu2.resource.Claim claimResource =
        new ca.uhn.fhir.model.dstu2.resource.Claim();
    ca.uhn.fhir.model.dstu2.resource.Encounter encounterResource =
        (ca.uhn.fhir.model.dstu2.resource.Encounter) encounterEntry
        .getResource();

    // assume institutional claim
    claimResource.setType(ClaimTypeEnum.INSTITUTIONAL); // TODO review claim type

    claimResource.setUse(UseEnum.COMPLETE);

    claimResource.setPatient(new ResourceReferenceDt(personEntry.getFullUrl()));
    claimResource.setOrganization(encounterResource.getServiceProvider());

    int itemSequence = 2;
    int conditionSequence = 1;
    for (ClaimItem item : claim.items) {
      if (item.entry instanceof Procedure) {
        // update claimItems list
        ca.uhn.fhir.model.dstu2.resource.Claim.Item procedureItem =
            new ca.uhn.fhir.model.dstu2.resource.Claim.Item();
        procedureItem.setSequence(new PositiveIntDt(itemSequence));

        // calculate cost of procedure based on rvu values for a facility
        MoneyDt moneyResource = new MoneyDt();
        moneyResource.setCode("USD");
        moneyResource.setSystem("urn:iso:std:iso:4217");
        moneyResource.setValue(item.cost());
        procedureItem.setNet(moneyResource);
        
        // assume item type is clinical service invoice
        CodingDt itemType = new CodingDt();
        itemType.setSystem("http://hl7.org/fhir/v3/ActCode")
            .setCode("CSINV")
            .setDisplay("clinical service invoice");
        procedureItem.setType(itemType); // TODO review claim item type
        
        // assume item service is expense
        CodingDt itemService = new CodingDt();
        itemService.setSystem("http://hl7.org/fhir/ex-USCLS")
            .setCode("99555")
            .setDisplay("Expense");
        procedureItem.setService(itemService); // TODO review claim item service
        
        claimResource.addItem(procedureItem);

      } else {
        // assume it's a Condition, we don't have a Condition class specifically
        // add diagnosisComponent to claim
        ca.uhn.fhir.model.dstu2.resource.Claim.Diagnosis diagnosisComponent =
            new ca.uhn.fhir.model.dstu2.resource.Claim.Diagnosis();
        diagnosisComponent.setSequence(new PositiveIntDt(conditionSequence));
        if (item.entry.codes.size() > 0) {
          // use first code
          diagnosisComponent.setDiagnosis(
              new CodingDt(item.entry.codes.get(0).system, item.entry.codes.get(0).code));
        }
        claimResource.addDiagnosis(diagnosisComponent);

        // update claimItems with diagnosis
        ca.uhn.fhir.model.dstu2.resource.Claim.Item diagnosisItem =
            new ca.uhn.fhir.model.dstu2.resource.Claim.Item();
        diagnosisItem.setSequence(new PositiveIntDt(itemSequence));
        diagnosisItem.addDiagnosisLinkId(conditionSequence);
        
        // assume item type is clinical service invoice
        CodingDt itemType = new CodingDt();
        itemType.setSystem("http://hl7.org/fhir/v3/ActCode")
            .setCode("CSINV")
            .setDisplay("clinical service invoice");
        diagnosisItem.setType(itemType);
        
        // assume item service is expense
        CodingDt itemService = new CodingDt();
        itemService.setSystem("http://hl7.org/fhir/ex-USCLS")
            .setCode("99555")
            .setDisplay("Expense");
        diagnosisItem.setService(itemService);
        
        claimResource.addItem(diagnosisItem);

        conditionSequence++;
      }
      itemSequence++;
    }
    
    return newEntry(bundle, claimResource);
  }

  /**
   * Map the Condition into a FHIR Condition resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param condition
   *          The Condition
   * @return The added Entry
   */
  private static Entry condition(Entry personEntry, Bundle bundle, Entry encounterEntry,
      HealthRecord.Entry condition) {
    Condition conditionResource = new Condition();

    conditionResource.setPatient(new ResourceReferenceDt(personEntry.getFullUrl()));
    conditionResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));

    Code code = condition.codes.get(0);
    conditionResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    conditionResource.setVerificationStatus(ConditionVerificationStatusEnum.CONFIRMED);
    conditionResource.setClinicalStatus(ConditionClinicalStatusCodesEnum.ACTIVE);

    conditionResource.setOnset(convertFhirDateTime(condition.start, true));
    conditionResource.setDateRecorded(new DateDt(new Date(condition.start)));

    if (condition.stop > 0) {
      conditionResource.setAbatement(convertFhirDateTime(condition.stop, true));
      conditionResource.setClinicalStatus(ConditionClinicalStatusCodesEnum.RESOLVED);
    }

    Entry conditionEntry = newEntry(bundle, conditionResource);

    condition.fullUrl = conditionEntry.getFullUrl();

    return conditionEntry;
  }

  /**
   * Map the given Observation into a FHIR Observation resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Person Entry
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param observation
   *          The Observation
   * @return The added Entry
   */
  private static Entry observation(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Observation observation) {
    ca.uhn.fhir.model.dstu2.resource.Observation observationResource =
        new ca.uhn.fhir.model.dstu2.resource.Observation();

    observationResource.setSubject(new ResourceReferenceDt(personEntry.getFullUrl()));
    observationResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));

    observationResource.setStatus(ObservationStatusEnum.FINAL);

    Code code = observation.codes.get(0);
    observationResource.setCode(mapCodeToCodeableConcept(code, LOINC_URI));

    Code category = new Code("http://hl7.org/fhir/observation-category", observation.category,
        observation.category);
    observationResource.setCategory(
        mapCodeToCodeableConcept(category, "http://hl7.org/fhir/observation-category"));

    IDatatype value = null;
    if (observation.value instanceof Condition) {
      Code conditionCode = ((HealthRecord.Entry) observation.value).codes.get(0);
      value = mapCodeToCodeableConcept(conditionCode, SNOMED_URI);
    } else if (observation.value instanceof Code) {
      value = mapCodeToCodeableConcept((Code) observation.value, SNOMED_URI);
    } else if (observation.value instanceof String) {
      value = new StringDt((String) observation.value);
    } else if (observation.value instanceof Number) {
      value = new QuantityDt().setValue(((Number) observation.value).doubleValue())
          .setCode(observation.unit).setSystem("http://unitsofmeasure.org/")
          .setUnit(observation.unit);
    } else if (observation.value != null) {
      throw new IllegalArgumentException("unexpected observation value class: "
          + observation.value.getClass().toString() + "; " + observation.value);
    }

    if (value != null) {
      observationResource.setValue(value);
    }

    observationResource.setEffective(convertFhirDateTime(observation.start, true));
    observationResource.setIssued(new InstantDt(new Date(observation.start)));

    Entry entry = newEntry(bundle, observationResource);
    observation.fullUrl = entry.getFullUrl();
    return entry;
  }

  /**
   * Map the given Procedure into a FHIR Procedure resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Person entry
   * @param bundle
   *          Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param procedure
   *          The Procedure
   * @return The added Entry
   */
  private static Entry procedure(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Procedure procedure) {
    ca.uhn.fhir.model.dstu2.resource.Procedure procedureResource =
        new ca.uhn.fhir.model.dstu2.resource.Procedure();

    procedureResource.setStatus(ProcedureStatusEnum.COMPLETED);
    procedureResource.setSubject(new ResourceReferenceDt(personEntry.getFullUrl()));
    procedureResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));

    Code code = procedure.codes.get(0);
    procedureResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    if (procedure.stop > 0L) {
      Date startDate = new Date(procedure.start);
      Date endDate = new Date(procedure.stop);
      procedureResource.setPerformed(
          new PeriodDt().setStart(new DateTimeDt(startDate)).setEnd(new DateTimeDt(endDate)));
    } else {
      procedureResource.setPerformed(convertFhirDateTime(procedure.start, true));
    }

    if (!procedure.reasons.isEmpty()) {
      Code reason = procedure.reasons.get(0); // Only one element in list
      for (Entry entry : bundle.getEntry()) {
        if (entry.getResource().getResourceName().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          CodingDt coding = condition.getCode().getCoding().get(0); // Only one element in list
          if (reason.code.equals(coding.getCode())) {
            procedureResource.setReason(new ResourceReferenceDt(entry.getFullUrl()));
          }
        }
      }
    }

    Entry procedureEntry = newEntry(bundle, procedureResource);

    procedure.fullUrl = procedureEntry.getFullUrl();

    return procedureEntry;
  }

  private static Entry immunization(Entry personEntry, Bundle bundle, Entry encounterEntry,
      HealthRecord.Entry immunization) {
    Immunization immResource = new Immunization();
    immResource.setStatus("completed");
    immResource.setDate(new DateTimeDt(new Date(immunization.start)));
    immResource.setVaccineCode(mapCodeToCodeableConcept(immunization.codes.get(0), CVX_URI));
    immResource.setReported(new BooleanDt(false));
    immResource.setWasNotGiven(false);
    immResource.setPatient(new ResourceReferenceDt(personEntry.getFullUrl()));
    immResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));
    return newEntry(bundle, immResource);
  }

  /**
   * Map the given Medication to a FHIR MedicationRequest resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the Medication to
   * @param encounterEntry
   *          Current Encounter entry
   * @param medication
   *          The Medication
   * @return The added Entry
   */
  private static Entry medication(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Medication medication) {
    MedicationOrder medicationResource = new MedicationOrder();

    medicationResource.setPatient(new ResourceReferenceDt(personEntry.getFullUrl()));
    medicationResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));

    medicationResource.setMedication(mapCodeToCodeableConcept(medication.codes.get(0), RXNORM_URI));

    medicationResource.setDateWritten(new DateTimeDt(new Date(medication.start)));

    if (medication.stop > 0L) {
      medicationResource.setStatus(MedicationOrderStatusEnum.STOPPED);
    } else {
      medicationResource.setStatus(MedicationOrderStatusEnum.ACTIVE);
    }

    if (!medication.reasons.isEmpty()) {
      // Only one element in list
      Code reason = medication.reasons.get(0);
      for (Entry entry : bundle.getEntry()) {
        if (entry.getResource().getResourceName().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          // Only one element in list
          CodingDt coding = condition.getCode().getCoding().get(0);
          if (reason.code.equals(coding.getCode())) {
            medicationResource.setReason(new ResourceReferenceDt(entry.getFullUrl()));
          }
        }
      }
    }

    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      DosageInstruction dosage = new DosageInstruction();

      // as_needed is true if present
      dosage.setAsNeeded(new BooleanDt(rxInfo.has("as_needed")));

      // as_needed is true if present
      if ((rxInfo.has("dosage")) && (!rxInfo.has("as_needed"))) {
        TimingDt timing = new TimingDt();
        Repeat timingRepeatComponent = new Repeat();
        timingRepeatComponent
            .setFrequency(rxInfo.get("dosage").getAsJsonObject().get("frequency").getAsInt());
        timingRepeatComponent
            .setPeriod(rxInfo.get("dosage").getAsJsonObject().get("period").getAsDouble());
        timingRepeatComponent.setPeriodUnits(
            convertUcumCode(rxInfo.get("dosage").getAsJsonObject().get("unit").getAsString()));
        timing.setRepeat(timingRepeatComponent);
        dosage.setTiming(timing);

        QuantityDt dose = new SimpleQuantityDt()
            .setValue(rxInfo.get("dosage").getAsJsonObject().get("amount").getAsDouble());
        dosage.setDose(dose);

        if (rxInfo.has("instructions")) {
          for (JsonElement instructionElement : rxInfo.get("instructions").getAsJsonArray()) {
            JsonObject instruction = instructionElement.getAsJsonObject();
            Code instructionCode = new Code(SNOMED_URI, instruction.get("code").getAsString(),
                instruction.get("display").getAsString());

            dosage.setAdditionalInstructions(mapCodeToCodeableConcept(instructionCode, SNOMED_URI));
          }
        }
      }

      List<DosageInstruction> dosageInstruction = new ArrayList<DosageInstruction>();
      dosageInstruction.add(dosage);
      medicationResource.setDosageInstruction(dosageInstruction);
    }

    Entry medicationEntry = newEntry(bundle, medicationResource);
    // create new claim for medication
    medicationClaim(personEntry, bundle, encounterEntry, medication.claim, medicationEntry);

    return medicationEntry;
  }

  /**
   * Map the given Report to a FHIR DiagnosticReport resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the Report to
   * @param encounterEntry
   *          Current Encounter entry
   * @param report
   *          The Report
   * @return The added Entry
   */
  private static Entry report(Entry personEntry, Bundle bundle, Entry encounterEntry,
      Report report) {
    DiagnosticReport reportResource = new DiagnosticReport();
    reportResource.setStatus(DiagnosticReportStatusEnum.FINAL);
    reportResource.setCode(mapCodeToCodeableConcept(report.codes.get(0), LOINC_URI));
    reportResource.setSubject(new ResourceReferenceDt(personEntry.getFullUrl()));
    reportResource.setEncounter(new ResourceReferenceDt(encounterEntry.getFullUrl()));
    reportResource.setEffective(convertFhirDateTime(report.start, true));
    reportResource.setIssued(new InstantDt(new Date(report.start)));
    
    ca.uhn.fhir.model.dstu2.resource.Encounter encounter =
        (ca.uhn.fhir.model.dstu2.resource.Encounter) encounterEntry.getResource();
    reportResource.setPerformer(encounter.getServiceProvider());
    
    for (Observation observation : report.observations) {
      ResourceReferenceDt reference = new ResourceReferenceDt(observation.fullUrl);
      reference.setDisplay(observation.codes.get(0).display);
      List<ResourceReferenceDt> result = new ArrayList<ResourceReferenceDt>();
      result.add(reference);
      reportResource.setResult(result);
    }

    return newEntry(bundle, reportResource);
  }

  /**
   * Map the given CarePlan to a FHIR CarePlan resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the CarePlan to
   * @param encounterEntry
   *          Current Encounter entry
   * @param carePlan
   *          The CarePlan to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static Entry careplan(Entry personEntry, Bundle bundle, Entry encounterEntry,
      CarePlan carePlan) {
    ca.uhn.fhir.model.dstu2.resource.CarePlan careplanResource =
        new ca.uhn.fhir.model.dstu2.resource.CarePlan();
    careplanResource.setSubject(new ResourceReferenceDt(personEntry.getFullUrl()));
    careplanResource.setContext(new ResourceReferenceDt(encounterEntry.getFullUrl()));

    Code code = carePlan.codes.get(0);
    careplanResource.addCategory(mapCodeToCodeableConcept(code, SNOMED_URI));

    CarePlanActivityStatusEnum activityStatus;
    GoalStatusEnum goalStatus;

    PeriodDt period = new PeriodDt().setStart(new DateTimeDt(new Date(carePlan.start)));
    careplanResource.setPeriod(period);
    if (carePlan.stop > 0L) {
      period.setEnd(new DateTimeDt(new Date(carePlan.stop)));
      careplanResource.setStatus(CarePlanStatusEnum.COMPLETED);
      activityStatus = CarePlanActivityStatusEnum.COMPLETED;
      goalStatus = GoalStatusEnum.ACHIEVED;
    } else {
      careplanResource.setStatus(CarePlanStatusEnum.ACTIVE);
      activityStatus = CarePlanActivityStatusEnum.IN_PROGRESS;
      goalStatus = GoalStatusEnum.IN_PROGRESS;
    }

    if (!carePlan.activities.isEmpty()) {
      for (Code activity : carePlan.activities) {
        Activity activityComponent = new Activity();
        ActivityDetail activityDetailComponent = new ActivityDetail();

        activityDetailComponent.setStatus(activityStatus);

        activityDetailComponent.setCode(mapCodeToCodeableConcept(activity, SNOMED_URI));
        activityDetailComponent.setProhibited(new BooleanDt(false));
        activityComponent.setDetail(activityDetailComponent);

        careplanResource.addActivity(activityComponent);
      }
    }

    if (!carePlan.reasons.isEmpty()) {
      // Only one element in list
      Code reason = carePlan.reasons.get(0);
      for (Entry entry : bundle.getEntry()) {
        if (entry.getResource().getResourceName().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          // Only one element in list
          CodingDt coding = condition.getCode().getCoding().get(0);
          if (reason.code.equals(coding.getCode())) {
            careplanResource.addAddresses().setReference(entry.getFullUrl());
          }
        }
      }
    }

    for (JsonObject goal : carePlan.goals) {
      Entry goalEntry = caregoal(bundle, goalStatus, goal);
      careplanResource.addGoal().setReference(goalEntry.getFullUrl());
    }

    return newEntry(bundle, careplanResource);
  }

  /**
   * Map the Provider into a FHIR Organization resource, and add it to the given Bundle.
   * 
   * @param bundle
   *          The Bundle to add to
   * @param provider
   *          The Provider
   * @return The added Entry
   */
  private static Entry provider(Bundle bundle, Provider provider) {
    ca.uhn.fhir.model.dstu2.resource.Organization organizationResource =
        new ca.uhn.fhir.model.dstu2.resource.Organization();

    CodeableConceptDt organizationType = mapCodeToCodeableConcept(
        new Code("http://hl7.org/fhir/ValueSet/organization-type", "prov", "Healthcare Provider"),
        "Healthcare Provider");

    organizationResource.setId(provider.getResourceID());
    organizationResource.setName((String) provider.getAttributes().get("name"));
    organizationResource.setType(organizationType);

    return newEntry(bundle, organizationResource);
  }

  /*
   * Map the JsonObject into a FHIR Goal resource, and add it to the given Bundle.
   * 
   * @param bundle The Bundle to add to
   * 
   * @param goalStatus The GoalStatus
   * 
   * @param goal The JsonObject
   * 
   * @return The added Entry
   */
  private static Entry caregoal(Bundle bundle, GoalStatusEnum goalStatus, JsonObject goal) {
    String resourceID = UUID.randomUUID().toString();

    ca.uhn.fhir.model.dstu2.resource.Goal goalResource =
        new ca.uhn.fhir.model.dstu2.resource.Goal();
    goalResource.setStatus(goalStatus);
    goalResource.setId(resourceID);

    if (goal.has("text")) {
      goalResource.setDescription(goal.get("text").getAsString());
    } else if (goal.has("codes")) {
      JsonObject code = goal.get("codes").getAsJsonArray().get(0).getAsJsonObject();

      goalResource.setDescription(code.get("display").getAsString());
    } else if (goal.has("observation")) {
      // build up our own text from the observation condition, similar to the graphviz logic
      JsonObject logic = goal.get("observation").getAsJsonObject();

      String[] text = {
          logic.get("codes").getAsJsonArray().get(0).getAsJsonObject().get("display").getAsString(),
          logic.get("operator").getAsString(), logic.get("value").getAsString() };

      goalResource.setDescription(String.join(" ", text));
    }

    if (goal.has("addresses")) {
      for (JsonElement reasonElement : goal.get("addresses").getAsJsonArray()) {
        if (reasonElement instanceof JsonObject) {
          JsonObject reasonObject = reasonElement.getAsJsonObject();
          String reasonCode = reasonObject.get("codes").getAsJsonObject().get("SNOMED-CT")
              .getAsJsonArray().get(0).getAsString();

          for (Entry entry : bundle.getEntry()) {
            if (entry.getResource().getResourceName().equals("Condition")) {
              Condition condition = (Condition) entry.getResource();
              // Only one element in list
              CodingDt coding = condition.getCode().getCoding().get(0);
              if (reasonCode.equals(coding.getCode())) {
                goalResource.addAddresses().setReference(entry.getFullUrl());
              }
            }
          }
        }
      }
    }

    return newEntry(bundle, goalResource);
  }

  /**
   * Convert the unit into a UnitsOfTime.
   *
   * @param unit
   *          unit String
   * @return a UnitsOfTime representing the given unit
   */
  private static UnitsOfTimeEnum convertUcumCode(String unit) {
    // From: http://hl7.org/fhir/ValueSet/units-of-time
    switch (unit) {
      case "seconds":
        return UnitsOfTimeEnum.S;
      case "minutes":
        return UnitsOfTimeEnum.MIN;
      case "hours":
        return UnitsOfTimeEnum.H;
      case "days":
        return UnitsOfTimeEnum.D;
      case "weeks":
        return UnitsOfTimeEnum.WK;
      case "months":
        return UnitsOfTimeEnum.MO;
      case "years":
        return UnitsOfTimeEnum.A;
      default:
        return null;
    }
  }

  /**
   * Convert the timestamp into a FHIR DateType or DateTimeType.
   * 
   * @param datetime
   *          Timestamp
   * @param time
   *          If true, return a DateTimeDt; if false, return a DateDt.
   * @return a DateDt or DateTimeDt representing the given timestamp
   */
  private static IDatatype convertFhirDateTime(long datetime, boolean time) {
    Date date = new Date(datetime);

    if (time) {
      return new DateTimeDt(date);
    } else {
      return new DateDt(date);
    }
  }

  /**
   * Helper function to convert a Code into a CodeableConceptDt. Takes an optional system, which
   * replaces the Code.system in the resulting CodeableConceptDt if not null.
   * 
   * @param from
   *          The Code to create a CodeableConcept from.
   * @param system
   *          The system identifier, such as a URI. Optional; may be null.
   * @return The converted CodeableConcept
   */
  private static CodeableConceptDt mapCodeToCodeableConcept(Code from, String system) {
    CodeableConceptDt to = new CodeableConceptDt();

    if (from.display != null) {
      to.setText(from.display);
    }

    CodingDt coding = new CodingDt();
    coding.setCode(from.code);
    coding.setDisplay(from.display);
    if (system == null) {
      coding.setSystem(from.system);
    } else {
      coding.setSystem(system);
    }

    to.addCoding(coding);

    return to;
  }

  /**
   * Helper function to create an Entry for the given Resource within the given Bundle. Sets the
   * resourceID to a random UUID, sets the entry's fullURL to that resourceID, and adds the entry to
   * the bundle.
   * 
   * @param bundle
   *          The Bundle to add the Entry to
   * @param resource
   *          Resource the new Entry should contain
   * @return the created Entry
   */
  private static Entry newEntry(Bundle bundle, BaseResource resource) {
    Entry entry = bundle.addEntry();

    String resourceID = UUID.randomUUID().toString();
    resource.setId(resourceID);
    entry.setFullUrl("urn:uuid:" + resourceID);

    entry.setResource(resource);

    return entry;
  }
}

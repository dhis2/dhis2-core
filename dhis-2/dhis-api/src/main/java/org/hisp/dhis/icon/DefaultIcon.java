/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.icon;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import lombok.Getter;

/** Default icons are pre-installed immutable icons. */
@Getter
public class DefaultIcon implements Icon {
  private final String key;

  private final String description;

  private final String[] keywords;

  private final boolean custom;

  private Date createdAt;

  private Date lastUpdatedAt;

  public DefaultIcon(String key, String description, String[] keywords, boolean custom) {
    this.key = key;
    this.description = description;
    this.keywords = keywords;
    this.custom = custom;
    this.setAutoFields();
  }

  public void setAutoFields() {
    Date date = new Date();

    if (createdAt == null) {
      createdAt = date;
    }

    lastUpdatedAt = date;
  }

  public enum Icons {
    _2G("2g", "", new String[] {}, false),
    _3G("3g", "", new String[] {}, false),
    _4X4("4x4", "", new String[] {}, false),
    AGRICULTURE("agriculture", "", new String[] {"agriculture"}, false),
    AGRICULTURE_WORKER("agriculture_worker", "", new String[] {"agriculture", "worker"}, false),
    ALERT("alert", "", new String[] {}, false),
    ALERT_CIRCLE("alert_circle", "", new String[] {}, false),
    ALERT_TRIANGLE("alert_triangle", "", new String[] {}, false),
    AMBULANCE("ambulance", "", new String[] {"ambulance", "transport"}, false),
    AMBULATORY_CLINIC(
        "ambulatory_clinic", "", new String[] {"refer", "health", "ambulatory"}, false),
    ANCV("ancv", "ANC visit", new String[] {}, false),
    BABY_FEMALE_0203M(
        "baby_female_0203m", "", new String[] {"baby", "2-3 months", "male", "pediatric"}, false),
    BABY_FEMALE_0306M(
        "baby_female_0306m", "", new String[] {"3-6 months", "baby", "male", "pediatric"}, false),
    BABY_FEMALE_0609M(
        "baby_female_0609m", "", new String[] {"baby", "male", "6-9 months", "pediatric"}, false),
    BABY_MALE_0203M(
        "baby_male_0203m", "", new String[] {"baby", "2-3 months", "female", "pediatric"}, false),
    BABY_MALE_0306M(
        "baby_male_0306m", "", new String[] {"3-6 months", "baby", "female", "pediatric"}, false),
    BABY_MALE_0609M(
        "baby_male_0609m", "", new String[] {"baby", "female", "6-9 months", "pediatric"}, false),
    BASIC_MOTORCYCLE("basic_motorcycle", "", new String[] {"motorcycle", "transport"}, false),
    BIKE("bike", "", new String[] {"transport", "bike"}, false),
    BILLS(
        "bills", "Bills", new String[] {"money", "buck", "bank notes", "bills", "currency"}, false),
    BLISTER_PILLS_OVAL_X1(
        "blister_pills_oval_x1",
        "",
        new String[] {"treatment", "pills", "1", "blister", "oval"},
        false),
    BLISTER_PILLS_OVAL_X14(
        "blister_pills_oval_x14",
        "",
        new String[] {"treatment", "pills", "14", "blister", "oval"},
        false),
    BLISTER_PILLS_OVAL_X16(
        "blister_pills_oval_x16",
        "",
        new String[] {"treatment", "pills", "16", "blister", "oval"},
        false),
    BLISTER_PILLS_OVAL_X4(
        "blister_pills_oval_x4",
        "",
        new String[] {"treatment", "pills", "4", "blister", "oval"},
        false),
    BLISTER_PILLS_ROUND_X1(
        "blister_pills_round_x1",
        "",
        new String[] {"treatment", "pills", "1", "round", "blister"},
        false),
    BLISTER_PILLS_ROUND_X14(
        "blister_pills_round_x14",
        "",
        new String[] {"treatment", "pills", "14", "round", "blister"},
        false),
    BLISTER_PILLS_ROUND_X16(
        "blister_pills_round_x16",
        "",
        new String[] {"treatment", "pills", "round", "16", "blister"},
        false),
    BLISTER_PILLS_ROUND_X4(
        "blister_pills_round_x4",
        "",
        new String[] {"treatment", "pills", "round", "4", "blister"},
        false),
    BLOOD_A_N("blood_a_n", "", new String[] {"a", "hematology", "negative", "blood"}, false),
    BLOOD_A_P("blood_a_p", "", new String[] {"a", "hematology", "positive", "blood"}, false),
    BLOOD_AB_N("blood_ab_n", "", new String[] {"ab", "hematology", "negative", "blood"}, false),
    BLOOD_AB_P("blood_ab_p", "", new String[] {"ab", "hematology", "positive", "blood"}, false),
    BLOOD_B_N("blood_b_n", "", new String[] {"hematology", "b", "negative", "blood"}, false),
    BLOOD_B_P("blood_b_p", "", new String[] {"hematology", "b", "positive", "blood"}, false),
    BLOOD_O_N("blood_o_n", "", new String[] {"hematology", "negative", "blood", "o"}, false),
    BLOOD_O_P("blood_o_p", "", new String[] {"hematology", "positive", "blood", "o"}, false),
    BLOOD_PRESSURE("blood_pressure", "", new String[] {"Blood Pressure"}, false),
    BLOOD_PRESSURE_2("blood_pressure_2", "", new String[] {"pressure", "blood"}, false),
    BLOOD_PRESSURE_MONITOR("blood_pressure_monitor", "", new String[] {"pressure", "blood"}, false),
    BLOOD_RH_N("blood_rh_n", "", new String[] {"hematology", "negative", "rh", "blood"}, false),
    BLOOD_RH_P("blood_rh_p", "", new String[] {"hematology", "rh", "positive", "blood"}, false),
    BOY_0105Y("boy_0105y", "", new String[] {"1-5 Yrs", "boy"}, false),
    BOY_1015Y("boy_1015y", "", new String[] {"boy", "10-15 Yrs"}, false),
    BREEDING_SITES(
        "breeding_sites",
        "Breeding sites",
        new String[] {"pupa", "larva", "malaria", "mosquito", "reproduction"},
        false),
    CALENDAR("calendar", "", new String[] {"calendar"}, false),
    CARDIOGRAM("cardiogram", "Cardiogram card", new String[] {"medical history"}, false),
    CARDIOGRAM_E(
        "cardiogram_e",
        "Edit ardiogram card",
        new String[] {"Cardiogram card", "cardiogram"},
        false),
    CERVICAL_CANCER("cervical_cancer", "", new String[] {"cervical", "cancer", "female"}, false),
    CHILD_CARE(
        "child_care", "Children", new String[] {"toddler", "kid", "baby", "child", "care"}, false),
    CHILD_PROGRAM("child_program", "child program", new String[] {}, false),
    CHILLS("chills", "", new String[] {"symptom", "chills", "patient", "diagnosis"}, false),
    CHOLERA("cholera", "", new String[] {"vibrio cholerae", "cholera"}, false),
    CHURCH("church", "", new String[] {}, false),
    CIRCLE_LARGE("circle_large", "", new String[] {}, false),
    CIRCLE_MEDIUM("circle_medium", "", new String[] {}, false),
    CIRCLE_SMALL("circle_small", "", new String[] {}, false),
    CITY(
        "city",
        "City",
        new String[] {"city", "house", "building", "edifice", "architecture"},
        false),
    CITY_WORKER("city_worker", "", new String[] {"city worker", "worker"}, false),
    CLEAN_HANDS(
        "clean_hands",
        "Clean hands",
        new String[] {"washing", "hands", "sanitizer", "clean", "soap"},
        false),
    CLINICAL_A("clinical_a", "Clinical analysis", new String[] {}, false),
    CLINICAL_F(
        "clinical_f",
        "Clinical file",
        new String[] {"Clinical file", "medical history", "diagnosis test", "patient record"},
        false),
    CLINICAL_FE(
        "clinical_fe",
        "Edit clinical file",
        new String[] {"medical history", "diagnosis test"},
        false),
    COINS("coins", "Money", new String[] {"wealth", "money", "payment", "cash"}, false),
    COLD_CHAIN("cold_chain", "", new String[] {"cold chain", "cold"}, false),
    COMMUNICATION("communication", "", new String[] {"contact", "communication"}, false),
    CONE_TEST_ON_NETS(
        "cone_test_on_nets",
        "Cone test on nets",
        new String[] {"test", "nets", "malaria", "analysis", "cone"},
        false),
    CONE_TEST_ON_WALLS(
        "cone_test_on_walls",
        "Cone test on walls",
        new String[] {"test", "malaria", "analysis", "cone", "wall"},
        false),
    CONSTRUCTION(
        "construction", "", new String[] {"infrastructure", "construction", "worker"}, false),
    CONSTRUCTION_WORKER("construction_worker", "", new String[] {"construction", "worker"}, false),
    CONTACT_SUPPORT(
        "contact_support",
        "Contact support",
        new String[] {"question", "contact", "support", "info"},
        false),
    CONTRACEPTIVE_DIAPHRAGM(
        "contraceptive_diaphragm",
        "",
        new String[] {"female", "contraception", "sexual", "reproductive"},
        false),
    CONTRACEPTIVE_INJECTION(
        "contraceptive_injection",
        "",
        new String[] {"inyectable", "female", "contraception", "sexual", "reproductive"},
        false),
    CONTRACEPTIVE_PATCH(
        "contraceptive_patch",
        "",
        new String[] {"female", "contraception", "sexual", "reproductive"},
        false),
    CONTRACEPTIVE_VOUCHER("contraceptive_voucher", "contraceptive voucher", new String[] {}, false),
    COPPER_IUD(
        "copper_iud",
        "",
        new String[] {"copper iud", "copper", "contraception", "sexual", "reproductive"},
        false),
    COUGHING("coughing", "", new String[] {"symptom", "coughing", "diagnosis", "cough"}, false),
    CREDIT_CARD(
        "credit_card",
        "Credit card",
        new String[] {"bank", "money", "credit", "debit", "card"},
        false),
    CROSS_COUNTRY_MOTORCYCLE(
        "cross_country_motorcycle", "", new String[] {"motorcycle", "transport"}, false),
    DEFAULT("default", "", new String[] {"default"}, false),
    DHIS2_LOGO("dhis2_logo", "DHIS2", new String[] {"dhis2", "logo", "DHIS"}, false),
    DIARRHEA("diarrhea", "", new String[] {"symptom", "patient", "diagnosis", "diarrhea"}, false),
    DISCRIMINATING_CONCENTRATION_BIOASSAYS(
        "discriminating_concentration_bioassays",
        "Discriminating concentration bioassays",
        new String[] {"assay", "concentration", "analysis", "malaria", "mosquito"},
        false),
    DOCTOR(
        "doctor",
        "Doctor",
        new String[] {"doctor", "medical person", "nurse", "health", "worker"},
        false),
    DOMESTIC_WORKER("domestic_worker", "", new String[] {"domestic", "domestic worker"}, false),
    DONKEY("donkey", "", new String[] {"donkey", "transport"}, false),
    DRONE("drone", "", new String[] {}, false),
    ECO_CARE("eco_care", "Eco care", new String[] {"eco", "world", "nature", "bio", "care"}, false),
    ELDERLY("elderly", "Elderly", new String[] {"old", "person", "aged", "aging", "people"}, false),
    ELECTRICITY("electricity", "", new String[] {"electricity"}, false),
    EMERGENCY_POST("emergency_post", "", new String[] {"emergency", "emergency post"}, false),
    EXPECTORATE("expectorate", "", new String[] {"symptom", "diagnosis", "expectorate"}, false),
    FACTORY_WORKER("factory_worker", "", new String[] {"factory", "worker"}, false),
    FAMILY_PLANNING(
        "family_planning",
        "",
        new String[] {"family planning", "contraception", "sexual", "reproductive"},
        false),
    FEMALE_AND_MALE("female_and_male", "Female and Male adult", new String[] {}, false),
    FEMALE_CONDOM(
        "female_condom",
        "",
        new String[] {"female", "contraception", "sexual", "reproductive", "condom"},
        false),
    FEMALE_SEX_WORKER("female_sex_worker", "", new String[] {"female", "sex worker"}, false),
    FETUS("fetus", "", new String[] {"baby", "not born"}, false),
    FEVER("fever", "", new String[] {"symptom", "fever", "diagnosis"}, false),
    FEVER_2("fever_2", "", new String[] {"symptom", "fever", "diagnosis"}, false),
    FEVER_CHILLS(
        "fever_chills",
        "",
        new String[] {"symptom", "chills", "patient", "fever", "diagnosis"},
        false),
    FOREST("forest", "", new String[] {"forest"}, false),
    FOREST_PERSONS("forest_persons", "", new String[] {"persons", "forest"}, false),
    FORUM("forum", "Forum", new String[] {"forum", "chat", "discussion", "conversation"}, false),
    GIRL_0105Y("girl_0105y", "", new String[] {"1-5 Yrs", "girl"}, false),
    GIRL_1015Y("girl_1015y", "", new String[] {"girl", "10-15 Yrs"}, false),
    GROUP_DISCUSSION_MEETING(
        "group_discussion_meeting",
        "",
        new String[] {"contact", "communication", "meeting", "group"},
        false),
    GROUP_DISCUSSION_MEETINGX3(
        "group_discussion_meetingx3",
        "",
        new String[] {"contact", "communication", "meeting", "group"},
        false),
    HAPPY("happy", "", new String[] {"face"}, false),
    HAZARDOUS(
        "hazardous",
        "Hazardous",
        new String[] {"hazardous", "difficult", "unsafe", "risky"},
        false),
    HEADACHE("headache", "", new String[] {"symptom", "patient", "diagnosis", "headache"}, false),
    HEALTH_WORKER(
        "health_worker",
        "Health worker",
        new String[] {"doctor", "nurse", "health", "worker"},
        false),
    HEALTH_WORKER_FORM(
        "health_worker_form",
        "Health worker form",
        new String[] {"quiz", "questionnaire", "form", "health worker"},
        false),
    HEART("heart", "Heart", new String[] {"love", "core", "favorite", "heart"}, false),
    HEART_CARDIOGRAM(
        "heart_cardiogram",
        "Heart - cardiogram",
        new String[] {"electrocardiogram", "heart"},
        false),
    HELICOPTER("helicopter", "", new String[] {}, false),
    HIGH_BARS("high_bars", "High bars", new String[] {"big", "high", "great", "bars"}, false),
    HIGH_LEVEL("high_level", "High level", new String[] {"big", "high", "level", "great"}, false),
    HIV_IND("hiv_ind", "", new String[] {"HIV", "indeterminate", "STI"}, false),
    HIV_NEG("hiv_neg", "", new String[] {"negative", "HIV", "STI"}, false),
    HIV_POS("hiv_pos", "", new String[] {"HIV", "STI", "positive"}, false),
    HIV_SELF_TEST("hiv_self_test", "", new String[] {"HIV", "test", "STI"}, false),
    HOME("home", "Home", new String[] {"cabin", "place", "house", "apartment", "home"}, false),
    HORMONAL_RING(
        "hormonal_ring",
        "",
        new String[] {"sterilization", "contraception", "sexual", "reproductive", "hormonal ring"},
        false),
    HOSPITAL("hospital", "", new String[] {"refer", "hospital"}, false),
    HOSPITALIZED("hospitalized", "", new String[] {"hospitalized", "patient"}, false),
    HOT_MEAL("hot_meal", "", new String[] {"hot meal", "food"}, false),
    HPV(
        "hpv",
        "",
        new String[] {
          "Human papillomavirus", "papillomavirus", "STI", "diagnosis", "Sexual Transmitted"
        },
        false),
    I_CERTIFICATE_PAPER(
        "i_certificate_paper",
        "Certification",
        new String[] {"qualification", "education", "credential", "certificate", "diploma"},
        false),
    I_DOCUMENTS_ACCEPTED(
        "i_documents_accepted",
        "Documents accepted",
        new String[] {"documents", "accepted", "check", "done"},
        false),
    I_DOCUMENTS_DENIED(
        "i_documents_denied",
        "Documents denied",
        new String[] {"refuse", "bad", "documents", "denied"},
        false),
    I_EXAM_MULTIPLE_CHOICE(
        "i_exam_multiple_choice",
        "Test",
        new String[] {"exam", "quiz", "test", "multiple choice"},
        false),
    I_EXAM_QUALIFICATION(
        "i_exam_qualification",
        "Exam qualification",
        new String[] {"exam", "qualification", "notes", "test"},
        false),
    I_GROUPS_PERSPECTIVE_CROWD(
        "i_groups_perspective_crowd",
        "Crowd",
        new String[] {"persons", "crowd", "people", "group"},
        false),
    I_NOTE_ACTION(
        "i_note_action",
        "Homework",
        new String[] {"notes", "homework", "assignment", "document", "write"},
        false),
    I_SCHEDULE_SCHOOL_DATE_TIME(
        "i_schedule_school_date_time",
        "Schedule",
        new String[] {"date", "schedule", "school", "time"},
        false),
    I_TRAINING_CLASS(
        "i_training_class",
        "Training",
        new String[] {"teach", "training", "class", "board"},
        false),
    I_UTENSILS(
        "i_utensils",
        "School utensils",
        new String[] {"education", "school", "pen", "ruler", "utensils"},
        false),
    IMM("imm", "Inpatient morbidity and mortality", new String[] {}, false),
    IMPLANT(
        "implant",
        "",
        new String[] {"pellet", "implant", "contraception", "sexual", "reproductive"},
        false),
    INFO("info", "Info", new String[] {"advice", "instruction", "information", "info"}, false),
    INFORMATION_CAMPAIGN("information_campaign", "information campaign", new String[] {}, false),
    INPATIENT("inpatient", "", new String[] {"patient", "inpatient"}, false),
    INSECTICIDE_RESISTANCE(
        "insecticide_resistance",
        "Insecticide resistance",
        new String[] {"insecticide", "malaria", "resistance", "mosquito"},
        false),
    INTENSITY_CONCENTRATION_BIOASSAYS(
        "intensity_concentration_bioassays",
        "Intensity concentration bioassays",
        new String[] {"assay", "concentration", "analysis", "malaria", "mosquito"},
        false),
    IUD("iud", "", new String[] {"iud", "contraception", "sexual", "reproductive"}, false),
    JUSTICE("justice", "", new String[] {"justice"}, false),
    LACTATION("lactation", "", new String[] {"ANC", "baby", "child", "pediatric"}, false),
    LETRINA("letrina", "", new String[] {"letrina"}, false),
    LLIN("llin", "", new String[] {"llin", "malaria", "net"}, false),
    LOW_BARS("low_bars", "Low bars", new String[] {"small", "low", "poor", "bars"}, false),
    LOW_LEVEL("low_level", "Low level", new String[] {"small", "low", "level", "poor"}, false),
    MACHINERY(
        "machinery",
        "Machinery",
        new String[] {"excavator", "engine", "machinery", "vehicle"},
        false),
    MAGNIFYING_GLASS("magnifying_glass", "", new String[] {"magnifying glass"}, false),
    MALARIA_MIXED_MICROSCOPE("malaria_mixed_microscope", "", new String[] {}, false),
    MALARIA_NEGATIVE_MICROSCOPE("malaria_negative_microscope", "", new String[] {}, false),
    MALARIA_OUTBREAK(
        "malaria_outbreak",
        "malaria outbreak",
        new String[] {"midge", "outbreak", "denge", "malaria"},
        false),
    MALARIA_PF_MICROSCOPE("malaria_pf_microscope", "", new String[] {}, false),
    MALARIA_PV_MICROSCOPE("malaria_pv_microscope", "", new String[] {}, false),
    MALARIA_TESTING("malaria_testing", "malaria testing", new String[] {}, false),
    MALE_AND_FEMALE(
        "male_and_female", "Male and Female icon", new String[] {"man and female"}, false),
    MALE_CONDOM(
        "male_condom",
        "",
        new String[] {"copper IUD", "contraception", "sexual", "reproductive"},
        false),
    MALE_SEX_WORKER("male_sex_worker", "", new String[] {"sex worker", "male"}, false),
    MAN("man", "", new String[] {"man", "boy", "male"}, false),
    MARKET_STALL("market_stall", "", new String[] {"market"}, false),
    MASK("mask", "Face mask", new String[] {"face mask", "ffp", "virus", "mask"}, false),
    MEASLES("measles", "", new String[] {"symptom", "patient", "diagnosis", "measles"}, false),
    MEDICINES("medicines", "", new String[] {"treatment", "medicines"}, false),
    MEDIUM_BARS(
        "medium_bars",
        "Medium bars",
        new String[] {"average", "normal", "common", "medium", "bars"},
        false),
    MEDIUM_LEVEL(
        "medium_level",
        "Medium level",
        new String[] {"average", "normal", "common", "level", "medium"},
        false),
    MEGAPHONE("megaphone", "", new String[] {"megaphone", "communication"}, false),
    MENTAL_DISORDERS(
        "mental_disorders",
        "mental_disorders",
        new String[] {"mental health", "mental disorder"},
        false),
    MICROSCOPE(
        "microscope",
        "Optical Microscope",
        new String[] {"laboratory", "lab", "analysis", "microscope"},
        false),
    MILITARY_WORKER("military_worker", "", new String[] {"army", "military", "worker"}, false),
    MINER_WORKER("miner_worker", "", new String[] {"miner worker", "worker", "miner"}, false),
    MOBILE_CLINIC(
        "mobile_clinic",
        "",
        new String[] {"refer", "health", "mobile clinic", "ambulance", "transport"},
        false),
    MONEY_BAG(
        "money_bag",
        "Money bag",
        new String[] {"money", "bag", "currency", "cash", "dollar"},
        false),
    MOSQUITO("mosquito", "", new String[] {"denge", "malaria", "mosquito"}, false),
    MOSQUITO_COLLECTION(
        "mosquito_collection",
        "Mosquito collection",
        new String[] {"collection", "gather", "malaria", "case", "mosquito"},
        false),
    MSM("msm", "", new String[] {"msm"}, false),
    NAUSEA("nausea", "", new String[] {"symptom", "patient", "diagnosis", "nausea"}, false),
    NEGATIVE("negative", "", new String[] {"negative"}, false),
    NETWORK_4G("network_4g", "", new String[] {"4g", "transfer", "connectivity", "net"}, false),
    NETWORK_5G("network_5g", "", new String[] {"5g", "transfer", "connectivity", "net"}, false),
    NEUROLOGY("neurology", "", new String[] {"mental health", "meningitis"}, false),
    NEUTRAL("neutral", "", new String[] {"face"}, false),
    NO("no", "", new String[] {"no"}, false),
    NOT_OK("not_ok", "", new String[] {"face"}, false),
    NURSE(
        "nurse", "Nurse", new String[] {"doctor", "assistant", "nurse", "health", "medic"}, false),
    OBSERVATION("observation", "", new String[] {"observation", "patient"}, false),
    ODONTOLOGY("odontology", "", new String[] {"odontology"}, false),
    ODONTOLOGY_IMPLANT("odontology_implant", "", new String[] {"odontology", "implant"}, false),
    OFFICER("officer", "Officer", new String[] {"agent", "police", "cop", "officer"}, false),
    OK("ok", "", new String[] {"face"}, false),
    OLD_MAN("old_man", "Old man", new String[] {"old", "elderly", "aged", "man"}, false),
    OLD_WOMAN("old_woman", "Old woman", new String[] {"woman", "old", "elderly", "aged"}, false),
    ORAL_CONTRACEPTION_PILLSX21(
        "oral_contraception_pillsx21",
        "",
        new String[] {"treatment", "female", "contraception", "sexual", "reproductive"},
        false),
    ORAL_CONTRACEPTION_PILLSX28(
        "oral_contraception_pillsx28",
        "",
        new String[] {"treatment", "female", "contraception", "sexual", "reproductive"},
        false),
    OUTPATIENT("outpatient", "", new String[] {"patient", "outpatient"}, false),
    OVERWEIGHT(
        "overweight", "", new String[] {"symptom", "patient", "diagnosis", "overweight"}, false),
    PALM_BRANCHES_ROOF("palm_branches_roof", "", new String[] {"roof", "laton", "zinc"}, false),
    PAVE_ROAD("pave_road", "", new String[] {"pave", "road"}, false),
    PEACE("peace", "Peace", new String[] {"love", "truce", "peace", "accord"}, false),
    PEOPLE(
        "people",
        "People",
        new String[] {"crowd", "person", "community", "people", "group"},
        false),
    PERSON("person", "Person", new String[] {"woman", "person", "man", "people"}, false),
    PHONE("phone", "", new String[] {"phone", "contact", "communication"}, false),
    PILL_1("pill_1", "", new String[] {"pills 1", "One pill"}, false),
    PILLS_2("pills_2", "", new String[] {"pills", "two"}, false),
    PILLS_3("pills_3", "", new String[] {"pills", "three"}, false),
    PILLS_4("pills_4", "", new String[] {"pills", "four"}, false),
    PLANTATION_WORKER("plantation_worker", "", new String[] {"plantation", "worker"}, false),
    POLYGON("polygon", "", new String[] {"polygon"}, false),
    POSITIVE("positive", "", new String[] {"positive"}, false),
    PREGNANT("pregnant", "ANC", new String[] {"ANC", "maternity", "pregnant"}, false),
    PREGNANT_0812W(
        "pregnant_0812w",
        "ANC",
        new String[] {"First visit 8-12 weeks", "ANC", "maternity", "pregnant"},
        false),
    PREGNANT_2426W(
        "pregnant_2426w",
        "ANC",
        new String[] {"Second visit 24-26 weeks", "ANC", "maternity", "pregnant"},
        false),
    PREGNANT_32W(
        "pregnant_32w",
        "ANC",
        new String[] {"ANC", "maternity", "Third visit 32 weeks", "pregnant"},
        false),
    PREGNANT_3638W(
        "pregnant_3638w",
        "ANC",
        new String[] {"ANC", "maternity", "Fourth visit 36-38 weeks", "pregnant"},
        false),
    PRISONER(
        "prisoner",
        "Prisoner",
        new String[] {"detainee", "prisoner", "captive", "justice", "convict"},
        false),
    PROPER_ROOF("proper_roof", "", new String[] {"roof", "proper"}, false),
    PROVIDER_FST("provider_fst", "Provider Follow-up and Support Tool", new String[] {}, false),
    PWID("pwid", "", new String[] {"pwid"}, false),
    QUESTION("question", "", new String[] {}, false),
    QUESTION_CIRCLE("question_circle", "", new String[] {}, false),
    QUESTION_TRIANGLE("question_triangle", "", new String[] {}, false),
    RDT_RESULT_INVALID(
        "rdt_result_invalid", "", new String[] {"result", "rdt", "invalid", "diagnosis"}, false),
    RDT_RESULT_MIXED("rdt_result_mixed", "", new String[] {}, false),
    RDT_RESULT_MIXED_INVALID(
        "rdt_result_mixed_invalid", "", new String[] {"result", "rdt", "pv", "diagnosis"}, false),
    RDT_RESULT_MIXED_INVALID_RECTANGULAR(
        "rdt_result_mixed_invalid_rectangular",
        "",
        new String[] {"result", "rdt", "invalid", "diagnosis", "mixed"},
        false),
    RDT_RESULT_MIXED_RECTANGULAR(
        "rdt_result_mixed_rectangular",
        "",
        new String[] {"result", "rdt", "diagnosis", "mixed"},
        false),
    RDT_RESULT_NEG("rdt_result_neg", "", new String[] {}, false),
    RDT_RESULT_NEG_INVALID(
        "rdt_result_neg_invalid",
        "",
        new String[] {"result", "neg", "rdt", "invalid", "diagnosis"},
        false),
    RDT_RESULT_NEG_INVALID_RECTANGULAR(
        "rdt_result_neg_invalid_rectangular",
        "",
        new String[] {"result", "neg", "rdt", "invalid", "diagnosis"},
        false),
    RDT_RESULT_NEG_RECTANGULAR(
        "rdt_result_neg_rectangular",
        "",
        new String[] {"result", "neg", "rdt", "diagnosis"},
        false),
    RDT_RESULT_NEGATIVE(
        "rdt_result_negative", "", new String[] {"result", "negative", "rdt", "diagnosis"}, false),
    RDT_RESULT_NO_TEST("rdt_result_no_test", "", new String[] {}, false),
    RDT_RESULT_OUT_STOCK(
        "rdt_result_out_stock", "", new String[] {"result", "out sock", "rdt"}, false),
    RDT_RESULT_PF("rdt_result_pf", "", new String[] {}, false),
    RDT_RESULT_PF_INVALID(
        "rdt_result_pf_invalid",
        "",
        new String[] {"result", "rdt", "pf", "invalid", "diagnosis"},
        false),
    RDT_RESULT_PF_INVALID_RECTANGULAR(
        "rdt_result_pf_invalid_rectangular",
        "",
        new String[] {"result", "rdt", "pf", "invalid", "diagnosis"},
        false),
    RDT_RESULT_PF_RECTANGULAR(
        "rdt_result_pf_rectangular", "", new String[] {"result", "rdt", "pf", "diagnosis"}, false),
    RDT_RESULT_POSITIVE(
        "rdt_result_positive", "", new String[] {"result", "rdt", "diagnosis", "positive"}, false),
    RDT_RESULT_PV("rdt_result_pv", "", new String[] {}, false),
    RDT_RESULT_PV_INVALID(
        "rdt_result_pv_invalid",
        "",
        new String[] {"result", "rdt", "pv", "invalid", "diagnosis"},
        false),
    RDT_RESULT_PV_INVALID_RECTANGULAR(
        "rdt_result_pv_invalid_rectangular",
        "",
        new String[] {"result", "rdt", "pv", "invalid", "diagnosis"},
        false),
    RDT_RESULT_PV_RECTANGULAR(
        "rdt_result_pv_rectangular", "", new String[] {"result", "rdt", "pv", "diagnosis"}, false),
    REFERRAL("referral", "", new String[] {"referral", "patient", "health"}, false),
    REFUSED("refused", "", new String[] {"treatment", "refused"}, false),
    RIBBON("ribbon", "", new String[] {"ribbon", "STI"}, false),
    RMNH("rmnh", "WHO RMNCH Tracker", new String[] {}, false),
    RUNNING_WATER("running_water", "", new String[] {"running water", "water"}, false),
    RURAL_POST("rural_post", "", new String[] {"refer", "rural", "health", "clinic"}, false),
    SAD("sad", "", new String[] {"face"}, false),
    SANITIZER(
        "sanitizer", "Sanitizer", new String[] {"hands", "sanitizer", "cleaner", "soap"}, false),
    SAYANA_PRESS(
        "sayana_press",
        "",
        new String[] {"inyectable", "female", "contraception", "sexual", "reproductive"},
        false),
    SECURITY_WORKER("security_worker", "", new String[] {"worker", "cecurity"}, false),
    SEXUAL_REPRODUCTIVE_HEALTH(
        "sexual_reproductive_health",
        "",
        new String[] {"contraception", "sexual", "reproductive"},
        false),
    SMALL_PLANE("small_plane", "", new String[] {}, false),
    SOCIAL_DISTANCING(
        "social_distancing",
        "Social distancing",
        new String[] {"distance", "social", "separation", "distancing"},
        false),
    SPRAYING("spraying", "", new String[] {"spraying"}, false),
    SQUARE_LARGE("square_large", "", new String[] {}, false),
    SQUARE_MEDIUM("square_medium", "", new String[] {}, false),
    SQUARE_SMALL("square_small", "", new String[] {}, false),
    STAR_LARGE("star_large", "", new String[] {}, false),
    STAR_MEDIUM("star_medium", "", new String[] {}, false),
    STAR_SMALL("star_small", "", new String[] {}, false),
    STETHOSCOPE("stethoscope", "", new String[] {"stethoscope"}, false),
    STI(
        "sti",
        "",
        new String[] {"symptom", "Trichomoniasis", "patient", "STI", "diagnosis"},
        false),
    STOCK_OUT("stock_out", "", new String[] {"stock", "status"}, false),
    STOP("stop", "", new String[] {"stop"}, false),
    SURGICAL_STERILIZATION(
        "surgical_sterilization",
        "",
        new String[] {"circusicion", "sterilization", "contraception", "sexual", "reproductive"},
        false),
    SWEATING("sweating", "", new String[] {"symptom", "patient", "diagnosis", "sweating"}, false),
    SYMPTOM("symptom", "", new String[] {"symptoms"}, false),
    SYNERGIST_INSECTICIDE_BIOASSAYS(
        "synergist_insecticide_bioassays",
        "Synergist insecticide bioassays",
        new String[] {"chemical", "assay", "analysis", "malaria", "mosquito"},
        false),
    SYRINGE("syringe", "", new String[] {"syringe", "injectable", "vaccination"}, false),
    TAC("tac", "Computed axial tomography", new String[] {}, false),
    TB("tb", "TB program", new String[] {}, false),
    TRANSGENDER("transgender", "", new String[] {"transgender"}, false),
    TRAUMATISM("traumatism", "", new String[] {"patient", "traumatism"}, false),
    TRAVEL("travel", "", new String[] {"travel"}, false),
    TREATED_WATER("treated_water", "", new String[] {"treated", "infrastructure", "water"}, false),
    TRIANGLE_LARGE("triangle_large", "", new String[] {}, false),
    TRIANGLE_MEDIUM("triangle_medium", "", new String[] {}, false),
    TRIANGLE_SMALL("triangle_small", "", new String[] {}, false),
    TRUCK_DRIVER("truck_driver", "", new String[] {"driver", "truck driver", "worker"}, false),
    UN_PAVE_ROAD("un_pave_road", "", new String[] {"pave", "road"}, false),
    UNDERWEIGHT(
        "underweight", "", new String[] {"symptom", "patient", "diagnosis", "underweight"}, false),
    VESPA_MOTORCYCLE("vespa_motorcycle", "", new String[] {"motorcycle", "transport"}, false),
    VIH_AIDS(
        "vih_aids", "VIH/AIDS", new String[] {"hematology", "vih", "STI", "aids", "blood"}, false),
    VIRUS("virus", "Coronavirus", new String[] {"covid", "19", "corona", "virus"}, false),
    VOMITING("vomiting", "", new String[] {"threw-up", "sickness", "ailment", "vomiting"}, false),
    WAR("war", "Conflict", new String[] {"battle", "armed", "war", "tank", "conflict"}, false),
    WASH_HANDS(
        "wash_hands",
        "Wash hands",
        new String[] {"cleaning", "hands", "wash", "water", "soap"},
        false),
    WATER_SANITATION("water_sanitation", "", new String[] {"sanitation", "water"}, false),
    WATER_TREATMENT("water_treatment", "", new String[] {"water treatment", "water"}, false),
    WEIGHT("weight", "", new String[] {"weight"}, false),
    WOLD_CARE(
        "wold_care",
        "World care",
        new String[] {"world care", "nature", "earth", "protection"},
        false),
    WOMAN("woman", "", new String[] {"woman", "female", "girl"}, false),
    YES("yes", "", new String[] {"yes"}, false),
    YOUNG_PEOPLE(
        "young_people",
        "Young people",
        new String[] {"young", "juvenile", "teens", "youth"},
        false);

    private static final String[] VARIANTS = {"positive", "negative", "outline"};

    public static final String SUFFIX = "svg";

    @Getter private final String key;

    @Getter private final String description;

    @Getter private final String[] keywords;

    @Getter private final boolean custom;

    Icons(String key, String description, String[] keywords, boolean custom) {
      this.key = key;
      this.description = description;
      this.keywords = keywords;
      this.custom = custom;
    }

    public Collection<DefaultIcon> getVariants() {
      return Arrays.stream(VARIANTS)
          .map(
              variant ->
                  new DefaultIcon(
                      String.format("%s_%s", getKey(), variant),
                      getDescription(),
                      getKeywords(),
                      isCustom()))
          .toList();
    }
  }
}

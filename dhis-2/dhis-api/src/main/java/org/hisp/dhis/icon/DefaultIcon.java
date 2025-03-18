/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import java.util.List;
import java.util.Set;
import lombok.Getter;

/** Default icons are pre-installed immutable icons. */
public enum DefaultIcon {
  _2G("2g", ""),
  _3G("3g", ""),
  _4X4("4x4", ""),
  AGRICULTURE("agriculture", "", "agriculture"),
  AGRICULTURE_WORKER("agriculture_worker", "", "agriculture", "worker"),
  ALERT("alert", ""),
  ALERT_CIRCLE("alert_circle", ""),
  ALERT_TRIANGLE("alert_triangle", ""),
  AMBULANCE("ambulance", "", "ambulance", "transport"),
  AMBULATORY_CLINIC("ambulatory_clinic", "", "refer", "health", "ambulatory"),
  ANCV("ancv", "ANC visit"),
  BABY_FEMALE_0203M("baby_female_0203m", "", "baby", "2-3 months", "male", "pediatric"),
  BABY_FEMALE_0306M("baby_female_0306m", "", "3-6 months", "baby", "male", "pediatric"),
  BABY_FEMALE_0609M("baby_female_0609m", "", "baby", "male", "6-9 months", "pediatric"),
  BABY_MALE_0203M("baby_male_0203m", "", "baby", "2-3 months", "female", "pediatric"),
  BABY_MALE_0306M("baby_male_0306m", "", "3-6 months", "baby", "female", "pediatric"),
  BABY_MALE_0609M("baby_male_0609m", "", "baby", "female", "6-9 months", "pediatric"),
  BASIC_MOTORCYCLE("basic_motorcycle", "", "motorcycle", "transport"),
  BIKE("bike", "", "transport", "bike"),
  BILLS("bills", "Bills", "money", "buck", "bank notes", "bills", "currency"),
  BLISTER_PILLS_OVAL_X1("blister_pills_oval_x1", "", "treatment", "pills", "1", "blister", "oval"),
  BLISTER_PILLS_OVAL_X14(
      "blister_pills_oval_x14", "", "treatment", "pills", "14", "blister", "oval"),
  BLISTER_PILLS_OVAL_X16(
      "blister_pills_oval_x16", "", "treatment", "pills", "16", "blister", "oval"),
  BLISTER_PILLS_OVAL_X4("blister_pills_oval_x4", "", "treatment", "pills", "4", "blister", "oval"),
  BLISTER_PILLS_ROUND_X1(
      "blister_pills_round_x1", "", "treatment", "pills", "1", "round", "blister"),
  BLISTER_PILLS_ROUND_X14(
      "blister_pills_round_x14", "", "treatment", "pills", "14", "round", "blister"),
  BLISTER_PILLS_ROUND_X16(
      "blister_pills_round_x16", "", "treatment", "pills", "round", "16", "blister"),
  BLISTER_PILLS_ROUND_X4(
      "blister_pills_round_x4", "", "treatment", "pills", "round", "4", "blister"),
  BLOOD_A_N("blood_a_n", "", "a", "hematology", "negative", "blood"),
  BLOOD_A_P("blood_a_p", "", "a", "hematology", "positive", "blood"),
  BLOOD_AB_N("blood_ab_n", "", "ab", "hematology", "negative", "blood"),
  BLOOD_AB_P("blood_ab_p", "", "ab", "hematology", "positive", "blood"),
  BLOOD_B_N("blood_b_n", "", "hematology", "b", "negative", "blood"),
  BLOOD_B_P("blood_b_p", "", "hematology", "b", "positive", "blood"),
  BLOOD_O_N("blood_o_n", "", "hematology", "negative", "blood", "o"),
  BLOOD_O_P("blood_o_p", "", "hematology", "positive", "blood", "o"),
  BLOOD_PRESSURE("blood_pressure", "", "Blood Pressure"),
  BLOOD_PRESSURE_2("blood_pressure_2", "", "pressure", "blood"),
  BLOOD_PRESSURE_MONITOR("blood_pressure_monitor", "", "pressure", "blood"),
  BLOOD_RH_N("blood_rh_n", "", "hematology", "negative", "rh", "blood"),
  BLOOD_RH_P("blood_rh_p", "", "hematology", "rh", "positive", "blood"),
  BOY_0105Y("boy_0105y", "", "1-5 Yrs", "boy"),
  BOY_1015Y("boy_1015y", "", "boy", "10-15 Yrs"),
  BREEDING_SITES(
      "breeding_sites", "Breeding sites", "pupa", "larva", "malaria", "mosquito", "reproduction"),
  CALENDAR("calendar", "", "calendar"),
  CARDIOGRAM("cardiogram", "Cardiogram card", "medical history"),
  CARDIOGRAM_E("cardiogram_e", "Edit ardiogram card", "Cardiogram card", "cardiogram"),
  CERVICAL_CANCER("cervical_cancer", "", "cervical", "cancer", "female"),
  CHILD_CARE("child_care", "Children", "toddler", "kid", "baby", "child", "care"),
  CHILD_PROGRAM("child_program", "child program"),
  CHILLS("chills", "", "symptom", "chills", "patient", "diagnosis"),
  CHOLERA("cholera", "", "vibrio cholerae", "cholera"),
  CHURCH("church", ""),
  CIRCLE_LARGE("circle_large", ""),
  CIRCLE_MEDIUM("circle_medium", ""),
  CIRCLE_SMALL("circle_small", ""),
  CITY("city", "City", "city", "house", "building", "edifice", "architecture"),
  CITY_WORKER("city_worker", "", "city worker", "worker"),
  CLEAN_HANDS("clean_hands", "Clean hands", "washing", "hands", "sanitizer", "clean", "soap"),
  CLINICAL_A("clinical_a", "Clinical analysis"),
  CLINICAL_F(
      "clinical_f",
      "Clinical file",
      "Clinical file",
      "medical history",
      "diagnosis test",
      "patient record"),
  CLINICAL_FE("clinical_fe", "Edit clinical file", "medical history", "diagnosis test"),
  COINS("coins", "Money", "wealth", "money", "payment", "cash"),
  COLD_CHAIN("cold_chain", "", "cold chain", "cold"),
  COMMUNICATION("communication", "", "contact", "communication"),
  CONE_TEST_ON_NETS(
      "cone_test_on_nets", "Cone test on nets", "test", "nets", "malaria", "analysis", "cone"),
  CONE_TEST_ON_WALLS(
      "cone_test_on_walls", "Cone test on walls", "test", "malaria", "analysis", "cone", "wall"),
  CONSTRUCTION("construction", "", "infrastructure", "construction", "worker"),
  CONSTRUCTION_WORKER("construction_worker", "", "construction", "worker"),
  CONTACT_SUPPORT("contact_support", "Contact support", "question", "contact", "support", "info"),
  CONTRACEPTIVE_DIAPHRAGM(
      "contraceptive_diaphragm", "", "female", "contraception", "sexual", "reproductive"),
  CONTRACEPTIVE_INJECTION(
      "contraceptive_injection",
      "",
      "inyectable",
      "female",
      "contraception",
      "sexual",
      "reproductive"),
  CONTRACEPTIVE_PATCH(
      "contraceptive_patch", "", "female", "contraception", "sexual", "reproductive"),
  CONTRACEPTIVE_VOUCHER("contraceptive_voucher", "contraceptive voucher"),
  COPPER_IUD("copper_iud", "", "copper iud", "copper", "contraception", "sexual", "reproductive"),
  COUGHING("coughing", "", "symptom", "coughing", "diagnosis", "cough"),
  CREDIT_CARD("credit_card", "Credit card", "bank", "money", "credit", "debit", "card"),
  CROSS_COUNTRY_MOTORCYCLE("cross_country_motorcycle", "", "motorcycle", "transport"),
  DEFAULT("default", "", "default"),
  DHIS2_LOGO("dhis2_logo", "DHIS2", "dhis2", "logo", "DHIS"),
  DIARRHEA("diarrhea", "", "symptom", "patient", "diagnosis", "diarrhea"),
  DISCRIMINATING_CONCENTRATION_BIOASSAYS(
      "discriminating_concentration_bioassays",
      "Discriminating concentration bioassays",
      "assay",
      "concentration",
      "analysis",
      "malaria",
      "mosquito"),
  DOCTOR("doctor", "Doctor", "doctor", "medical person", "nurse", "health", "worker"),
  DOMESTIC_WORKER("domestic_worker", "", "domestic", "domestic worker"),
  DONKEY("donkey", "", "donkey", "transport"),
  DRONE("drone", ""),
  ECO_CARE("eco_care", "Eco care", "eco", "world", "nature", "bio", "care"),
  ELDERLY("elderly", "Elderly", "old", "person", "aged", "aging", "people"),
  ELECTRICITY("electricity", "", "electricity"),
  EMERGENCY_POST("emergency_post", "", "emergency", "emergency post"),
  EXPECTORATE("expectorate", "", "symptom", "diagnosis", "expectorate"),
  FACTORY_WORKER("factory_worker", "", "factory", "worker"),
  FAMILY_PLANNING(
      "family_planning", "", "family planning", "contraception", "sexual", "reproductive"),
  FEMALE_AND_MALE("female_and_male", "Female and Male adult"),
  FEMALE_CONDOM("female_condom", "", "female", "contraception", "sexual", "reproductive", "condom"),
  FEMALE_SEX_WORKER("female_sex_worker", "", "female", "sex worker"),
  FETUS("fetus", "", "baby", "not born"),
  FEVER("fever", "", "symptom", "fever", "diagnosis"),
  FEVER_2("fever_2", "", "symptom", "fever", "diagnosis"),
  FEVER_CHILLS("fever_chills", "", "symptom", "chills", "patient", "fever", "diagnosis"),
  FOREST("forest", "", "forest"),
  FOREST_PERSONS("forest_persons", "", "persons", "forest"),
  FORUM("forum", "Forum", "forum", "chat", "discussion", "conversation"),
  GIRL_0105Y("girl_0105y", "", "1-5 Yrs", "girl"),
  GIRL_1015Y("girl_1015y", "", "girl", "10-15 Yrs"),
  GROUP_DISCUSSION_MEETING(
      "group_discussion_meeting", "", "contact", "communication", "meeting", "group"),
  GROUP_DISCUSSION_MEETINGX3(
      "group_discussion_meetingx3", "", "contact", "communication", "meeting", "group"),
  HAPPY("happy", "", "face"),
  HAZARDOUS("hazardous", "Hazardous", "hazardous", "difficult", "unsafe", "risky"),
  HEADACHE("headache", "", "symptom", "patient", "diagnosis", "headache"),
  HEALTH_WORKER("health_worker", "Health worker", "doctor", "nurse", "health", "worker"),
  HEALTH_WORKER_FORM(
      "health_worker_form", "Health worker form", "quiz", "questionnaire", "form", "health worker"),
  HEART("heart", "Heart", "love", "core", "favorite", "heart"),
  HEART_CARDIOGRAM("heart_cardiogram", "Heart - cardiogram", "electrocardiogram", "heart"),
  HELICOPTER("helicopter", ""),
  HIGH_BARS("high_bars", "High bars", "big", "high", "great", "bars"),
  HIGH_LEVEL("high_level", "High level", "big", "high", "level", "great"),
  HIV_IND("hiv_ind", "", "HIV", "indeterminate", "STI"),
  HIV_NEG("hiv_neg", "", "negative", "HIV", "STI"),
  HIV_POS("hiv_pos", "", "HIV", "STI", "positive"),
  HIV_SELF_TEST("hiv_self_test", "", "HIV", "test", "STI"),
  HOME("home", "Home", "cabin", "place", "house", "apartment", "home"),
  HORMONAL_RING(
      "hormonal_ring",
      "",
      "sterilization",
      "contraception",
      "sexual",
      "reproductive",
      "hormonal ring"),
  HOSPITAL("hospital", "", "refer", "hospital"),
  HOSPITALIZED("hospitalized", "", "hospitalized", "patient"),
  HOT_MEAL("hot_meal", "", "hot meal", "food"),
  HPV(
      "hpv",
      "",
      "Human papillomavirus",
      "papillomavirus",
      "STI",
      "diagnosis",
      "Sexual Transmitted"),
  I_CERTIFICATE_PAPER(
      "i_certificate_paper",
      "Certification",
      "qualification",
      "education",
      "credential",
      "certificate",
      "diploma"),
  I_DOCUMENTS_ACCEPTED(
      "i_documents_accepted", "Documents accepted", "documents", "accepted", "check", "done"),
  I_DOCUMENTS_DENIED(
      "i_documents_denied", "Documents denied", "refuse", "bad", "documents", "denied"),
  I_EXAM_MULTIPLE_CHOICE(
      "i_exam_multiple_choice", "Test", "exam", "quiz", "test", "multiple choice"),
  I_EXAM_QUALIFICATION(
      "i_exam_qualification", "Exam qualification", "exam", "qualification", "notes", "test"),
  I_GROUPS_PERSPECTIVE_CROWD(
      "i_groups_perspective_crowd", "Crowd", "persons", "crowd", "people", "group"),
  I_NOTE_ACTION(
      "i_note_action", "Homework", "notes", "homework", "assignment", "document", "write"),
  I_SCHEDULE_SCHOOL_DATE_TIME(
      "i_schedule_school_date_time", "Schedule", "date", "schedule", "school", "time"),
  I_TRAINING_CLASS("i_training_class", "Training", "teach", "training", "class", "board"),
  I_UTENSILS("i_utensils", "School utensils", "education", "school", "pen", "ruler", "utensils"),
  IMM("imm", "Inpatient morbidity and mortality"),
  IMPLANT("implant", "", "pellet", "implant", "contraception", "sexual", "reproductive"),
  INFO("info", "Info", "advice", "instruction", "information", "info"),
  INFORMATION_CAMPAIGN("information_campaign", "information campaign"),
  INPATIENT("inpatient", "", "patient", "inpatient"),
  INSECTICIDE_RESISTANCE(
      "insecticide_resistance",
      "Insecticide resistance",
      "insecticide",
      "malaria",
      "resistance",
      "mosquito"),
  INTENSITY_CONCENTRATION_BIOASSAYS(
      "intensity_concentration_bioassays",
      "Intensity concentration bioassays",
      "assay",
      "concentration",
      "analysis",
      "malaria",
      "mosquito"),
  IUD("iud", "", "iud", "contraception", "sexual", "reproductive"),
  JUSTICE("justice", "", "justice"),
  LACTATION("lactation", "", "ANC", "baby", "child", "pediatric"),
  LETRINA("letrina", "", "letrina"),
  LLIN("llin", "", "llin", "malaria", "net"),
  LOW_BARS("low_bars", "Low bars", "small", "low", "poor", "bars"),
  LOW_LEVEL("low_level", "Low level", "small", "low", "level", "poor"),
  MACHINERY("machinery", "Machinery", "excavator", "engine", "machinery", "vehicle"),
  MAGNIFYING_GLASS("magnifying_glass", "", "magnifying glass"),
  MALARIA_MIXED_MICROSCOPE("malaria_mixed_microscope", ""),
  MALARIA_NEGATIVE_MICROSCOPE("malaria_negative_microscope", ""),
  MALARIA_OUTBREAK("malaria_outbreak", "malaria outbreak", "midge", "outbreak", "denge", "malaria"),
  MALARIA_PF_MICROSCOPE("malaria_pf_microscope", ""),
  MALARIA_PV_MICROSCOPE("malaria_pv_microscope", ""),
  MALARIA_TESTING("malaria_testing", "malaria testing"),
  MALE_AND_FEMALE("male_and_female", "Male and Female icon", "man and female"),
  MALE_CONDOM("male_condom", "", "copper IUD", "contraception", "sexual", "reproductive"),
  MALE_SEX_WORKER("male_sex_worker", "", "sex worker", "male"),
  MAN("man", "", "man", "boy", "male"),
  MARKET_STALL("market_stall", "", "market"),
  MASK("mask", "Face mask", "face mask", "ffp", "virus", "mask"),
  MEASLES("measles", "", "symptom", "patient", "diagnosis", "measles"),
  MEDICINES("medicines", "", "treatment", "medicines"),
  MEDIUM_BARS("medium_bars", "Medium bars", "average", "normal", "common", "medium", "bars"),
  MEDIUM_LEVEL("medium_level", "Medium level", "average", "normal", "common", "level", "medium"),
  MEGAPHONE("megaphone", "", "megaphone", "communication"),
  MENTAL_DISORDERS("mental_disorders", "mental_disorders", "mental health", "mental disorder"),
  MICROSCOPE("microscope", "Optical Microscope", "laboratory", "lab", "analysis", "microscope"),
  MILITARY_WORKER("military_worker", "", "army", "military", "worker"),
  MINER_WORKER("miner_worker", "", "miner worker", "worker", "miner"),
  MOBILE_CLINIC("mobile_clinic", "", "refer", "health", "mobile clinic", "ambulance", "transport"),
  MONEY_BAG("money_bag", "Money bag", "money", "bag", "currency", "cash", "dollar"),
  MOSQUITO("mosquito", "", "denge", "malaria", "mosquito"),
  MOSQUITO_COLLECTION(
      "mosquito_collection",
      "Mosquito collection",
      "collection",
      "gather",
      "malaria",
      "case",
      "mosquito"),
  MSM("msm", "", "msm"),
  NAUSEA("nausea", "", "symptom", "patient", "diagnosis", "nausea"),
  NEGATIVE("negative", "", "negative"),
  NETWORK_4G("network_4g", "", "4g", "transfer", "connectivity", "net"),
  NETWORK_5G("network_5g", "", "5g", "transfer", "connectivity", "net"),
  NEUROLOGY("neurology", "", "mental health", "meningitis"),
  NEUTRAL("neutral", "", "face"),
  NO("no", "", "no"),
  NOT_OK("not_ok", "", "face"),
  NURSE("nurse", "Nurse", "doctor", "assistant", "nurse", "health", "medic"),
  OBSERVATION("observation", "", "observation", "patient"),
  ODONTOLOGY("odontology", "", "odontology"),
  ODONTOLOGY_IMPLANT("odontology_implant", "", "odontology", "implant"),
  OFFICER("officer", "Officer", "agent", "police", "cop", "officer"),
  OK("ok", "", "face"),
  OLD_MAN("old_man", "Old man", "old", "elderly", "aged", "man"),
  OLD_WOMAN("old_woman", "Old woman", "woman", "old", "elderly", "aged"),
  ORAL_CONTRACEPTION_PILLSX21(
      "oral_contraception_pillsx21",
      "",
      "treatment",
      "female",
      "contraception",
      "sexual",
      "reproductive"),
  ORAL_CONTRACEPTION_PILLSX28(
      "oral_contraception_pillsx28",
      "",
      "treatment",
      "female",
      "contraception",
      "sexual",
      "reproductive"),
  OUTPATIENT("outpatient", "", "patient", "outpatient"),
  OVERWEIGHT("overweight", "", "symptom", "patient", "diagnosis", "overweight"),
  PALM_BRANCHES_ROOF("palm_branches_roof", "", "roof", "laton", "zinc"),
  PAVE_ROAD("pave_road", "", "pave", "road"),
  PEACE("peace", "Peace", "love", "truce", "peace", "accord"),
  PEOPLE("people", "People", "crowd", "person", "community", "people", "group"),
  PERSON("person", "Person", "woman", "person", "man", "people"),
  PHONE("phone", "", "phone", "contact", "communication"),
  PILL_1("pill_1", "", "pills 1", "One pill"),
  PILLS_2("pills_2", "", "pills", "two"),
  PILLS_3("pills_3", "", "pills", "three"),
  PILLS_4("pills_4", "", "pills", "four"),
  PLANTATION_WORKER("plantation_worker", "", "plantation", "worker"),
  POLYGON("polygon", "", "polygon"),
  POSITIVE("positive", "", "positive"),
  PREGNANT("pregnant", "ANC", "ANC", "maternity", "pregnant"),
  PREGNANT_0812W("pregnant_0812w", "ANC", "First visit 8-12 weeks", "ANC", "maternity", "pregnant"),
  PREGNANT_2426W(
      "pregnant_2426w", "ANC", "Second visit 24-26 weeks", "ANC", "maternity", "pregnant"),
  PREGNANT_32W("pregnant_32w", "ANC", "ANC", "maternity", "Third visit 32 weeks", "pregnant"),
  PREGNANT_3638W(
      "pregnant_3638w", "ANC", "ANC", "maternity", "Fourth visit 36-38 weeks", "pregnant"),
  PRISONER("prisoner", "Prisoner", "detainee", "prisoner", "captive", "justice", "convict"),
  PROPER_ROOF("proper_roof", "", "roof", "proper"),
  PROVIDER_FST("provider_fst", "Provider Follow-up and Support Tool"),
  PWID("pwid", "", "pwid"),
  QUESTION("question", ""),
  QUESTION_CIRCLE("question_circle", ""),
  QUESTION_TRIANGLE("question_triangle", ""),
  RDT_RESULT_INVALID("rdt_result_invalid", "", "result", "rdt", "invalid", "diagnosis"),
  RDT_RESULT_MIXED("rdt_result_mixed", ""),
  RDT_RESULT_MIXED_INVALID("rdt_result_mixed_invalid", "", "result", "rdt", "pv", "diagnosis"),
  RDT_RESULT_MIXED_INVALID_RECTANGULAR(
      "rdt_result_mixed_invalid_rectangular", "", "result", "rdt", "invalid", "diagnosis", "mixed"),
  RDT_RESULT_MIXED_RECTANGULAR(
      "rdt_result_mixed_rectangular", "", "result", "rdt", "diagnosis", "mixed"),
  RDT_RESULT_NEG("rdt_result_neg", ""),
  RDT_RESULT_NEG_INVALID(
      "rdt_result_neg_invalid", "", "result", "neg", "rdt", "invalid", "diagnosis"),
  RDT_RESULT_NEG_INVALID_RECTANGULAR(
      "rdt_result_neg_invalid_rectangular", "", "result", "neg", "rdt", "invalid", "diagnosis"),
  RDT_RESULT_NEG_RECTANGULAR("rdt_result_neg_rectangular", "", "result", "neg", "rdt", "diagnosis"),
  RDT_RESULT_NEGATIVE("rdt_result_negative", "", "result", "negative", "rdt", "diagnosis"),
  RDT_RESULT_NO_TEST("rdt_result_no_test", ""),
  RDT_RESULT_OUT_STOCK("rdt_result_out_stock", "", "result", "out sock", "rdt"),
  RDT_RESULT_PF("rdt_result_pf", ""),
  RDT_RESULT_PF_INVALID("rdt_result_pf_invalid", "", "result", "rdt", "pf", "invalid", "diagnosis"),
  RDT_RESULT_PF_INVALID_RECTANGULAR(
      "rdt_result_pf_invalid_rectangular", "", "result", "rdt", "pf", "invalid", "diagnosis"),
  RDT_RESULT_PF_RECTANGULAR("rdt_result_pf_rectangular", "", "result", "rdt", "pf", "diagnosis"),
  RDT_RESULT_POSITIVE("rdt_result_positive", "", "result", "rdt", "diagnosis", "positive"),
  RDT_RESULT_PV("rdt_result_pv", ""),
  RDT_RESULT_PV_INVALID("rdt_result_pv_invalid", "", "result", "rdt", "pv", "invalid", "diagnosis"),
  RDT_RESULT_PV_INVALID_RECTANGULAR(
      "rdt_result_pv_invalid_rectangular", "", "result", "rdt", "pv", "invalid", "diagnosis"),
  RDT_RESULT_PV_RECTANGULAR("rdt_result_pv_rectangular", "", "result", "rdt", "pv", "diagnosis"),
  REFERRAL("referral", "", "referral", "patient", "health"),
  REFUSED("refused", "", "treatment", "refused"),
  RIBBON("ribbon", "", "ribbon", "STI"),
  RMNH("rmnh", "WHO RMNCH Tracker"),
  RUNNING_WATER("running_water", "", "running water", "water"),
  RURAL_POST("rural_post", "", "refer", "rural", "health", "clinic"),
  SAD("sad", "", "face"),
  SANITIZER("sanitizer", "Sanitizer", "hands", "sanitizer", "cleaner", "soap"),
  SAYANA_PRESS(
      "sayana_press", "", "inyectable", "female", "contraception", "sexual", "reproductive"),
  SECURITY_WORKER("security_worker", "", "worker", "cecurity"),
  SEXUAL_REPRODUCTIVE_HEALTH(
      "sexual_reproductive_health", "", "contraception", "sexual", "reproductive"),
  SMALL_PLANE("small_plane", ""),
  SOCIAL_DISTANCING(
      "social_distancing", "Social distancing", "distance", "social", "separation", "distancing"),
  SPRAYING("spraying", "", "spraying"),
  SQUARE_LARGE("square_large", ""),
  SQUARE_MEDIUM("square_medium", ""),
  SQUARE_SMALL("square_small", ""),
  STAR_LARGE("star_large", ""),
  STAR_MEDIUM("star_medium", ""),
  STAR_SMALL("star_small", ""),
  STETHOSCOPE("stethoscope", "", "stethoscope"),
  STI("sti", "", "symptom", "Trichomoniasis", "patient", "STI", "diagnosis"),
  STOCK_OUT("stock_out", "", "stock", "status"),
  STOP("stop", "", "stop"),
  SURGICAL_STERILIZATION(
      "surgical_sterilization",
      "",
      "circusicion",
      "sterilization",
      "contraception",
      "sexual",
      "reproductive"),
  SWEATING("sweating", "", "symptom", "patient", "diagnosis", "sweating"),
  SYMPTOM("symptom", "", "symptoms"),
  SYNERGIST_INSECTICIDE_BIOASSAYS(
      "synergist_insecticide_bioassays",
      "Synergist insecticide bioassays",
      "chemical",
      "assay",
      "analysis",
      "malaria",
      "mosquito"),
  SYRINGE("syringe", "", "syringe", "injectable", "vaccination"),
  TAC("tac", "Computed axial tomography"),
  TB("tb", "TB program"),
  TRANSGENDER("transgender", "", "transgender"),
  TRAUMATISM("traumatism", "", "patient", "traumatism"),
  TRAVEL("travel", "", "travel"),
  TREATED_WATER("treated_water", "", "treated", "infrastructure", "water"),
  TRIANGLE_LARGE("triangle_large", ""),
  TRIANGLE_MEDIUM("triangle_medium", ""),
  TRIANGLE_SMALL("triangle_small", ""),
  TRUCK_DRIVER("truck_driver", "", "driver", "truck driver", "worker"),
  UN_PAVE_ROAD("un_pave_road", "", "pave", "road"),
  UNDERWEIGHT("underweight", "", "symptom", "patient", "diagnosis", "underweight"),
  VESPA_MOTORCYCLE("vespa_motorcycle", "", "motorcycle", "transport"),
  VIH_AIDS("vih_aids", "VIH/AIDS", "hematology", "vih", "STI", "aids", "blood"),
  VIRUS("virus", "Coronavirus", "covid", "19", "corona", "virus"),
  VOMITING("vomiting", "", "threw-up", "sickness", "ailment", "vomiting"),
  WAR("war", "Conflict", "battle", "armed", "war", "tank", "conflict"),
  WASH_HANDS("wash_hands", "Wash hands", "cleaning", "hands", "wash", "water", "soap"),
  WATER_SANITATION("water_sanitation", "", "sanitation", "water"),
  WATER_TREATMENT("water_treatment", "", "water treatment", "water"),
  WEIGHT("weight", "", "weight"),
  WOLD_CARE("wold_care", "World care", "world care", "nature", "earth", "protection"),
  WOMAN("woman", "", "woman", "female", "girl"),
  YES("yes", "", "yes"),
  YOUNG_PEOPLE("young_people", "Young people", "young", "juvenile", "teens", "youth");

  public static final String SUFFIX = "svg";

  @Getter private final String keyPrefix;
  @Getter private final String description;
  @Getter private final Set<String> keywords;
  @Getter private final List<String> variantKeys;

  DefaultIcon(String keyPrefix, String description, String... keywords) {
    this.keyPrefix = keyPrefix;
    this.description = description;
    this.keywords = Set.of(keywords);
    this.variantKeys =
        Set.of("positive", "negative", "outline").stream()
            .map(variant -> "%s_%s".formatted(keyPrefix, variant))
            .toList();
  }

  public List<AddIconRequest> toVariantIcons() {
    return getVariantKeys().stream()
        .map(
            key ->
                AddIconRequest.builder()
                    .key(key)
                    .description(getDescription())
                    .keywords(getKeywords())
                    .build())
        .toList();
  }
}

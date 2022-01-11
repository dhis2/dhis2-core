/*
 * Copyright (c) 2004-2022, University of Oslo
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Kristian Wærstad
 */
public enum Icon
{
    _2G( "2g", "", new String[] {} ),
    _3G( "3g", "", new String[] {} ),
    _4X4( "4x4", "", new String[] {} ),
    AGRICULTURE( "agriculture", "", new String[] { "agriculture" } ),
    AGRICULTURE_WORKER( "agriculture_worker", "", new String[] { "agriculture", "worker" } ),
    ALERT( "alert", "", new String[] {} ),
    ALERT_CIRCLE( "alert_circle", "", new String[] {} ),
    ALERT_TRIANGLE( "alert_triangle", "", new String[] {} ),
    AMBULANCE( "ambulance", "", new String[] { "ambulance", "transport" } ),
    AMBULATORY_CLINIC( "ambulatory_clinic", "", new String[] { "refer", "health", "ambulatory" } ),
    ANCV( "ancv", "ANC visit", new String[] {} ),
    BABY_FEMALE_0203M( "baby_female_0203m", "", new String[] { "baby", "2-3 months", "male", "pediatric" } ),
    BABY_FEMALE_0306M( "baby_female_0306m", "", new String[] { "3-6 months", "baby", "male", "pediatric" } ),
    BABY_FEMALE_0609M( "baby_female_0609m", "", new String[] { "baby", "male", "6-9 months", "pediatric" } ),
    BABY_MALE_0203M( "baby_male_0203m", "", new String[] { "baby", "2-3 months", "female", "pediatric" } ),
    BABY_MALE_0306M( "baby_male_0306m", "", new String[] { "3-6 months", "baby", "female", "pediatric" } ),
    BABY_MALE_0609M( "baby_male_0609m", "", new String[] { "baby", "female", "6-9 months", "pediatric" } ),
    BASIC_MOTORCYCLE( "basic_motorcycle", "", new String[] { "motorcycle", "transport" } ),
    BIKE( "bike", "", new String[] { "transport", "bike" } ),
    BILLS( "bills", "Bills", new String[] { "money", "buck", "bank notes", "bills", "currency" } ),
    BLISTER_PILLS_OVAL_X1( "blister_pills_oval_x1", "", new String[] { "treatment", "pills", "1", "blister", "oval" } ),
    BLISTER_PILLS_OVAL_X14( "blister_pills_oval_x14", "",
        new String[] { "treatment", "pills", "14", "blister", "oval" } ),
    BLISTER_PILLS_OVAL_X16( "blister_pills_oval_x16", "",
        new String[] { "treatment", "pills", "16", "blister", "oval" } ),
    BLISTER_PILLS_OVAL_X4( "blister_pills_oval_x4", "", new String[] { "treatment", "pills", "4", "blister", "oval" } ),
    BLISTER_PILLS_ROUND_X1( "blister_pills_round_x1", "",
        new String[] { "treatment", "pills", "1", "round", "blister" } ),
    BLISTER_PILLS_ROUND_X14( "blister_pills_round_x14", "",
        new String[] { "treatment", "pills", "14", "round", "blister" } ),
    BLISTER_PILLS_ROUND_X16( "blister_pills_round_x16", "",
        new String[] { "treatment", "pills", "round", "16", "blister" } ),
    BLISTER_PILLS_ROUND_X4( "blister_pills_round_x4", "",
        new String[] { "treatment", "pills", "round", "4", "blister" } ),
    BLOOD_A_N( "blood_a_n", "", new String[] { "a", "hematology", "negative", "blood" } ),
    BLOOD_A_P( "blood_a_p", "", new String[] { "a", "hematology", "positive", "blood" } ),
    BLOOD_AB_N( "blood_ab_n", "", new String[] { "ab", "hematology", "negative", "blood" } ),
    BLOOD_AB_P( "blood_ab_p", "", new String[] { "ab", "hematology", "positive", "blood" } ),
    BLOOD_B_N( "blood_b_n", "", new String[] { "hematology", "b", "negative", "blood" } ),
    BLOOD_B_P( "blood_b_p", "", new String[] { "hematology", "b", "positive", "blood" } ),
    BLOOD_O_N( "blood_o_n", "", new String[] { "hematology", "negative", "blood", "o" } ),
    BLOOD_O_P( "blood_o_p", "", new String[] { "hematology", "positive", "blood", "o" } ),
    BLOOD_PRESSURE( "blood_pressure", "", new String[] { "Blood Pressure" } ),
    BLOOD_PRESSURE_2( "blood_pressure_2", "", new String[] { "pressure", "blood" } ),
    BLOOD_PRESSURE_MONITOR( "blood_pressure_monitor", "", new String[] { "pressure", "blood" } ),
    BLOOD_RH_N( "blood_rh_n", "", new String[] { "hematology", "negative", "rh", "blood" } ),
    BLOOD_RH_P( "blood_rh_p", "", new String[] { "hematology", "rh", "positive", "blood" } ),
    BOY_0105Y( "boy_0105y", "", new String[] { "1-5 Yrs", "boy" } ),
    BOY_1015Y( "boy_1015y", "", new String[] { "boy", "10-15 Yrs" } ),
    BREEDING_SITES( "breeding_sites", "Breeding sites",
        new String[] { "pupa", "larva", "malaria", "mosquito", "reproduction" } ),
    CALENDAR( "calendar", "", new String[] { "calendar" } ),
    CARDIOGRAM( "cardiogram", "Cardiogram card", new String[] { "medical history" } ),
    CARDIOGRAM_E( "cardiogram_e", "Edit ardiogram card", new String[] { "Cardiogram card", "cardiogram" } ),
    CERVICAL_CANCER( "cervical_cancer", "", new String[] { "cervical", "cancer", "female" } ),
    CHILD_CARE( "child_care", "Children", new String[] { "toddler", "kid", "baby", "child", "care" } ),
    CHILD_PROGRAM( "child_program", "child program", new String[] {} ),
    CHILLS( "chills", "", new String[] { "symptom", "chills", "patient", "diagnosis" } ),
    CHOLERA( "cholera", "", new String[] { "vibrio cholerae", "cholera" } ),
    CHURCH( "church", "", new String[] {} ),
    CIRCLE_LARGE( "circle_large", "", new String[] {} ),
    CIRCLE_MEDIUM( "circle_medium", "", new String[] {} ),
    CIRCLE_SMALL( "circle_small", "", new String[] {} ),
    CITY( "city", "City", new String[] { "city", "house", "building", "edifice", "architecture" } ),
    CITY_WORKER( "city_worker", "", new String[] { "city worker", "worker" } ),
    CLEAN_HANDS( "clean_hands", "Clean hands", new String[] { "washing", "hands", "sanitizer", "clean", "soap" } ),
    CLINICAL_A( "clinical_a", "Clinical analysis", new String[] {} ),
    CLINICAL_F( "clinical_f", "Clinical file",
        new String[] { "Clinical file", "medical history", "diagnosis test", "patient record" } ),
    CLINICAL_FE( "clinical_fe", "Edit clinical file", new String[] { "medical history", "diagnosis test" } ),
    COINS( "coins", "Money", new String[] { "wealth", "money", "payment", "cash" } ),
    COLD_CHAIN( "cold_chain", "", new String[] { "cold chain", "cold" } ),
    COMMUNICATION( "communication", "", new String[] { "contact", "communication" } ),
    CONE_TEST_ON_NETS( "cone_test_on_nets", "Cone test on nets",
        new String[] { "test", "nets", "malaria", "analysis", "cone" } ),
    CONE_TEST_ON_WALLS( "cone_test_on_walls", "Cone test on walls",
        new String[] { "test", "malaria", "analysis", "cone", "wall" } ),
    CONSTRUCTION( "construction", "", new String[] { "infrastructure", "construction", "worker" } ),
    CONSTRUCTION_WORKER( "construction_worker", "", new String[] { "construction", "worker" } ),
    CONTACT_SUPPORT( "contact_support", "Contact support", new String[] { "question", "contact", "support", "info" } ),
    CONTRACEPTIVE_DIAPHRAGM( "contraceptive_diaphragm", "",
        new String[] { "female", "contraception", "sexual", "reproductive" } ),
    CONTRACEPTIVE_INJECTION( "contraceptive_injection", "",
        new String[] { "inyectable", "female", "contraception", "sexual", "reproductive" } ),
    CONTRACEPTIVE_PATCH( "contraceptive_patch", "",
        new String[] { "female", "contraception", "sexual", "reproductive" } ),
    CONTRACEPTIVE_VOUCHER( "contraceptive_voucher", "contraceptive voucher", new String[] {} ),
    COPPER_IUD( "copper_iud", "", new String[] { "copper iud", "copper", "contraception", "sexual", "reproductive" } ),
    COUGHING( "coughing", "", new String[] { "symptom", "coughing", "diagnosis", "cough" } ),
    CREDIT_CARD( "credit_card", "Credit card", new String[] { "bank", "money", "credit", "debit", "card" } ),
    CROSS_COUNTRY_MOTORCYCLE( "cross_country_motorcycle", "", new String[] { "motorcycle", "transport" } ),
    DEFAULT( "default", "", new String[] { "default" } ),
    DHIS2_LOGO( "dhis2_logo", "DHIS2", new String[] { "dhis2", "logo", "DHIS" } ),
    DIARRHEA( "diarrhea", "", new String[] { "symptom", "patient", "diagnosis", "diarrhea" } ),
    DISCRIMINATING_CONCENTRATION_BIOASSAYS( "discriminating_concentration_bioassays",
        "Discriminating concentration bioassays",
        new String[] { "assay", "concentration", "analysis", "malaria", "mosquito" } ),
    DOCTOR( "doctor", "Doctor", new String[] { "doctor", "medical person", "nurse", "health", "worker" } ),
    DOMESTIC_WORKER( "domestic_worker", "", new String[] { "domestic", "domestic worker" } ),
    DONKEY( "donkey", "", new String[] { "donkey", "transport" } ),
    DRONE( "drone", "", new String[] {} ),
    ECO_CARE( "eco_care", "Eco care", new String[] { "eco", "world", "nature", "bio", "care" } ),
    ELDERLY( "elderly", "Elderly", new String[] { "old", "person", "aged", "aging", "people" } ),
    ELECTRICITY( "electricity", "", new String[] { "electricity" } ),
    EMERGENCY_POST( "emergency_post", "", new String[] { "emergency", "emergency post" } ),
    EXPECTORATE( "expectorate", "", new String[] { "symptom", "diagnosis", "expectorate" } ),
    FACTORY_WORKER( "factory_worker", "", new String[] { "factory", "worker" } ),
    FAMILY_PLANNING( "family_planning", "",
        new String[] { "family planning", "contraception", "sexual", "reproductive" } ),
    FEMALE_AND_MALE( "female_and_male", "Female and Male adult", new String[] {} ),
    FEMALE_CONDOM( "female_condom", "",
        new String[] { "female", "contraception", "sexual", "reproductive", "condom" } ),
    FEMALE_SEX_WORKER( "female_sex_worker", "", new String[] { "female", "sex worker" } ),
    FETUS( "fetus", "", new String[] { "baby", "not born" } ),
    FEVER( "fever", "", new String[] { "symptom", "fever", "diagnosis" } ),
    FEVER_2( "fever_2", "", new String[] { "symptom", "fever", "diagnosis" } ),
    FEVER_CHILLS( "fever_chills", "", new String[] { "symptom", "chills", "patient", "fever", "diagnosis" } ),
    FOREST( "forest", "", new String[] { "forest" } ),
    FOREST_PERSONS( "forest_persons", "", new String[] { "persons", "forest" } ),
    FORUM( "forum", "Forum", new String[] { "forum", "chat", "discussion", "conversation" } ),
    GIRL_0105Y( "girl_0105y", "", new String[] { "1-5 Yrs", "girl" } ),
    GIRL_1015Y( "girl_1015y", "", new String[] { "girl", "10-15 Yrs" } ),
    GROUP_DISCUSSION_MEETING( "group_discussion_meeting", "",
        new String[] { "contact", "communication", "meeting", "group" } ),
    GROUP_DISCUSSION_MEETINGX3( "group_discussion_meetingx3", "",
        new String[] { "contact", "communication", "meeting", "group" } ),
    HAPPY( "happy", "", new String[] { "face" } ),
    HAZARDOUS( "hazardous", "Hazardous", new String[] { "hazardous", "difficult", "unsafe", "risky" } ),
    HEADACHE( "headache", "", new String[] { "symptom", "patient", "diagnosis", "headache" } ),
    HEALTH_WORKER( "health_worker", "Health worker", new String[] { "doctor", "nurse", "health", "worker" } ),
    HEALTH_WORKER_FORM( "health_worker_form", "Health worker form",
        new String[] { "quiz", "questionnaire", "form", "health worker" } ),
    HEART( "heart", "Heart", new String[] { "love", "core", "favorite", "heart" } ),
    HEART_CARDIOGRAM( "heart_cardiogram", "Heart - cardiogram", new String[] { "electrocardiogram", "heart" } ),
    HELICOPTER( "helicopter", "", new String[] {} ),
    HIGH_BARS( "high_bars", "High bars", new String[] { "big", "high", "great", "bars" } ),
    HIGH_LEVEL( "high_level", "High level", new String[] { "big", "high", "level", "great" } ),
    HIV_IND( "hiv_ind", "", new String[] { "HIV", "indeterminate", "STI" } ),
    HIV_NEG( "hiv_neg", "", new String[] { "negative", "HIV", "STI" } ),
    HIV_POS( "hiv_pos", "", new String[] { "HIV", "STI", "positive" } ),
    HIV_SELF_TEST( "hiv_self_test", "", new String[] { "HIV", "test", "STI" } ),
    HOME( "home", "Home", new String[] { "cabin", "place", "house", "apartment", "home" } ),
    HORMONAL_RING( "hormonal_ring", "",
        new String[] { "sterilization", "contraception", "sexual", "reproductive", "hormonal ring" } ),
    HOSPITAL( "hospital", "", new String[] { "refer", "hospital" } ),
    HOSPITALIZED( "hospitalized", "", new String[] { "hospitalized", "patient" } ),
    HOT_MEAL( "hot_meal", "", new String[] { "hot meal", "food" } ),
    HPV( "hpv", "",
        new String[] { "Human papillomavirus", "papillomavirus", "STI", "diagnosis", "Sexual Transmitted" } ),
    I_CERTIFICATE_PAPER( "i_certificate_paper", "Certification",
        new String[] { "qualification", "education", "credential", "certificate", "diploma" } ),
    I_DOCUMENTS_ACCEPTED( "i_documents_accepted", "Documents accepted",
        new String[] { "documents", "accepted", "check", "done" } ),
    I_DOCUMENTS_DENIED( "i_documents_denied", "Documents denied",
        new String[] { "refuse", "bad", "documents", "denied" } ),
    I_EXAM_MULTIPLE_CHOICE( "i_exam_multiple_choice", "Test",
        new String[] { "exam", "quiz", "test", "multiple choice" } ),
    I_EXAM_QUALIFICATION( "i_exam_qualification", "Exam qualification",
        new String[] { "exam", "qualification", "notes", "test" } ),
    I_GROUPS_PERSPECTIVE_CROWD( "i_groups_perspective_crowd", "Crowd",
        new String[] { "persons", "crowd", "people", "group" } ),
    I_NOTE_ACTION( "i_note_action", "Homework",
        new String[] { "notes", "homework", "assignment", "document", "write" } ),
    I_SCHEDULE_SCHOOL_DATE_TIME( "i_schedule_school_date_time", "Schedule",
        new String[] { "date", "schedule", "school", "time" } ),
    I_TRAINING_CLASS( "i_training_class", "Training", new String[] { "teach", "training", "class", "board" } ),
    I_UTENSILS( "i_utensils", "School utensils", new String[] { "education", "school", "pen", "ruler", "utensils" } ),
    IMM( "imm", "Inpatient morbidity and mortality", new String[] {} ),
    IMPLANT( "implant", "", new String[] { "pellet", "implant", "contraception", "sexual", "reproductive" } ),
    INFO( "info", "Info", new String[] { "advice", "instruction", "information", "info" } ),
    INFORMATION_CAMPAIGN( "information_campaign", "information campaign", new String[] {} ),
    INPATIENT( "inpatient", "", new String[] { "patient", "inpatient" } ),
    INSECTICIDE_RESISTANCE( "insecticide_resistance", "Insecticide resistance",
        new String[] { "insecticide", "malaria", "resistance", "mosquito" } ),
    INTENSITY_CONCENTRATION_BIOASSAYS( "intensity_concentration_bioassays", "Intensity concentration bioassays",
        new String[] { "assay", "concentration", "analysis", "malaria", "mosquito" } ),
    IUD( "iud", "", new String[] { "iud", "contraception", "sexual", "reproductive" } ),
    JUSTICE( "justice", "", new String[] { "justice" } ),
    LACTATION( "lactation", "", new String[] { "ANC", "baby", "child", "pediatric" } ),
    LETRINA( "letrina", "", new String[] { "letrina" } ),
    LLIN( "llin", "", new String[] { "llin", "malaria", "net" } ),
    LOW_BARS( "low_bars", "Low bars", new String[] { "small", "low", "poor", "bars" } ),
    LOW_LEVEL( "low_level", "Low level", new String[] { "small", "low", "level", "poor" } ),
    MACHINERY( "machinery", "Machinery", new String[] { "excavator", "engine", "machinery", "vehicle" } ),
    MAGNIFYING_GLASS( "magnifying_glass", "", new String[] { "magnifying glass" } ),
    MALARIA_MIXED_MICROSCOPE( "malaria_mixed_microscope", "", new String[] {} ),
    MALARIA_NEGATIVE_MICROSCOPE( "malaria_negative_microscope", "", new String[] {} ),
    MALARIA_OUTBREAK( "malaria_outbreak", "malaria outbreak",
        new String[] { "midge", "outbreak", "denge", "malaria" } ),
    MALARIA_PF_MICROSCOPE( "malaria_pf_microscope", "", new String[] {} ),
    MALARIA_PV_MICROSCOPE( "malaria_pv_microscope", "", new String[] {} ),
    MALARIA_TESTING( "malaria_testing", "malaria testing", new String[] {} ),
    MALE_AND_FEMALE( "male_and_female", "Male and Female icon", new String[] { "man and female" } ),
    MALE_CONDOM( "male_condom", "", new String[] { "copper IUD", "contraception", "sexual", "reproductive" } ),
    MALE_SEX_WORKER( "male_sex_worker", "", new String[] { "sex worker", "male" } ),
    MAN( "man", "", new String[] { "man", "boy", "male" } ),
    MARKET_STALL( "market_stall", "", new String[] { "market" } ),
    MASK( "mask", "Face mask", new String[] { "face mask", "ffp", "virus", "mask" } ),
    MEASLES( "measles", "", new String[] { "symptom", "patient", "diagnosis", "measles" } ),
    MEDICINES( "medicines", "", new String[] { "treatment", "medicines" } ),
    MEDIUM_BARS( "medium_bars", "Medium bars", new String[] { "average", "normal", "common", "medium", "bars" } ),
    MEDIUM_LEVEL( "medium_level", "Medium level", new String[] { "average", "normal", "common", "level", "medium" } ),
    MEGAPHONE( "megaphone", "", new String[] { "megaphone", "communication" } ),
    MENTAL_DISORDERS( "mental_disorders", "mental_disorders", new String[] { "mental health", "mental disorder" } ),
    MICROSCOPE( "microscope", "Optical Microscope", new String[] { "laboratory", "lab", "analysis", "microscope" } ),
    MILITARY_WORKER( "military_worker", "", new String[] { "army", "military", "worker" } ),
    MINER_WORKER( "miner_worker", "", new String[] { "miner worker", "worker", "miner" } ),
    MOBILE_CLINIC( "mobile_clinic", "", new String[] { "refer", "health", "mobile clinic", "ambulance", "transport" } ),
    MONEY_BAG( "money_bag", "Money bag", new String[] { "money", "bag", "currency", "cash", "dollar" } ),
    MOSQUITO( "mosquito", "", new String[] { "denge", "malaria", "mosquito" } ),
    MOSQUITO_COLLECTION( "mosquito_collection", "Mosquito collection",
        new String[] { "collection", "gather", "malaria", "case", "mosquito" } ),
    MSM( "msm", "", new String[] { "msm" } ),
    NAUSEA( "nausea", "", new String[] { "symptom", "patient", "diagnosis", "nausea" } ),
    NEGATIVE( "negative", "", new String[] { "negative" } ),
    NETWORK_4G( "network_4g", "", new String[] { "4g", "transfer", "connectivity", "net" } ),
    NETWORK_5G( "network_5g", "", new String[] { "5g", "transfer", "connectivity", "net" } ),
    NEUROLOGY( "neurology", "", new String[] { "mental health", "meningitis" } ),
    NEUTRAL( "neutral", "", new String[] { "face" } ),
    NO( "no", "", new String[] { "no" } ),
    NOT_OK( "not_ok", "", new String[] { "face" } ),
    NURSE( "nurse", "Nurse", new String[] { "doctor", "assistant", "nurse", "health", "medic" } ),
    OBSERVATION( "observation", "", new String[] { "observation", "patient" } ),
    ODONTOLOGY( "odontology", "", new String[] { "odontology" } ),
    ODONTOLOGY_IMPLANT( "odontology_implant", "", new String[] { "odontology", "implant" } ),
    OFFICER( "officer", "Officer", new String[] { "agent", "police", "cop", "officer" } ),
    OK( "ok", "", new String[] { "face" } ),
    OLD_MAN( "old_man", "Old man", new String[] { "old", "elderly", "aged", "man" } ),
    OLD_WOMAN( "old_woman", "Old woman", new String[] { "woman", "old", "elderly", "aged" } ),
    ORAL_CONTRACEPTION_PILLSX21( "oral_contraception_pillsx21", "",
        new String[] { "treatment", "female", "contraception", "sexual", "reproductive" } ),
    ORAL_CONTRACEPTION_PILLSX28( "oral_contraception_pillsx28", "",
        new String[] { "treatment", "female", "contraception", "sexual", "reproductive" } ),
    OUTPATIENT( "outpatient", "", new String[] { "patient", "outpatient" } ),
    OVERWEIGHT( "overweight", "", new String[] { "symptom", "patient", "diagnosis", "overweight" } ),
    PALM_BRANCHES_ROOF( "palm_branches_roof", "", new String[] { "roof", "laton", "zinc" } ),
    PAVE_ROAD( "pave_road", "", new String[] { "pave", "road" } ),
    PEACE( "peace", "Peace", new String[] { "love", "truce", "peace", "accord" } ),
    PEOPLE( "people", "People", new String[] { "crowd", "person", "community", "people", "group" } ),
    PERSON( "person", "Person", new String[] { "woman", "person", "man", "people" } ),
    PHONE( "phone", "", new String[] { "phone", "contact", "communication" } ),
    PILL_1( "pill_1", "", new String[] { "pills 1", "One pill" } ),
    PILLS_2( "pills_2", "", new String[] { "pills", "two" } ),
    PILLS_3( "pills_3", "", new String[] { "pills", "three" } ),
    PILLS_4( "pills_4", "", new String[] { "pills", "four" } ),
    PLANTATION_WORKER( "plantation_worker", "", new String[] { "plantation", "worker" } ),
    POLYGON( "polygon", "", new String[] { "polygon" } ),
    POSITIVE( "positive", "", new String[] { "positive" } ),
    PREGNANT( "pregnant", "ANC", new String[] { "ANC", "maternity", "pregnant" } ),
    PREGNANT_0812W( "pregnant_0812w", "ANC",
        new String[] { "First visit 8-12 weeks", "ANC", "maternity", "pregnant" } ),
    PREGNANT_2426W( "pregnant_2426w", "ANC",
        new String[] { "Second visit 24-26 weeks", "ANC", "maternity", "pregnant" } ),
    PREGNANT_32W( "pregnant_32w", "ANC", new String[] { "ANC", "maternity", "Third visit 32 weeks", "pregnant" } ),
    PREGNANT_3638W( "pregnant_3638w", "ANC",
        new String[] { "ANC", "maternity", "Fourth visit 36-38 weeks", "pregnant" } ),
    PRISONER( "prisoner", "Prisoner", new String[] { "detainee", "prisoner", "captive", "justice", "convict" } ),
    PROPER_ROOF( "proper_roof", "", new String[] { "roof", "proper" } ),
    PROVIDER_FST( "provider_fst", "Provider Follow-up and Support Tool", new String[] {} ),
    PWID( "pwid", "", new String[] { "pwid" } ),
    QUESTION( "question", "", new String[] {} ),
    QUESTION_CIRCLE( "question_circle", "", new String[] {} ),
    QUESTION_TRIANGLE( "question_triangle", "", new String[] {} ),
    RDT_RESULT_INVALID( "rdt_result_invalid", "", new String[] { "result", "rdt", "invalid", "diagnosis" } ),
    RDT_RESULT_MIXED( "rdt_result_mixed", "", new String[] {} ),
    RDT_RESULT_MIXED_INVALID( "rdt_result_mixed_invalid", "", new String[] { "result", "rdt", "pv", "diagnosis" } ),
    RDT_RESULT_MIXED_INVALID_RECTANGULAR( "rdt_result_mixed_invalid_rectangular", "",
        new String[] { "result", "rdt", "invalid", "diagnosis", "mixed" } ),
    RDT_RESULT_MIXED_RECTANGULAR( "rdt_result_mixed_rectangular", "",
        new String[] { "result", "rdt", "diagnosis", "mixed" } ),
    RDT_RESULT_NEG( "rdt_result_neg", "", new String[] {} ),
    RDT_RESULT_NEG_INVALID( "rdt_result_neg_invalid", "",
        new String[] { "result", "neg", "rdt", "invalid", "diagnosis" } ),
    RDT_RESULT_NEG_INVALID_RECTANGULAR( "rdt_result_neg_invalid_rectangular", "",
        new String[] { "result", "neg", "rdt", "invalid", "diagnosis" } ),
    RDT_RESULT_NEG_RECTANGULAR( "rdt_result_neg_rectangular", "",
        new String[] { "result", "neg", "rdt", "diagnosis" } ),
    RDT_RESULT_NEGATIVE( "rdt_result_negative", "", new String[] { "result", "negative", "rdt", "diagnosis" } ),
    RDT_RESULT_NO_TEST( "rdt_result_no_test", "", new String[] {} ),
    RDT_RESULT_OUT_STOCK( "rdt_result_out_stock", "", new String[] { "result", "out sock", "rdt" } ),
    RDT_RESULT_PF( "rdt_result_pf", "", new String[] {} ),
    RDT_RESULT_PF_INVALID( "rdt_result_pf_invalid", "",
        new String[] { "result", "rdt", "pf", "invalid", "diagnosis" } ),
    RDT_RESULT_PF_INVALID_RECTANGULAR( "rdt_result_pf_invalid_rectangular", "",
        new String[] { "result", "rdt", "pf", "invalid", "diagnosis" } ),
    RDT_RESULT_PF_RECTANGULAR( "rdt_result_pf_rectangular", "", new String[] { "result", "rdt", "pf", "diagnosis" } ),
    RDT_RESULT_POSITIVE( "rdt_result_positive", "", new String[] { "result", "rdt", "diagnosis", "positive" } ),
    RDT_RESULT_PV( "rdt_result_pv", "", new String[] {} ),
    RDT_RESULT_PV_INVALID( "rdt_result_pv_invalid", "",
        new String[] { "result", "rdt", "pv", "invalid", "diagnosis" } ),
    RDT_RESULT_PV_INVALID_RECTANGULAR( "rdt_result_pv_invalid_rectangular", "",
        new String[] { "result", "rdt", "pv", "invalid", "diagnosis" } ),
    RDT_RESULT_PV_RECTANGULAR( "rdt_result_pv_rectangular", "", new String[] { "result", "rdt", "pv", "diagnosis" } ),
    REFERRAL( "referral", "", new String[] { "referral", "patient", "health" } ),
    REFUSED( "refused", "", new String[] { "treatment", "refused" } ),
    RIBBON( "ribbon", "", new String[] { "ribbon", "STI" } ),
    RMNH( "rmnh", "WHO RMNCH Tracker", new String[] {} ),
    RUNNING_WATER( "running_water", "", new String[] { "running water", "water" } ),
    RURAL_POST( "rural_post", "", new String[] { "refer", "rural", "health", "clinic" } ),
    SAD( "sad", "", new String[] { "face" } ),
    SANITIZER( "sanitizer", "Sanitizer", new String[] { "hands", "sanitizer", "cleaner", "soap" } ),
    SAYANA_PRESS( "sayana_press", "",
        new String[] { "inyectable", "female", "contraception", "sexual", "reproductive" } ),
    SECURITY_WORKER( "security_worker", "", new String[] { "worker", "cecurity" } ),
    SEXUAL_REPRODUCTIVE_HEALTH( "sexual_reproductive_health", "",
        new String[] { "contraception", "sexual", "reproductive" } ),
    SMALL_PLANE( "small_plane", "", new String[] {} ),
    SOCIAL_DISTANCING( "social_distancing", "Social distancing",
        new String[] { "distance", "social", "separation", "distancing" } ),
    SPRAYING( "spraying", "", new String[] { "spraying" } ),
    SQUARE_LARGE( "square_large", "", new String[] {} ),
    SQUARE_MEDIUM( "square_medium", "", new String[] {} ),
    SQUARE_SMALL( "square_small", "", new String[] {} ),
    STAR_LARGE( "star_large", "", new String[] {} ),
    STAR_MEDIUM( "star_medium", "", new String[] {} ),
    STAR_SMALL( "star_small", "", new String[] {} ),
    STETHOSCOPE( "stethoscope", "", new String[] { "stethoscope" } ),
    STI( "sti", "", new String[] { "symptom", "Trichomoniasis", "patient", "STI", "diagnosis" } ),
    STOCK_OUT( "stock_out", "", new String[] { "stock", "status" } ),
    STOP( "stop", "", new String[] { "stop" } ),
    SURGICAL_STERILIZATION( "surgical_sterilization", "",
        new String[] { "circusicion", "sterilization", "contraception", "sexual", "reproductive" } ),
    SWEATING( "sweating", "", new String[] { "symptom", "patient", "diagnosis", "sweating" } ),
    SYMPTOM( "symptom", "", new String[] { "symptoms" } ),
    SYNERGIST_INSECTICIDE_BIOASSAYS( "synergist_insecticide_bioassays", "Synergist insecticide bioassays",
        new String[] { "chemical", "assay", "analysis", "malaria", "mosquito" } ),
    SYRINGE( "syringe", "", new String[] { "syringe", "injectable", "vaccination" } ),
    TAC( "tac", "Computed axial tomography", new String[] {} ),
    TB( "tb", "TB program", new String[] {} ),
    TRANSGENDER( "transgender", "", new String[] { "transgender" } ),
    TRAUMATISM( "traumatism", "", new String[] { "patient", "traumatism" } ),
    TRAVEL( "travel", "", new String[] { "travel" } ),
    TREATED_WATER( "treated_water", "", new String[] { "treated", "infrastructure", "water" } ),
    TRIANGLE_LARGE( "triangle_large", "", new String[] {} ),
    TRIANGLE_MEDIUM( "triangle_medium", "", new String[] {} ),
    TRIANGLE_SMALL( "triangle_small", "", new String[] {} ),
    TRUCK_DRIVER( "truck_driver", "", new String[] { "driver", "truck driver", "worker" } ),
    UN_PAVE_ROAD( "un_pave_road", "", new String[] { "pave", "road" } ),
    UNDERWEIGHT( "underweight", "", new String[] { "symptom", "patient", "diagnosis", "underweight" } ),
    VESPA_MOTORCYCLE( "vespa_motorcycle", "", new String[] { "motorcycle", "transport" } ),
    VIH_AIDS( "vih_aids", "VIH/AIDS", new String[] { "hematology", "vih", "STI", "aids", "blood" } ),
    VIRUS( "virus", "Coronavirus", new String[] { "covid", "19", "corona", "virus" } ),
    VOMITING( "vomiting", "", new String[] { "threw-up", "sickness", "ailment", "vomiting" } ),
    WAR( "war", "Conflict", new String[] { "battle", "armed", "war", "tank", "conflict" } ),
    WASH_HANDS( "wash_hands", "Wash hands", new String[] { "cleaning", "hands", "wash", "water", "soap" } ),
    WATER_SANITATION( "water_sanitation", "", new String[] { "sanitation", "water" } ),
    WATER_TREATMENT( "water_treatment", "", new String[] { "water treatment", "water" } ),
    WEIGHT( "weight", "", new String[] { "weight" } ),
    WOLD_CARE( "wold_care", "World care", new String[] { "world care", "nature", "earth", "protection" } ),
    WOMAN( "woman", "", new String[] { "woman", "female", "girl" } ),
    YES( "yes", "", new String[] { "yes" } ),
    YOUNG_PEOPLE( "young_people", "Young people", new String[] { "young", "juvenile", "teens", "youth" } );

    private static final String[] VARIANTS = { "positive", "negative", "outline" };

    public static final String SUFFIX = "svg";

    private IconData data;

    Icon( String key, String description, String[] keywords )
    {
        this.data = new IconData( key, description, keywords );
    }

    public static Optional<Icon> getIcon( String name )
    {
        for ( Icon icon : Icon.values() )
        {
            if ( icon.getKey().equals( name ) )
            {
                return Optional.of( icon );
            }
        }

        return Optional.empty();
    }

    public String getKey()
    {
        return data.getKey();
    }

    public String getDescription()
    {
        return data.getDescription();
    }

    public String[] getKeywords()
    {
        return data.getKeywords();
    }

    public IconData getData()
    {
        return this.data;
    }

    public Collection<IconData> getVariants()
    {
        return Arrays.stream( VARIANTS )
            .map( variant -> new IconData( String.format( "%s_%s", getKey(), variant ), getDescription(),
                getKeywords() ) )
            .collect( Collectors.toList() );
    }
}

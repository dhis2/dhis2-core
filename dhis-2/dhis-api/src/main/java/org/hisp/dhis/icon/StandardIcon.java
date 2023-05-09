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
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class StandardIcon implements Icon
{
    private final String key;

    private final String description;

    private final List<String> keywords;

    public StandardIcon( String key, String description, List<String> keywords )
    {
        this.key = key;
        this.description = description;
        this.keywords = keywords;
    }

    public enum Icon
    {
        _2G( "2g", "", Collections.emptyList() ),
        _3G( "3g", "", Collections.emptyList() ),
        _4X4( "4x4", "", Collections.emptyList() ),
        AGRICULTURE( "agriculture", "", List.of( "agriculture" ) ),
        AGRICULTURE_WORKER( "agriculture_worker", "", List.of( "agriculture", "worker" ) ),
        ALERT( "alert", "", Collections.emptyList() ),
        ALERT_CIRCLE( "alert_circle", "", Collections.emptyList() ),
        ALERT_TRIANGLE( "alert_triangle", "", Collections.emptyList() ),
        AMBULANCE( "ambulance", "", List.of( "ambulance", "transport" ) ),
        AMBULATORY_CLINIC( "ambulatory_clinic", "", List.of( "refer", "health", "ambulatory" ) ),
        ANCV( "ancv", "ANC visit", Collections.emptyList() ),
        BABY_FEMALE_0203M( "baby_female_0203m", "", List.of( "baby", "2-3 months", "male", "pediatric" ) ),
        BABY_FEMALE_0306M( "baby_female_0306m", "", List.of( "3-6 months", "baby", "male", "pediatric" ) ),
        BABY_FEMALE_0609M( "baby_female_0609m", "", List.of( "baby", "male", "6-9 months", "pediatric" ) ),
        BABY_MALE_0203M( "baby_male_0203m", "", List.of( "baby", "2-3 months", "female", "pediatric" ) ),
        BABY_MALE_0306M( "baby_male_0306m", "", List.of( "3-6 months", "baby", "female", "pediatric" ) ),
        BABY_MALE_0609M( "baby_male_0609m", "", List.of( "baby", "female", "6-9 months", "pediatric" ) ),
        BASIC_MOTORCYCLE( "basic_motorcycle", "", List.of( "motorcycle", "transport" ) ),
        BIKE( "bike", "", List.of( "transport", "bike" ) ),
        BILLS( "bills", "Bills", List.of( "money", "buck", "bank notes", "bills", "currency" ) ),
        BLISTER_PILLS_OVAL_X1( "blister_pills_oval_x1", "", List.of( "treatment", "pills", "1", "blister", "oval" ) ),
        BLISTER_PILLS_OVAL_X14( "blister_pills_oval_x14", "",
            List.of( "treatment", "pills", "14", "blister", "oval" ) ),
        BLISTER_PILLS_OVAL_X16( "blister_pills_oval_x16", "",
            List.of( "treatment", "pills", "16", "blister", "oval" ) ),
        BLISTER_PILLS_OVAL_X4( "blister_pills_oval_x4", "", List.of( "treatment", "pills", "4", "blister", "oval" ) ),
        BLISTER_PILLS_ROUND_X1( "blister_pills_round_x1", "",
            List.of( "treatment", "pills", "1", "round", "blister" ) ),
        BLISTER_PILLS_ROUND_X14( "blister_pills_round_x14", "",
            List.of( "treatment", "pills", "14", "round", "blister" ) ),
        BLISTER_PILLS_ROUND_X16( "blister_pills_round_x16", "",
            List.of( "treatment", "pills", "round", "16", "blister" ) ),
        BLISTER_PILLS_ROUND_X4( "blister_pills_round_x4", "",
            List.of( "treatment", "pills", "round", "4", "blister" ) ),
        BLOOD_A_N( "blood_a_n", "", List.of( "a", "hematology", "negative", "blood" ) ),
        BLOOD_A_P( "blood_a_p", "", List.of( "a", "hematology", "positive", "blood" ) ),
        BLOOD_AB_N( "blood_ab_n", "", List.of( "ab", "hematology", "negative", "blood" ) ),
        BLOOD_AB_P( "blood_ab_p", "", List.of( "ab", "hematology", "positive", "blood" ) ),
        BLOOD_B_N( "blood_b_n", "", List.of( "hematology", "b", "negative", "blood" ) ),
        BLOOD_B_P( "blood_b_p", "", List.of( "hematology", "b", "positive", "blood" ) ),
        BLOOD_O_N( "blood_o_n", "", List.of( "hematology", "negative", "blood", "o" ) ),
        BLOOD_O_P( "blood_o_p", "", List.of( "hematology", "positive", "blood", "o" ) ),
        BLOOD_PRESSURE( "blood_pressure", "", List.of( "Blood Pressure" ) ),
        BLOOD_PRESSURE_2( "blood_pressure_2", "", List.of( "pressure", "blood" ) ),
        BLOOD_PRESSURE_MONITOR( "blood_pressure_monitor", "", List.of( "pressure", "blood" ) ),
        BLOOD_RH_N( "blood_rh_n", "", List.of( "hematology", "negative", "rh", "blood" ) ),
        BLOOD_RH_P( "blood_rh_p", "", List.of( "hematology", "rh", "positive", "blood" ) ),
        BOY_0105Y( "boy_0105y", "", List.of( "1-5 Yrs", "boy" ) ),
        BOY_1015Y( "boy_1015y", "", List.of( "boy", "10-15 Yrs" ) ),
        BREEDING_SITES( "breeding_sites", "Breeding sites",
            List.of( "pupa", "larva", "malaria", "mosquito", "reproduction" ) ),
        CALENDAR( "calendar", "", List.of( "calendar" ) ),
        CARDIOGRAM( "cardiogram", "Cardiogram card", List.of( "medical history" ) ),
        CARDIOGRAM_E( "cardiogram_e", "Edit ardiogram card", List.of( "Cardiogram card", "cardiogram" ) ),
        CERVICAL_CANCER( "cervical_cancer", "", List.of( "cervical", "cancer", "female" ) ),
        CHILD_CARE( "child_care", "Children", List.of( "toddler", "kid", "baby", "child", "care" ) ),
        CHILD_PROGRAM( "child_program", "child program", Collections.emptyList() ),
        CHILLS( "chills", "", List.of( "symptom", "chills", "patient", "diagnosis" ) ),
        CHOLERA( "cholera", "", List.of( "vibrio cholerae", "cholera" ) ),
        CHURCH( "church", "", Collections.emptyList() ),
        CIRCLE_LARGE( "circle_large", "", Collections.emptyList() ),
        CIRCLE_MEDIUM( "circle_medium", "", Collections.emptyList() ),
        CIRCLE_SMALL( "circle_small", "", Collections.emptyList() ),
        CITY( "city", "City", List.of( "city", "house", "building", "edifice", "architecture" ) ),
        CITY_WORKER( "city_worker", "", List.of( "city worker", "worker" ) ),
        CLEAN_HANDS( "clean_hands", "Clean hands", List.of( "washing", "hands", "sanitizer", "clean", "soap" ) ),
        CLINICAL_A( "clinical_a", "Clinical analysis", Collections.emptyList() ),
        CLINICAL_F( "clinical_f", "Clinical file",
            List.of( "Clinical file", "medical history", "diagnosis test", "patient record" ) ),
        CLINICAL_FE( "clinical_fe", "Edit clinical file", List.of( "medical history", "diagnosis test" ) ),
        COINS( "coins", "Money", List.of( "wealth", "money", "payment", "cash" ) ),
        COLD_CHAIN( "cold_chain", "", List.of( "cold chain", "cold" ) ),
        COMMUNICATION( "communication", "", List.of( "contact", "communication" ) ),
        CONE_TEST_ON_NETS( "cone_test_on_nets", "Cone test on nets",
            List.of( "test", "nets", "malaria", "analysis", "cone" ) ),
        CONE_TEST_ON_WALLS( "cone_test_on_walls", "Cone test on walls",
            List.of( "test", "malaria", "analysis", "cone", "wall" ) ),
        CONSTRUCTION( "construction", "", List.of( "infrastructure", "construction", "worker" ) ),
        CONSTRUCTION_WORKER( "construction_worker", "", List.of( "construction", "worker" ) ),
        CONTACT_SUPPORT( "contact_support", "Contact support", List.of( "question", "contact", "support", "info" ) ),
        CONTRACEPTIVE_DIAPHRAGM( "contraceptive_diaphragm", "",
            List.of( "female", "contraception", "sexual", "reproductive" ) ),
        CONTRACEPTIVE_INJECTION( "contraceptive_injection", "",
            List.of( "inyectable", "female", "contraception", "sexual", "reproductive" ) ),
        CONTRACEPTIVE_PATCH( "contraceptive_patch", "",
            List.of( "female", "contraception", "sexual", "reproductive" ) ),
        CONTRACEPTIVE_VOUCHER( "contraceptive_voucher", "contraceptive voucher", Collections.emptyList() ),
        COPPER_IUD( "copper_iud", "", List.of( "copper iud", "copper", "contraception", "sexual", "reproductive" ) ),
        COUGHING( "coughing", "", List.of( "symptom", "coughing", "diagnosis", "cough" ) ),
        CREDIT_CARD( "credit_card", "Credit card", List.of( "bank", "money", "credit", "debit", "card" ) ),
        CROSS_COUNTRY_MOTORCYCLE( "cross_country_motorcycle", "", List.of( "motorcycle", "transport" ) ),
        DEFAULT( "default", "", List.of( "default" ) ),
        DHIS2_LOGO( "dhis2_logo", "DHIS2", List.of( "dhis2", "logo", "DHIS" ) ),
        DIARRHEA( "diarrhea", "", List.of( "symptom", "patient", "diagnosis", "diarrhea" ) ),
        DISCRIMINATING_CONCENTRATION_BIOASSAYS( "discriminating_concentration_bioassays",
            "Discriminating concentration bioassays",
            List.of( "assay", "concentration", "analysis", "malaria", "mosquito" ) ),
        DOCTOR( "doctor", "Doctor", List.of( "doctor", "medical person", "nurse", "health", "worker" ) ),
        DOMESTIC_WORKER( "domestic_worker", "", List.of( "domestic", "domestic worker" ) ),
        DONKEY( "donkey", "", List.of( "donkey", "transport" ) ),
        DRONE( "drone", "", Collections.emptyList() ),
        ECO_CARE( "eco_care", "Eco care", List.of( "eco", "world", "nature", "bio", "care" ) ),
        ELDERLY( "elderly", "Elderly", List.of( "old", "person", "aged", "aging", "people" ) ),
        ELECTRICITY( "electricity", "", List.of( "electricity" ) ),
        EMERGENCY_POST( "emergency_post", "", List.of( "emergency", "emergency post" ) ),
        EXPECTORATE( "expectorate", "", List.of( "symptom", "diagnosis", "expectorate" ) ),
        FACTORY_WORKER( "factory_worker", "", List.of( "factory", "worker" ) ),
        FAMILY_PLANNING( "family_planning", "",
            List.of( "family planning", "contraception", "sexual", "reproductive" ) ),
        FEMALE_AND_MALE( "female_and_male", "Female and Male adult", Collections.emptyList() ),
        FEMALE_CONDOM( "female_condom", "",
            List.of( "female", "contraception", "sexual", "reproductive", "condom" ) ),
        FEMALE_SEX_WORKER( "female_sex_worker", "", List.of( "female", "sex worker" ) ),
        FETUS( "fetus", "", List.of( "baby", "not born" ) ),
        FEVER( "fever", "", List.of( "symptom", "fever", "diagnosis" ) ),
        FEVER_2( "fever_2", "", List.of( "symptom", "fever", "diagnosis" ) ),
        FEVER_CHILLS( "fever_chills", "", List.of( "symptom", "chills", "patient", "fever", "diagnosis" ) ),
        FOREST( "forest", "", List.of( "forest" ) ),
        FOREST_PERSONS( "forest_persons", "", List.of( "persons", "forest" ) ),
        FORUM( "forum", "Forum", List.of( "forum", "chat", "discussion", "conversation" ) ),
        GIRL_0105Y( "girl_0105y", "", List.of( "1-5 Yrs", "girl" ) ),
        GIRL_1015Y( "girl_1015y", "", List.of( "girl", "10-15 Yrs" ) ),
        GROUP_DISCUSSION_MEETING( "group_discussion_meeting", "",
            List.of( "contact", "communication", "meeting", "group" ) ),
        GROUP_DISCUSSION_MEETINGX3( "group_discussion_meetingx3", "",
            List.of( "contact", "communication", "meeting", "group" ) ),
        HAPPY( "happy", "", List.of( "face" ) ),
        HAZARDOUS( "hazardous", "Hazardous", List.of( "hazardous", "difficult", "unsafe", "risky" ) ),
        HEADACHE( "headache", "", List.of( "symptom", "patient", "diagnosis", "headache" ) ),
        HEALTH_WORKER( "health_worker", "Health worker", List.of( "doctor", "nurse", "health", "worker" ) ),
        HEALTH_WORKER_FORM( "health_worker_form", "Health worker form",
            List.of( "quiz", "questionnaire", "form", "health worker" ) ),
        HEART( "heart", "Heart", List.of( "love", "core", "favorite", "heart" ) ),
        HEART_CARDIOGRAM( "heart_cardiogram", "Heart - cardiogram", List.of( "electrocardiogram", "heart" ) ),
        HELICOPTER( "helicopter", "", Collections.emptyList() ),
        HIGH_BARS( "high_bars", "High bars", List.of( "big", "high", "great", "bars" ) ),
        HIGH_LEVEL( "high_level", "High level", List.of( "big", "high", "level", "great" ) ),
        HIV_IND( "hiv_ind", "", List.of( "HIV", "indeterminate", "STI" ) ),
        HIV_NEG( "hiv_neg", "", List.of( "negative", "HIV", "STI" ) ),
        HIV_POS( "hiv_pos", "", List.of( "HIV", "STI", "positive" ) ),
        HIV_SELF_TEST( "hiv_self_test", "", List.of( "HIV", "test", "STI" ) ),
        HOME( "home", "Home", List.of( "cabin", "place", "house", "apartment", "home" ) ),
        HORMONAL_RING( "hormonal_ring", "",
            List.of( "sterilization", "contraception", "sexual", "reproductive", "hormonal ring" ) ),
        HOSPITAL( "hospital", "", List.of( "refer", "hospital" ) ),
        HOSPITALIZED( "hospitalized", "", List.of( "hospitalized", "patient" ) ),
        HOT_MEAL( "hot_meal", "", List.of( "hot meal", "food" ) ),
        HPV( "hpv", "",
            List.of( "Human papillomavirus", "papillomavirus", "STI", "diagnosis", "Sexual Transmitted" ) ),
        I_CERTIFICATE_PAPER( "i_certificate_paper", "Certification",
            List.of( "qualification", "education", "credential", "certificate", "diploma" ) ),
        I_DOCUMENTS_ACCEPTED( "i_documents_accepted", "Documents accepted",
            List.of( "documents", "accepted", "check", "done" ) ),
        I_DOCUMENTS_DENIED( "i_documents_denied", "Documents denied",
            List.of( "refuse", "bad", "documents", "denied" ) ),
        I_EXAM_MULTIPLE_CHOICE( "i_exam_multiple_choice", "Test",
            List.of( "exam", "quiz", "test", "multiple choice" ) ),
        I_EXAM_QUALIFICATION( "i_exam_qualification", "Exam qualification",
            List.of( "exam", "qualification", "notes", "test" ) ),
        I_GROUPS_PERSPECTIVE_CROWD( "i_groups_perspective_crowd", "Crowd",
            List.of( "persons", "crowd", "people", "group" ) ),
        I_NOTE_ACTION( "i_note_action", "Homework",
            List.of( "notes", "homework", "assignment", "document", "write" ) ),
        I_SCHEDULE_SCHOOL_DATE_TIME( "i_schedule_school_date_time", "Schedule",
            List.of( "date", "schedule", "school", "time" ) ),
        I_TRAINING_CLASS( "i_training_class", "Training", List.of( "teach", "training", "class", "board" ) ),
        I_UTENSILS( "i_utensils", "School utensils", List.of( "education", "school", "pen", "ruler", "utensils" ) ),
        IMM( "imm", "Inpatient morbidity and mortality", Collections.emptyList() ),
        IMPLANT( "implant", "", List.of( "pellet", "implant", "contraception", "sexual", "reproductive" ) ),
        INFO( "info", "Info", List.of( "advice", "instruction", "information", "info" ) ),
        INFORMATION_CAMPAIGN( "information_campaign", "information campaign", Collections.emptyList() ),
        INPATIENT( "inpatient", "", List.of( "patient", "inpatient" ) ),
        INSECTICIDE_RESISTANCE( "insecticide_resistance", "Insecticide resistance",
            List.of( "insecticide", "malaria", "resistance", "mosquito" ) ),
        INTENSITY_CONCENTRATION_BIOASSAYS( "intensity_concentration_bioassays", "Intensity concentration bioassays",
            List.of( "assay", "concentration", "analysis", "malaria", "mosquito" ) ),
        IUD( "iud", "", List.of( "iud", "contraception", "sexual", "reproductive" ) ),
        JUSTICE( "justice", "", List.of( "justice" ) ),
        LACTATION( "lactation", "", List.of( "ANC", "baby", "child", "pediatric" ) ),
        LETRINA( "letrina", "", List.of( "letrina" ) ),
        LLIN( "llin", "", List.of( "llin", "malaria", "net" ) ),
        LOW_BARS( "low_bars", "Low bars", List.of( "small", "low", "poor", "bars" ) ),
        LOW_LEVEL( "low_level", "Low level", List.of( "small", "low", "level", "poor" ) ),
        MACHINERY( "machinery", "Machinery", List.of( "excavator", "engine", "machinery", "vehicle" ) ),
        MAGNIFYING_GLASS( "magnifying_glass", "", List.of( "magnifying glass" ) ),
        MALARIA_MIXED_MICROSCOPE( "malaria_mixed_microscope", "", Collections.emptyList() ),
        MALARIA_NEGATIVE_MICROSCOPE( "malaria_negative_microscope", "", Collections.emptyList() ),
        MALARIA_OUTBREAK( "malaria_outbreak", "malaria outbreak",
            List.of( "midge", "outbreak", "denge", "malaria" ) ),
        MALARIA_PF_MICROSCOPE( "malaria_pf_microscope", "", Collections.emptyList() ),
        MALARIA_PV_MICROSCOPE( "malaria_pv_microscope", "", Collections.emptyList() ),
        MALARIA_TESTING( "malaria_testing", "malaria testing", Collections.emptyList() ),
        MALE_AND_FEMALE( "male_and_female", "Male and Female icon", List.of( "man and female" ) ),
        MALE_CONDOM( "male_condom", "", List.of( "copper IUD", "contraception", "sexual", "reproductive" ) ),
        MALE_SEX_WORKER( "male_sex_worker", "", List.of( "sex worker", "male" ) ),
        MAN( "man", "", List.of( "man", "boy", "male" ) ),
        MARKET_STALL( "market_stall", "", List.of( "market" ) ),
        MASK( "mask", "Face mask", List.of( "face mask", "ffp", "virus", "mask" ) ),
        MEASLES( "measles", "", List.of( "symptom", "patient", "diagnosis", "measles" ) ),
        MEDICINES( "medicines", "", List.of( "treatment", "medicines" ) ),
        MEDIUM_BARS( "medium_bars", "Medium bars", List.of( "average", "normal", "common", "medium", "bars" ) ),
        MEDIUM_LEVEL( "medium_level", "Medium level", List.of( "average", "normal", "common", "level", "medium" ) ),
        MEGAPHONE( "megaphone", "", List.of( "megaphone", "communication" ) ),
        MENTAL_DISORDERS( "mental_disorders", "mental_disorders", List.of( "mental health", "mental disorder" ) ),
        MICROSCOPE( "microscope", "Optical Microscope", List.of( "laboratory", "lab", "analysis", "microscope" ) ),
        MILITARY_WORKER( "military_worker", "", List.of( "army", "military", "worker" ) ),
        MINER_WORKER( "miner_worker", "", List.of( "miner worker", "worker", "miner" ) ),
        MOBILE_CLINIC( "mobile_clinic", "", List.of( "refer", "health", "mobile clinic", "ambulance", "transport" ) ),
        MONEY_BAG( "money_bag", "Money bag", List.of( "money", "bag", "currency", "cash", "dollar" ) ),
        MOSQUITO( "mosquito", "", List.of( "denge", "malaria", "mosquito" ) ),
        MOSQUITO_COLLECTION( "mosquito_collection", "Mosquito collection",
            List.of( "collection", "gather", "malaria", "case", "mosquito" ) ),
        MSM( "msm", "", List.of( "msm" ) ),
        NAUSEA( "nausea", "", List.of( "symptom", "patient", "diagnosis", "nausea" ) ),
        NEGATIVE( "negative", "", List.of( "negative" ) ),
        NETWORK_4G( "network_4g", "", List.of( "4g", "transfer", "connectivity", "net" ) ),
        NETWORK_5G( "network_5g", "", List.of( "5g", "transfer", "connectivity", "net" ) ),
        NEUROLOGY( "neurology", "", List.of( "mental health", "meningitis" ) ),
        NEUTRAL( "neutral", "", List.of( "face" ) ),
        NO( "no", "", List.of( "no" ) ),
        NOT_OK( "not_ok", "", List.of( "face" ) ),
        NURSE( "nurse", "Nurse", List.of( "doctor", "assistant", "nurse", "health", "medic" ) ),
        OBSERVATION( "observation", "", List.of( "observation", "patient" ) ),
        ODONTOLOGY( "odontology", "", List.of( "odontology" ) ),
        ODONTOLOGY_IMPLANT( "odontology_implant", "", List.of( "odontology", "implant" ) ),
        OFFICER( "officer", "Officer", List.of( "agent", "police", "cop", "officer" ) ),
        OK( "ok", "", List.of( "face" ) ),
        OLD_MAN( "old_man", "Old man", List.of( "old", "elderly", "aged", "man" ) ),
        OLD_WOMAN( "old_woman", "Old woman", List.of( "woman", "old", "elderly", "aged" ) ),
        ORAL_CONTRACEPTION_PILLSX21( "oral_contraception_pillsx21", "",
            List.of( "treatment", "female", "contraception", "sexual", "reproductive" ) ),
        ORAL_CONTRACEPTION_PILLSX28( "oral_contraception_pillsx28", "",
            List.of( "treatment", "female", "contraception", "sexual", "reproductive" ) ),
        OUTPATIENT( "outpatient", "", List.of( "patient", "outpatient" ) ),
        OVERWEIGHT( "overweight", "", List.of( "symptom", "patient", "diagnosis", "overweight" ) ),
        PALM_BRANCHES_ROOF( "palm_branches_roof", "", List.of( "roof", "laton", "zinc" ) ),
        PAVE_ROAD( "pave_road", "", List.of( "pave", "road" ) ),
        PEACE( "peace", "Peace", List.of( "love", "truce", "peace", "accord" ) ),
        PEOPLE( "people", "People", List.of( "crowd", "person", "community", "people", "group" ) ),
        PERSON( "person", "Person", List.of( "woman", "person", "man", "people" ) ),
        PHONE( "phone", "", List.of( "phone", "contact", "communication" ) ),
        PILL_1( "pill_1", "", List.of( "pills 1", "One pill" ) ),
        PILLS_2( "pills_2", "", List.of( "pills", "two" ) ),
        PILLS_3( "pills_3", "", List.of( "pills", "three" ) ),
        PILLS_4( "pills_4", "", List.of( "pills", "four" ) ),
        PLANTATION_WORKER( "plantation_worker", "", List.of( "plantation", "worker" ) ),
        POLYGON( "polygon", "", List.of( "polygon" ) ),
        POSITIVE( "positive", "", List.of( "positive" ) ),
        PREGNANT( "pregnant", "ANC", List.of( "ANC", "maternity", "pregnant" ) ),
        PREGNANT_0812W( "pregnant_0812w", "ANC",
            List.of( "First visit 8-12 weeks", "ANC", "maternity", "pregnant" ) ),
        PREGNANT_2426W( "pregnant_2426w", "ANC",
            List.of( "Second visit 24-26 weeks", "ANC", "maternity", "pregnant" ) ),
        PREGNANT_32W( "pregnant_32w", "ANC", List.of( "ANC", "maternity", "Third visit 32 weeks", "pregnant" ) ),
        PREGNANT_3638W( "pregnant_3638w", "ANC",
            List.of( "ANC", "maternity", "Fourth visit 36-38 weeks", "pregnant" ) ),
        PRISONER( "prisoner", "Prisoner", List.of( "detainee", "prisoner", "captive", "justice", "convict" ) ),
        PROPER_ROOF( "proper_roof", "", List.of( "roof", "proper" ) ),
        PROVIDER_FST( "provider_fst", "Provider Follow-up and Support Tool", Collections.emptyList() ),
        PWID( "pwid", "", List.of( "pwid" ) ),
        QUESTION( "question", "", Collections.emptyList() ),
        QUESTION_CIRCLE( "question_circle", "", Collections.emptyList() ),
        QUESTION_TRIANGLE( "question_triangle", "", Collections.emptyList() ),
        RDT_RESULT_INVALID( "rdt_result_invalid", "", List.of( "result", "rdt", "invalid", "diagnosis" ) ),
        RDT_RESULT_MIXED( "rdt_result_mixed", "", Collections.emptyList() ),
        RDT_RESULT_MIXED_INVALID( "rdt_result_mixed_invalid", "", List.of( "result", "rdt", "pv", "diagnosis" ) ),
        RDT_RESULT_MIXED_INVALID_RECTANGULAR( "rdt_result_mixed_invalid_rectangular", "",
            List.of( "result", "rdt", "invalid", "diagnosis", "mixed" ) ),
        RDT_RESULT_MIXED_RECTANGULAR( "rdt_result_mixed_rectangular", "",
            List.of( "result", "rdt", "diagnosis", "mixed" ) ),
        RDT_RESULT_NEG( "rdt_result_neg", "", Collections.emptyList() ),
        RDT_RESULT_NEG_INVALID( "rdt_result_neg_invalid", "",
            List.of( "result", "neg", "rdt", "invalid", "diagnosis" ) ),
        RDT_RESULT_NEG_INVALID_RECTANGULAR( "rdt_result_neg_invalid_rectangular", "",
            List.of( "result", "neg", "rdt", "invalid", "diagnosis" ) ),
        RDT_RESULT_NEG_RECTANGULAR( "rdt_result_neg_rectangular", "",
            List.of( "result", "neg", "rdt", "diagnosis" ) ),
        RDT_RESULT_NEGATIVE( "rdt_result_negative", "", List.of( "result", "negative", "rdt", "diagnosis" ) ),
        RDT_RESULT_NO_TEST( "rdt_result_no_test", "", Collections.emptyList() ),
        RDT_RESULT_OUT_STOCK( "rdt_result_out_stock", "", List.of( "result", "out sock", "rdt" ) ),
        RDT_RESULT_PF( "rdt_result_pf", "", Collections.emptyList() ),
        RDT_RESULT_PF_INVALID( "rdt_result_pf_invalid", "",
            List.of( "result", "rdt", "pf", "invalid", "diagnosis" ) ),
        RDT_RESULT_PF_INVALID_RECTANGULAR( "rdt_result_pf_invalid_rectangular", "",
            List.of( "result", "rdt", "pf", "invalid", "diagnosis" ) ),
        RDT_RESULT_PF_RECTANGULAR( "rdt_result_pf_rectangular", "", List.of( "result", "rdt", "pf", "diagnosis" ) ),
        RDT_RESULT_POSITIVE( "rdt_result_positive", "", List.of( "result", "rdt", "diagnosis", "positive" ) ),
        RDT_RESULT_PV( "rdt_result_pv", "", Collections.emptyList() ),
        RDT_RESULT_PV_INVALID( "rdt_result_pv_invalid", "",
            List.of( "result", "rdt", "pv", "invalid", "diagnosis" ) ),
        RDT_RESULT_PV_INVALID_RECTANGULAR( "rdt_result_pv_invalid_rectangular", "",
            List.of( "result", "rdt", "pv", "invalid", "diagnosis" ) ),
        RDT_RESULT_PV_RECTANGULAR( "rdt_result_pv_rectangular", "", List.of( "result", "rdt", "pv", "diagnosis" ) ),
        REFERRAL( "referral", "", List.of( "referral", "patient", "health" ) ),
        REFUSED( "refused", "", List.of( "treatment", "refused" ) ),
        RIBBON( "ribbon", "", List.of( "ribbon", "STI" ) ),
        RMNH( "rmnh", "WHO RMNCH Tracker", Collections.emptyList() ),
        RUNNING_WATER( "running_water", "", List.of( "running water", "water" ) ),
        RURAL_POST( "rural_post", "", List.of( "refer", "rural", "health", "clinic" ) ),
        SAD( "sad", "", List.of( "face" ) ),
        SANITIZER( "sanitizer", "Sanitizer", List.of( "hands", "sanitizer", "cleaner", "soap" ) ),
        SAYANA_PRESS( "sayana_press", "",
            List.of( "inyectable", "female", "contraception", "sexual", "reproductive" ) ),
        SECURITY_WORKER( "security_worker", "", List.of( "worker", "cecurity" ) ),
        SEXUAL_REPRODUCTIVE_HEALTH( "sexual_reproductive_health", "",
            List.of( "contraception", "sexual", "reproductive" ) ),
        SMALL_PLANE( "small_plane", "", Collections.emptyList() ),
        SOCIAL_DISTANCING( "social_distancing", "Social distancing",
            List.of( "distance", "social", "separation", "distancing" ) ),
        SPRAYING( "spraying", "", List.of( "spraying" ) ),
        SQUARE_LARGE( "square_large", "", Collections.emptyList() ),
        SQUARE_MEDIUM( "square_medium", "", Collections.emptyList() ),
        SQUARE_SMALL( "square_small", "", Collections.emptyList() ),
        STAR_LARGE( "star_large", "", Collections.emptyList() ),
        STAR_MEDIUM( "star_medium", "", Collections.emptyList() ),
        STAR_SMALL( "star_small", "", Collections.emptyList() ),
        STETHOSCOPE( "stethoscope", "", List.of( "stethoscope" ) ),
        STI( "sti", "", List.of( "symptom", "Trichomoniasis", "patient", "STI", "diagnosis" ) ),
        STOCK_OUT( "stock_out", "", List.of( "stock", "status" ) ),
        STOP( "stop", "", List.of( "stop" ) ),
        SURGICAL_STERILIZATION( "surgical_sterilization", "",
            List.of( "circusicion", "sterilization", "contraception", "sexual", "reproductive" ) ),
        SWEATING( "sweating", "", List.of( "symptom", "patient", "diagnosis", "sweating" ) ),
        SYMPTOM( "symptom", "", List.of( "symptoms" ) ),
        SYNERGIST_INSECTICIDE_BIOASSAYS( "synergist_insecticide_bioassays", "Synergist insecticide bioassays",
            List.of( "chemical", "assay", "analysis", "malaria", "mosquito" ) ),
        SYRINGE( "syringe", "", List.of( "syringe", "injectable", "vaccination" ) ),
        TAC( "tac", "Computed axial tomography", Collections.emptyList() ),
        TB( "tb", "TB program", Collections.emptyList() ),
        TRANSGENDER( "transgender", "", List.of( "transgender" ) ),
        TRAUMATISM( "traumatism", "", List.of( "patient", "traumatism" ) ),
        TRAVEL( "travel", "", List.of( "travel" ) ),
        TREATED_WATER( "treated_water", "", List.of( "treated", "infrastructure", "water" ) ),
        TRIANGLE_LARGE( "triangle_large", "", Collections.emptyList() ),
        TRIANGLE_MEDIUM( "triangle_medium", "", Collections.emptyList() ),
        TRIANGLE_SMALL( "triangle_small", "", Collections.emptyList() ),
        TRUCK_DRIVER( "truck_driver", "", List.of( "driver", "truck driver", "worker" ) ),
        UN_PAVE_ROAD( "un_pave_road", "", List.of( "pave", "road" ) ),
        UNDERWEIGHT( "underweight", "", List.of( "symptom", "patient", "diagnosis", "underweight" ) ),
        VESPA_MOTORCYCLE( "vespa_motorcycle", "", List.of( "motorcycle", "transport" ) ),
        VIH_AIDS( "vih_aids", "VIH/AIDS", List.of( "hematology", "vih", "STI", "aids", "blood" ) ),
        VIRUS( "virus", "Coronavirus", List.of( "covid", "19", "corona", "virus" ) ),
        VOMITING( "vomiting", "", List.of( "threw-up", "sickness", "ailment", "vomiting" ) ),
        WAR( "war", "Conflict", List.of( "battle", "armed", "war", "tank", "conflict" ) ),
        WASH_HANDS( "wash_hands", "Wash hands", List.of( "cleaning", "hands", "wash", "water", "soap" ) ),
        WATER_SANITATION( "water_sanitation", "", List.of( "sanitation", "water" ) ),
        WATER_TREATMENT( "water_treatment", "", List.of( "water treatment", "water" ) ),
        WEIGHT( "weight", "", List.of( "weight" ) ),
        WOLD_CARE( "wold_care", "World care", List.of( "world care", "nature", "earth", "protection" ) ),
        WOMAN( "woman", "", List.of( "woman", "female", "girl" ) ),
        YES( "yes", "", List.of( "yes" ) ),
        YOUNG_PEOPLE( "young_people", "Young people", List.of( "young", "juvenile", "teens", "youth" ) );

        private static final String[] VARIANTS = { "positive", "negative", "outline" };

        public static final String SUFFIX = "svg";

        private final StandardIcon standardIcon;

        Icon( String key, String description, List<String> keywords )
        {
            this.standardIcon = new StandardIcon( key, description, keywords );
        }

        public String getKey()
        {
            return standardIcon.getKey();
        }

        public String getDescription()
        {
            return standardIcon.getDescription();
        }

        public List<String> getKeywords()
        {
            return standardIcon.getKeywords();
        }

        public Collection<StandardIcon> getVariants()
        {
            return Arrays.stream( VARIANTS )
                .map( variant -> new StandardIcon( String.format( "%s_%s", getKey(), variant ), getDescription(),
                    getKeywords() ) )
                .toList();
        }
    }
}

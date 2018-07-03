package org.hisp.dhis.icon;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.net.MediaType;

/**
 * @author Kristian Wærstad
 */
public enum Icon
{
    MALE_AND_FEMALE("male_and_female", "Male and Female icon", new String[]{"man and female"}),
    FEMALE_AND_MALE("female_and_male", "Female and Male adult", new String[]{}),
    MAN("man", "", new String[]{"male", "boy", "man"}),
    WOMAN("woman", "", new String[]{"female", "girl", "woman"}),
    SYRINGE("syringe", "", new String[]{"syringe", "vaccination", "injectable"}),
    MICROSCOPE("microscope", "Optical Microscope", new String[]{"microscope", "laboratory", "lab", "analysis"}),
    TAC("tac", "Computed axialtomography", new String[]{}),
    NEUROLOGY("neurology", "", new String[]{"mental health", "meningitis"}),
    MENTAL_DISORDERS("mental_disorders", "mental_disorders", new String[]{"mental health", "mental disorder"}),
    CLINICAL_A("clinical_a", "Clinical analysis", new String[]{}),
    CLINICAL_F("clinical_f", "Clinical file", new String[]{"medical history", "diagnosis test", "Clinical file", "patient record"}),
    CLINICAL_FE("clinical_fe", "Edit clinical file", new String[]{"medical history", "diagnosis test"}),
    CARDIOGRAM("cardiogram", "Cardiogram card", new String[]{"medical history"}),
    CARDIOGRAM_E("cardiogram_e", "Edit ardiogram card", new String[]{"cardiogram", "Cardiogram card"}),
    HEART_CARDIOGRAM("heart_cardiogram", "Heart - cardiogram", new String[]{"heart", "electrocardiogram"}),
    BLOOD_PRESSURE("blood_pressure", "", new String[]{"Blood Pressure"}),
    PREGNANT("pregnant", "ANC", new String[]{"pregnant", "ANC", "maternity"}),
    PREGNANT_0812W("pregnant_0812w", "ANC", new String[]{"pregnant", "ANC", "maternity", "First visit 8-12 weeks"}),
    PREGNANT_426W("pregnant_2426w", "ANC", new String[]{"pregnant", "ANC", "maternity", "Second visit 24-26 weeks"}),
    PREGNANT_32W("pregnant_32w", "ANC", new String[]{"pregnant", "ANC", "maternity", "Third visit 32 weeks"}),
    PREGNANT_3638W("pregnant_3638w", "ANC", new String[]{"pregnant", "ANC", "maternity", "Fourth visit 36-38 weeks"}),
    BABY_MALE_0203M("baby_male_0203m", "", new String[]{"baby", "female", "2-3 months", "pediatric"}),
    BABY_MALE_0306M("baby_male_0306m", "", new String[]{"baby", "female", "3-6 months", "pediatric"}),
    BABY_MALE_0609M("baby_male_0609m", "", new String[]{"baby", "female", "6-9 months", "pediatric"}),
    BABY_FEMALE_0203M("baby_female_0203m", "", new String[]{"baby", "male", "2-3 months", "pediatric"}),
    BABY_FEMALE_0306M("baby_female_0306m", "", new String[]{"baby", "male", "3-6 months", "pediatric"}),
    BABY_FEMALE_0609M("baby_female_0609m", "", new String[]{"baby", "male", "6-9 months", "pediatric"}),
    BOY_0105Y("boy_0105y", "", new String[]{"boy", "1-5 Yrs"}),
    BOY_0510Y("boy_0510y", "", new String[]{"boy", "5-10 Yrs"}),
    BOY_1015Y("boy_1015y", "", new String[]{"boy", "10-15 Yrs"}),
    BOY_1525Y("boy_1525y", "", new String[]{"boy", "15-25 Yrs"}),
    GIRL_0105Y("girl_0105y", "", new String[]{"girl", "1-5 Yrs"}),
    GIRL_0510Y("girl_0510y", "", new String[]{"girl", "5-10 Yrs"}),
    GIRL_1015Y("girl_1015y", "", new String[]{"girl", "10-15 Yrs"}),
    GIRL_1525Y("girl_1525y", "", new String[]{"girl", "15-25 Yrs"}),
    FETUS("fetus", "", new String[]{"not born", "baby"}),
    LACTATION("lactation", "", new String[]{"baby", "ANC", "child", "pediatric"}),
    FEVER("fever", "", new String[]{"fever"}),
    LLIN("llin", "", new String[]{"llin", "malaria", "net"}),
    PILL_1("pill_1", "", new String[]{"One pill", "pills, 1"}),
    PILLS_2("pills_2", "", new String[]{"pills", "two"}),
    PILLS_3("pills_3", "", new String[]{"pills", "three"}),
    PILLS_4("pills_4", "", new String[]{"pills", "four"}),
    MEDICINES("medicines", "", new String[]{"treatment", "medicines"}),
    MOSQUITO("mosquito", "", new String[]{"malaria", "mosquito", "denge"}),
    CHOLERA("cholera", "", new String[]{"cholera", "vibrio cholerae"}),
    MALARIA_OUTBREAK("malaria_outbreak", "malaria outbreak", new String[]{"outbreak", "malaria", "midge", "denge"}),
    DIARRHEA("diarrhea", "", new String[]{"diarrhea"}),
    VOMITING("vomiting", "", new String[]{"sickness", "ailment", "threw-up", "vomiting"}),
    HOT_MEAL("hot_meal", "", new String[]{"food", "hot meal"}),
    WATER_TREATMENT("water_treatment", "", new String[]{"water", "water treatment"}),
    SPRAYING("spraying", "", new String[]{"spraying"}),
    LETRINA("letrina", "", new String[]{"letrina"}),
    ODONTOLOGY("odontology", "", new String[]{"odontology"}),
    ODONTOLOGY_IMPLANT("odontology_implant", "", new String[]{"implant", "odontology"}),
    MALE_CONDOM("male_condom", "", new String[]{"copper IUD", "contraception", "reproductive", "sexual"}),
    FEMALE_CONDOM("female_condom", "", new String[]{"female", "contraception", "reproductive", "sexual", "condom"}),
    COPPER_IUD("copper_iud", "", new String[]{"copper iud", "contraception", "reproductive", "sexual", "copper"}),
    IUD("iud", "", new String[]{"iud", "contraception", "reproductive", "sexual", "iud"}),
    IMPLANT("implant", "", new String[]{"implant", "contraception", "reproductive", "sexual", "pellet"}),
    SURGICAL_STERILIZATION("surgical_sterilization", "", new String[]{"contraception", "reproductive", "sexual", "sterilization", "circusicion"}),
    HORMONAL_RING("hormonal_ring", "", new String[]{"contraception", "reproductive", "sexual", "sterilization", "hormonal ring"}),
    CONTRACEPTIVE_INJECTION("contraceptive_injection", "", new String[]{"female", "contraception", "reproductive", "sexual", "inyectable"}),
    CONTRACEPTIVE_PATCH("contraceptive_patch", "", new String[]{"female", "contraception", "reproductive", "sexual"}),
    CONTRACEPTIVE_DIAPHRAGM("contraceptive_diaphragm", "", new String[]{"female", "contraception", "reproductive", "sexual"}),
    SAYANA_PRESS("sayana_press", "", new String[]{"female", "contraception", "reproductive", "sexual", "inyectable"}),
    CIRCLE_LARGE("circle_large", "", new String[]{}),
    CIRCLE_MEDIUM("circle_medium", "", new String[]{}),
    CIRCLE_SMALL("circle_small", "", new String[]{}),
    TRIANGLE_LARGE("triangle_large", "", new String[]{}),
    TRIANGLE_MEDIUM("triangle_medium", "", new String[]{}),
    TRIANGLE_SMALL("triangle_small", "", new String[]{}),
    SQUARE_LARGE("square_large", "", new String[]{}),
    SQUARE_MEDIUM("square_medium", "", new String[]{}),
    SQUARE_SMALL("square_small", "", new String[]{}),
    STAR_LARGE("star_large", "", new String[]{}),
    STAR_MEDIUM("star_medium", "", new String[]{}),
    STAR_SMALL("star_small", "", new String[]{}),
    AMILY_PLANNING("family_planning", "", new String[]{"family planning", "contraception", "reproductive", "sexual"}),
    SEXUAL_REPRODUCTIVE_HEALTH("sexual_reproductive_health", "", new String[]{"sexual", "contraception", "sexual", "reproductive"}),
    CERVICAL_CANCER("cervical_cancer", "", new String[]{"cancer", "cervical", "female"}),
    FEMALE_SEX_WORKER("female_sex_worker", "", new String[]{"female", "sex worker"}),
    MALE_SEX_WORKER("male_sex_worker", "", new String[]{"male", "sex worker"}),
    MSM("msm", "", new String[]{"msm"}),
    PWID("pwid", "", new String[]{"pwid"}),
    TRANSGENDER("transgender", "", new String[]{"transgender"}),
    TRUCK_DRIVER("truck_driver", "", new String[]{"truck driver", "driver", "worker"}),
    PLANTATION_WORKER("plantation_worker", "", new String[]{"plantation worker", "plantation", "worker"}),
    MINER_WORKER("miner_worker", "", new String[]{"miner worker", "miner", "worker"}),
    SECURITY_WORKER("security_worker", "", new String[]{"security worker", "security", "worker"}),
    MILITARY_WORKER("military_worker", "", new String[]{"army", "military", "worker"}),
    FACTORY_WORKER("factory_worker", "", new String[]{"factory", "worker"}),
    CONSTRUCTION_WORKER("construction_worker", "", new String[]{"worker", "construction"}),
    DOMESTIC_WORKER("domestic_worker", "", new String[]{"domestic worker", "domestic"}),
    PRISIONER("prisioner", "", new String[]{"prisioner"}),
    AGRICULTURE_WORKER("agriculture_worker", "", new String[]{"agriculture worker", "worker"}),
    CITY_WORKER("city_worker", "", new String[]{"city worker", "worker"}),
    HIV_SELF_TEST("hiv_self_test", "", new String[]{"HIV", "test", "STI"}),
    HIV_POS("hiv_pos", "", new String[]{"HIV", "positive", "STI"}),
    HIV_NEG("hiv_neg", "", new String[]{"HIV", "negative", "STI"}),
    HIV_IND("hiv_ind", "", new String[]{"HIV", "indeterminate", "STI"}),
    RIBBON("ribbon", "", new String[]{"ribbon", "STI"}),
    VIH_AIDS("vih_aids", "VIH/AIDS", new String[]{"hematology", "blood", "vih", "aids", "STI"}),
    SYMPTOM("symptom", "", new String[]{"symptoms"}),
    ALERT_TRIANGLE("alert_triangle", "", new String[]{}),
    ALERT_CIRCLE("alert_circle", "", new String[]{}),
    ALERT("alert", "", new String[]{}),
    QUESTION_TRIANGLE("question_triangle", "", new String[]{}),
    QUESTION_CIRCLE("question_circle", "", new String[]{}),
    QUESTION("question", "", new String[]{}),
    RDT_RESULT_MIXED("rdt_result_mixed", "", new String[]{}),
    RDT_RESULT_NEG("rdt_result_neg", "", new String[]{}),
    RDT_RESULT_PV("rdt_result_pv", "", new String[]{}),
    RDT_RESULT_PF("rdt_result_pf", "", new String[]{}),
    RDT_RESULT_NO_TEST("rdt_result_no_test", "", new String[]{}),
    RDT_RESULT_OUT_STOCK("rdt_result_out_stock", "", new String[]{}),
    HAPPY("happy", "", new String[]{}),
    OK("ok", "", new String[]{}),
    NEUTRAL("neutral", "", new String[]{}),
    NOT_OK("not_ok", "", new String[]{}),
    SAD("sad", "", new String[]{}),
    MALARIA_NEGATIVE_MICROSCOPE("malaria_negative_microscope", "", new String[]{}),
    MALARIA_MIXED_MICROSCOPE("malaria_mixed_microscope", "", new String[]{}),
    MALARIA_PF_MICROSCOPE("malaria_pf_microscope", "", new String[]{}),
    MALARIA_PV_MICROSCOPE("malaria_pv_microscope", "", new String[]{}),
    ANCV("ancv", "ANC visit", new String[]{}),
    CHILD_PROGRAM("child_program", "child program", new String[]{}),
    CONTRACEPTIVE_VOUCHER("contraceptive_voucher", "contraceptive voucher", new String[]{}),
    IMM("imm", "Inpatient morbidity and mortality", new String[]{}),
    INFORMATION_CAMPAIGN("information_campaign", "information campaign", new String[]{}),
    MALARIA_TESTING("malaria_testing", "malaria testing", new String[]{}),
    PROVIDER_FST("provider_fst", "Provider Follow-up and Support Tool", new String[]{}),
    RMNH("rmnh", "WHO RMNCH Tracker", new String[]{}),
    TB("tb", "TB program", new String[]{});

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
            .map( variant -> new IconData( String.format( "%s_%s", getKey(), variant ), getDescription(), getKeywords() ) )
            .collect( Collectors.toList() );
    }
}

/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A subset of a BCP47 conform locale that only supports language, region and script.
 *
 * @param language 2-3 lower case characters, e.g. {@code en}
 * @param region 2 upper case characters, e.g. {@code US} (or null if undefined)
 * @param script upper case letter followed by 3 lower case letters, e.g. {@code Latn} (or null if
 *     undefined)
 * @author Jan Bernitt
 * @since 2.43
 */
public record Locale(
    @Nonnull String language, @CheckForNull String region, @CheckForNull String script)
    implements Serializable {

  /**
   * Parses and creates a locale from a BCP47 compatible or Java-format string.
   *
   * @param locale BCP47 or Java format (if variant is present it is intentionally "misunderstood"
   *     as script for backwards compatability)
   * @return the parsed value
   * @throws IllegalArgumentException in case the input is not a valid locale string
   */
  @Nonnull
  public static Locale of(@Nonnull String locale) throws IllegalArgumentException {
    int len = locale.length();
    // is it just ll or lll
    if (len == 2 || len == 3) return new Locale(locale, null, null);
    // is it ll-LL or ll_LL?
    if (len == 5 && isDash(locale.charAt(2)))
      return new Locale(locale.substring(0, 2), locale.substring(3, 5), null);
    String[] parts = locale.split("-|_#|_"); // order matters!
    if (parts.length < 2 || parts.length > 3)
      throw new IllegalArgumentException("Invalid locale: " + locale);
    if (parts.length == 2) return new Locale(parts[0], parts[1]);
    if (isScript(parts[1])) return new Locale(parts[0], parts[2], parts[1]);
    return new Locale(parts[0], parts[1], parts[2]);
  }

  @JsonCreator
  @CheckForNull
  public static Locale ofNullable(@CheckForNull String locale) throws IllegalArgumentException {
    return locale == null ? null : of(locale);
  }

  @Nonnull
  public static Locale of(@Nonnull java.util.Locale locale) {
    String lang = locale.getLanguage();
    String region = locale.getCountry();
    String script = locale.getScript();
    if (region.isEmpty()) region = null;
    if (script.isEmpty()) script = null;
    return new Locale(lang, region, script);
  }

  @CheckForNull
  public static Locale ofNullable(@CheckForNull java.util.Locale locale) {
    return locale == null ? null : of(locale);
  }

  public static Locale getDefault() {
    return of(java.util.Locale.getDefault());
  }

  public Locale(@Nonnull String language, @CheckForNull String region) {
    this(language, region, null);
  }

  public Locale(@Nonnull String language) {
    this(language, null, null);
  }

  public Locale {
    // map outdated codes
    language =
        switch (language) {
          // 2-letter deprecated codes
          case "iw" -> "he";
          case "ji" -> "yi";
          case "in" -> "id";
          // 3-letter bibliographic codes → terminological
          case "alb" -> "sqi";
          case "arm" -> "hye";
          case "baq" -> "eus";
          case "bur" -> "mya";
          case "chi" -> "zho";
          case "cze" -> "ces";
          case "dut" -> "nld";
          case "fre" -> "fra";
          case "geo" -> "kat";
          case "ger" -> "deu";
          case "gre" -> "ell";
          case "ice" -> "isl";
          case "mac" -> "mkd";
          case "mao" -> "mri";
          case "may" -> "msa";
          case "per" -> "fas";
          case "rum" -> "ron";
          case "slo" -> "slk";
          case "tib" -> "bod";
          case "wel" -> "cym";
          default -> language;
        };
    if (!isLanguageCode(language))
      throw new IllegalArgumentException("Invalid language code: " + language);
    if (!isRegionCode(region)) throw new IllegalArgumentException("Invalid region code: " + region);
    if (!isScript(script)) throw new IllegalArgumentException("Invalid script: " + script);
    if (script != null && region == null)
      throw new IllegalArgumentException("Script must be used with region");
  }

  private static boolean isLanguageCode(String code) {
    if (code == null) return false;
    int len = code.length();
    if (len < 2 || len > 3 || !code.chars().allMatch(Locale::isLowerCaseLetter)) return false;
    if (len == 2) return LANGUAGES.contains(code);
    return LANGUAGES_3.contains(code);
  }

  private static boolean isRegionCode(String code) {
    if (code == null) return true;
    if (code.length() != 2 || !code.chars().allMatch(Locale::isUpperCaseLetter)) return false;
    return REGIONS.contains(code);
  }

  private static boolean isScript(String code) {
    if (code == null) return true;
    if (code.length() != 4
        || !isUpperCaseLetter(code.charAt(0))
        || !code.chars().skip(1).allMatch(Locale::isLowerCaseLetter)) return false;
    return SCRIPTS.contains(code);
  }

  private static boolean isLowerCaseLetter(int c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isUpperCaseLetter(int c) {
    return c >= 'A' && c <= 'Z';
  }

  private static boolean isDash(int c) {
    return c == '-' || c == '_';
  }

  public String toLanguageTag() {
    if (region == null && script == null) return language;
    if (script == null) return language + "-" + region;
    return language + "-" + script + "-" + region;
  }

  @Override
  @JsonValue
  public String toString() {
    if (region == null && script == null) return language;
    if (script == null) return language + "_" + region;
    return language + "_" + region + "_" + script;
  }

  @Nonnull
  public java.util.Locale toJavaLocale() {
    if (region == null && script == null) return new java.util.Locale(language);
    if (script == null) return new java.util.Locale(language, region);
    return new java.util.Locale.Builder()
        .setLanguage(language)
        .setRegion(region)
        .setScript(script)
        .build();
  }

  /*
  Display stuff (just cached resolve of the Java util code)
   */

  private static final Map<String, String> DISPLAY_NAMES = new ConcurrentHashMap<>();
  private static final Map<String, String> DISPLAY_LANGUAGES = new ConcurrentHashMap<>();
  private static final Map<String, String> DISPLAY_REGIONS = new ConcurrentHashMap<>();

  public String getDisplayName(Locale in) {
    return DISPLAY_NAMES.computeIfAbsent(
        this + " in " + in, k -> toJavaLocale().getDisplayName(in.toJavaLocale()));
  }

  public String getDisplayName() {
    return DISPLAY_NAMES.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayName());
  }

  public String getDisplayLanguage() {
    return DISPLAY_LANGUAGES.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayLanguage());
  }

  public String getDisplayRegion() {
    return DISPLAY_REGIONS.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayCountry());
  }

  /*
  Code validation
   */

  /** Language: ISO 639-1 (2-letter codes only, ~180 entries) */
  private static final Set<String> LANGUAGES =
      parseCodes(
          """
        aa ab ae af ak am an ar as av ay az ba be bg bh bi bm bn bo br bs
        ca ce ch co cr cs cu cv cy da de dv dz ee el en eo es et eu fa ff
        fi fj fo fr fy ga gd gl gn gu gv ha he hi ho hr ht hu hy hz ia id
        ie ig ii ik io is it iu ja jv ka kg ki kj kk kl km kn ko kr ks ku
        kv kw ky la lb lg li ln lo lt lu lv mg mh mi mk ml mn mr ms mt my
        na nb nd ne ng nl nn no nr nv ny oc oj om or os pa pi pl ps pt qu
        rm rn ro ru rw sa sc sd se sg si sk sl sm sn so sq sr ss st su sv
        sw ta te tg th ti tk tl tn to tr ts tt tw ty ug uk ur uz ve vi vo
        wa wo xh yi yo za zh zu
        """);

  /** Region: ISO 3166-1 alpha-2 (~250 entries) */
  private static final Set<String> REGIONS =
      parseCodes(
          """
        AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG
        BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ CA CC CD CF CG CH CI
        CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH
        ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ
        GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT
        JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS
        LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU
        MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG
        PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG
        SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK
        TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU
        WF WS YE YT ZA ZM ZW
        """);

  /** Script: ISO 15924 (~200 entries) */
  private static final Set<String> SCRIPTS =
      parseCodes(
          """
        Adlm Afak Aghb Ahom Arab Aran Armi Armn Avst Bali Bamu Bass Batk
        Beng Bhks Blis Bopo Brah Brai Bugi Buhd Cakm Cans Cari Cham Cher
        Chrs Cirt Copt Cpmn Cprt Cyrl Cyrs Deva Diak Dogr Dsrt Dupl Egyd
        Egyh Egyp Elba Elym Ethi Geok Geor Glag Gong Gonm Goth Gran Grek
        Gujr Guru Hanb Hang Hani Hano Hans Hant Hatr Hebr Hira Hluw Hmng
        Hmnp Hrkt Hung Inds Ital Jamo Java Jpan Jurc Kali Kana Khar Khmr
        Khoj Kitl Kits Knda Kore Kpel Kthi Lana Laoo Latf Latg Latn Leke
        Lepc Limb Lina Linb Lisu Loma Lyci Lydi Mahj Maka Mand Mani Marc
        Maya Medf Mend Merc Mero Mlym Modi Mong Moon Mroo Mtei Mult Mymr
        Nand Narb Nbat Newa Nkdb Nkgb Nkoo Nshu Ogam Olck Orkh Orya Osge
        Osma Palm Pauc Perm Phag Phli Phlp Phlv Phnx Piqd Plrd Prti Qaaa
        Qaab Qaac Qaad Qaae Qaaf Qaag Qaah Qaai Qaaj Qaak Qaal Qaam Qaan
        Qaao Qaap Qaaq Qaar Qaas Qaat Qaau Qaav Qaaw Qaax Qaay Qaaz Qaba
        Qabb Qabc Qabd Qabe Qabf Qabg Qabh Qabi Qabj Qabk Qabl Qabm Qabn
        Qabo Qabp Qabq Qabr Qabs Qabt Qabu Qabv Qabw Qabx Rjng Rohg Roro
        Runr Samr Sara Sarb Saur Sgnw Shaw Shrd Shui Sidd Sind Sinh Sogd
        Sogo Sora Soyo Sund Sylo Syrc Syre Syrj Syrn Tagb Takr Tale Talu
        Taml Tang Tavt Telu Teng Tfng Tglg Thaa Thai Tibt Tirh Toto Ugar
        Vaii Visp Wara Wcho Wole Xpeo Xsux Yezi Yiii Zanb Zinh Zmth Zsye
        Zsym Zxxx Zzzz
        """);

  /**
   * ISO 639-2 alpha-3 language codes (terminological/preferred forms).
   *
   * <p>Includes every language that has a three-letter code, whether a two-letter ISO 639-1
   * equivalent exists or not; (e.g. {@code ast} Asturian, {@code csb} Kashubian, {@code haw}
   * Hawaiian, {@code yue} Cantonese).
   *
   * <p>For the 20 languages that have distinct bibliographic and terminological codes, only the
   * terminological form is listed here (e.g. {@code sqi}, not {@code alb}). The bibliographic forms
   * are normalized before validation, in the same way deprecated two-letter codes are normalized.
   *
   * <p>The private-use range {@code qaa..qtz} is intentionally excluded.
   */
  private static final Set<String> LANGUAGES_3 =
      parseCodes(
          """
        aar abk ace ach ada ady afa afh afr ain aka akk sqi ale alg alt
        amh ang anp apa ara arc arg hye arn arp art arw asm ast ath aus
        ava ave awa aym aze bad bai bak bal bam ban eus bas bat bej bel
        bem ben ber bho bih bik bin bis bla bnt bos bra bre btk bua bug
        bul mya byn cad cai car cat cau ceb cel cha chb che chg zho chk
        chm chn cho chp chr chu chv chy cmc cnr cop cor cos cpe cpf cpp
        cre crh crp csb cus ces dak dan dar day del den dgr din div doi
        dra dsb dua dum nld dyu dzo efi egy eka elx eng enm epo est ewe
        ewo fan fao fat fij fil fin fiu fon fra frm fro frr frs fry ful
        fur gaa gay gba gem kat deu gez gil gla gle glg glv gmh goh gon
        gor got grb grc ell grn gsw guj gwi hai hat hau haw heb her hil
        him hin hit hmn hmo hrv hsb hun hup iba ibo isl ido iii ijo iku
        ile ilo ina inc ind ine inh ipk ira iro ita jav jbo jpn jpr jrb
        kaa kab kac kal kam kan kar kas kau kaw kaz kbd kha khi khm kho
        kik kin kir kmb kok kom kon kor kos kpe krc krl kro kru kua kum
        kur kut lad lah lam lao lat lav lez lim lin lit lol loz ltz lua
        lub lug lui lun luo lus mkd mad mag mah mai mak mal man mri map
        mar mas msa mdf mdr men mga mic min mis mkh mlg mlt mnc mni mno
        moh mon mos mul mun mus mwl mwr myn myv nah nai nap nau nav nbl
        nde ndo nds nep new nia nic niu nno nob nog non nor nqo nso nub
        nwc nya nym nyn nyo nzi oci oji ori orm osa oss ota oto paa pag
        pal pam pan pap pau peo fas phi phn pli pol pon por pra pro pus
        que raj rap rar roa roh rom ron run rup rus sad sag sah sai sal
        sam san sas sat scn sco sel sem sga sgn shn sid sin sio sit sla
        slk slv sma sme smi smj smn smo sms sna snd snk sog som son sot
        spa srd srn srp srr ssa ssw suk sun sus sux swa swe syc syr tah
        tai tam tat tel tem ter tet tgk tgl tha bod tig tir tiv tkl tlh
        tli tmh tog ton tpi tsi tsn tso tuk tum tup tur tut tvl twi tyv
        udm uga uig ukr umb und urd uzb vai ven vie vol vot wak wal war
        was cym wen wln wol xal xho yao yap yid yor ypk zap zbl zen zgh
        zha znd zul zun zxx zza
        """);

  // needs to be defined after the code blocks as they depend upon them being initialized
  public static final Locale //
      ENGLISH = new Locale("en", null, null),
      FRENCH = new Locale("fr", null, null),
      US = new Locale("en", "US", null);

  private static Set<String> parseCodes(String codes) {
    return Set.copyOf(Stream.of(codes.strip().split("\\s+")).toList());
  }
}

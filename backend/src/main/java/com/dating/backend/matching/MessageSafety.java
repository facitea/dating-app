package com.dating.backend.matching;
import java.text.Normalizer;import java.util.*;import java.util.regex.Pattern;
public final class MessageSafety {
 private MessageSafety(){}
 private static final Pattern URL=Pattern.compile("(?iu)(?:https?\\s*[:：]\\s*[/／]{2}|www\\s*[.．]|(?:[\\p{L}\\p{N}-]+\\.)+(?:com|net|org|io|co|kr|me|app|xyz|site|online|link|top|vip|shop|info|cc|ly)(?:\\b|/)|(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?|(?:카톡|카카오톡|텔레그램|라인)\\s*(?:으로|아이디|id|링크))");
 public static List<String> flags(String text){if(text==null)return List.of();String t=Normalizer.normalize(text,Normalizer.Form.NFKC).replaceAll("[\\p{Cf}]","").toLowerCase(Locale.ROOT);var out=new ArrayList<String>();if(URL.matcher(t).find())out.add("EXTERNAL_LINK");if(List.of("송금","입금","계좌","돈 보내","돈을 보내","투자","고수익","가상자산","가상화폐","암호화폐","코인","비트코인","환전","crypto","bitcoin","usdt").stream().anyMatch(t::contains))out.add("FINANCIAL_RISK");return out;}
 public static String warning(String text){return flags(text).isEmpty()?null:"송금·투자 권유나 외부 사이트 이동에 주의하세요. 돈, 인증번호, 계정 정보를 전달하지 마세요. 의심되면 신고해 주세요.";}
}

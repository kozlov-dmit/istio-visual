package io.github.istiorouteexplorer.model.istio;

import lombok.Data;

@Data
public class StringMatchDto {
    public enum MatchType {
        PREFIX,
        EXACT,
        REGEX;
    }
    private String prefix;
    private String exact;
    private String regex;
    private MatchType matchType;

    public boolean isPrefixMatch() {
        return matchType == MatchType.PREFIX;
    }

    public boolean isExactMatch() {
        return matchType == MatchType.EXACT;
    }

    public boolean isRegexMatch() {
        return matchType == MatchType.REGEX;
    }

    public static StringMatchDto fromPrefix(String prefix) {
        if (prefix != null) {
            StringMatchDto stringMatchDto = new StringMatchDto();
            stringMatchDto.setPrefix(prefix);
            stringMatchDto.setMatchType(MatchType.PREFIX);
            return stringMatchDto;
        } else {
            return null;
        }
    }

    public static StringMatchDto fromExact(String exact) {
        if (exact != null) {
            StringMatchDto stringMatchDto = new StringMatchDto();
            stringMatchDto.setExact(exact);
            stringMatchDto.setMatchType(MatchType.EXACT);
            return stringMatchDto;
        } else {
            return null;
        }
    }

    public static StringMatchDto fromRegex(String regex) {
        if (regex != null) {
            StringMatchDto stringMatchDto = new StringMatchDto();
            stringMatchDto.setRegex(regex);
            stringMatchDto.setMatchType(MatchType.REGEX);
            return stringMatchDto;
        } else {
            return null;
        }
    }

}

package com.nhnacademy.environment.domain;

public enum KmaCategory {
    POP("강수확률", "%"),
    PTY("강수형태", ""),
    PCP("1시간 강수량", "범주(1 mm)"),
    REH("습도", "%"),
    SNO("1시간 신적설", "범주(1 cm)"),
    SKY("하늘상태", "코드값"),
    TMP("1시간 기온", "℃"),
    TMN("일 최저기온", "℃"),
    TMX("일 최고기온", "℃"),
    T1H("기온", "℃"),
    RN1("1시간 강수량", "mm"),
    UUU("동서바람성분", "m/s"),
    VVV("남북바람성분", "m/s"),
    WAV("파고", "m"),
    VEC("풍향", "deg"),
    WSD("풍속", "m/s"),
    LGT("낙뢰", "kA");

    private final String description;
    private final String unit;

    KmaCategory(String description, String unit) {
        this.description = description;
        this.unit = unit;
    }

    public String description(){
        return description;
    }

    public String unit(){
        return unit;
    }

    public static KmaCategory fromCode(String code) {
        return valueOf(code);
    }

    public String parseValue(String value) {
        return switch (this) {
            case SKY -> {
                int code = Integer.parseInt(value);
                yield switch (code) {
                    case 1 -> "맑음";
                    case 3 -> "구름많음";
                    case 4 -> "흐림";
                    default -> value;
                };
            }

            case PTY -> {
                int code = Integer.parseInt(value);
                yield switch (code) {
                    case 0 -> "없음";
                    case 1 -> "비";
                    case 2 -> "비/눈";
                    case 3 -> "눈";
                    case 4 -> "소나기";
                    case 5 -> "빗방울";
                    case 6 -> "빗방울눈날림";
                    case 7 -> "눈날림";
                    default -> value;
                };
            }

            default -> value;
        };
    }
}

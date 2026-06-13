package com.nolleo.onna.domain.spot.domain.model;

/** [도메인 모델] 관광 장소 이미지 — 조회 전용 데이터 모델. */
public class SpotImage {

    private final Long id;
    private final String contentId;
    private final String originImgUrl;
    private final String smallImgUrl;
    private final String imgName;
    private final String cpyrhtDivCd;
    private final String serialNum;

    public SpotImage(Long id, String contentId, String originImgUrl,
                     String smallImgUrl, String imgName,
                     String cpyrhtDivCd, String serialNum) {
        this.id = id;
        this.contentId = contentId;
        this.originImgUrl = originImgUrl;
        this.smallImgUrl = smallImgUrl;
        this.imgName = imgName;
        this.cpyrhtDivCd = cpyrhtDivCd;
        this.serialNum = serialNum;
    }

    public Long getId()            { return id; }
    public String getContentId()   { return contentId; }
    public String getOriginImgUrl(){ return originImgUrl; }
    public String getSmallImgUrl() { return smallImgUrl; }
    public String getImgName()     { return imgName; }
    public String getCpyrhtDivCd() { return cpyrhtDivCd; }
    public String getSerialNum()   { return serialNum; }
}
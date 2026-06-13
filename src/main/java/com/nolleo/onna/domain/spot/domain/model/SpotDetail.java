package com.nolleo.onna.domain.spot.domain.model;

import java.time.OffsetDateTime;

/** [도메인 모델] 관광 장소 상세 — 조회 전용 데이터 모델. */
public class SpotDetail {

    private final String contentId;
    private final String tel;
    private final String telName;
    private final String homepage;
    private final String addr1;
    private final String addr2;
    private final String zipcode;
    private final String overview;
    private final String overviewHash;
    private final String intro;
    private final Boolean parkingAvailable;
    private final OffsetDateTime sourceCreatedAt;
    private final OffsetDateTime updatedAt;

    public SpotDetail(String contentId, String tel, String telName, String homepage,
                      String addr1, String addr2, String zipcode,
                      String overview, String overviewHash, String intro,
                      Boolean parkingAvailable,
                      OffsetDateTime sourceCreatedAt, OffsetDateTime updatedAt) {
        this.contentId = contentId;
        this.tel = tel;
        this.telName = telName;
        this.homepage = homepage;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.zipcode = zipcode;
        this.overview = overview;
        this.overviewHash = overviewHash;
        this.intro = intro;
        this.parkingAvailable = parkingAvailable;
        this.sourceCreatedAt = sourceCreatedAt;
        this.updatedAt = updatedAt;
    }

    public String getContentId()              { return contentId; }
    public String getTel()                    { return tel; }
    public String getTelName()                { return telName; }
    public String getHomepage()               { return homepage; }
    public String getAddr1()                  { return addr1; }
    public String getAddr2()                  { return addr2; }
    public String getZipcode()                { return zipcode; }
    public String getOverview()               { return overview; }
    public String getOverviewHash()           { return overviewHash; }
    public String getIntro()                  { return intro; }
    public Boolean getParkingAvailable()      { return parkingAvailable; }
    public OffsetDateTime getSourceCreatedAt(){ return sourceCreatedAt; }
    public OffsetDateTime getUpdatedAt()      { return updatedAt; }
}
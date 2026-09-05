package com.nolleo.onna.domain.spot.domain.model;

import com.nolleo.onna.domain.spot.domain.model.vo.GeoCoordinate;
import com.nolleo.onna.domain.spot.domain.model.vo.SpotCategory;

import java.time.OffsetDateTime;

/** [애그리게이트 루트] 관광 장소 — 조회 전용 데이터 모델. */
public class Spot {

    private final String contentId;
    private final String contentTypeId;
    private final String title;
    private final boolean sourceTourApi;
    private final boolean sourceBusanFood;
    private final GeoCoordinate geoCoordinate;
    private final String lDongRegnCd;
    private final String lDongSignguCd;
    private final String lclsSystm1;
    private final String lclsSystm2;
    private final String lclsSystm3;
    private final String firstImage;
    private final String firstImage2;
    private final String firstImageCpyrhtDivCd;
    private final OffsetDateTime sourceModifiedTime;
    private final OffsetDateTime syncedAt;
    private final OffsetDateTime updatedAt;
    private final boolean active;
    private final OffsetDateTime inactiveSince;

    public Spot(String contentId, String contentTypeId, String title,
                boolean sourceTourApi, boolean sourceBusanFood,
                GeoCoordinate geoCoordinate,
                String lDongRegnCd, String lDongSignguCd,
                String lclsSystm1, String lclsSystm2, String lclsSystm3,
                String firstImage, String firstImage2, String firstImageCpyrhtDivCd,
                OffsetDateTime sourceModifiedTime, OffsetDateTime syncedAt,
                OffsetDateTime updatedAt, boolean active, OffsetDateTime inactiveSince) {
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
        this.title = title;
        this.sourceTourApi = sourceTourApi;
        this.sourceBusanFood = sourceBusanFood;
        this.geoCoordinate = geoCoordinate;
        this.lDongRegnCd = lDongRegnCd;
        this.lDongSignguCd = lDongSignguCd;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.firstImage = firstImage;
        this.firstImage2 = firstImage2;
        this.firstImageCpyrhtDivCd = firstImageCpyrhtDivCd;
        this.sourceModifiedTime = sourceModifiedTime;
        this.syncedAt = syncedAt;
        this.updatedAt = updatedAt;
        this.active = active;
        this.inactiveSince = inactiveSince;
    }

    public String getContentId()                  { return contentId; }
    public String getContentTypeId()              { return contentTypeId; }
    public String getTitle()                      { return title; }
    public boolean isSourceTourApi()              { return sourceTourApi; }
    public boolean isSourceBusanFood()            { return sourceBusanFood; }
    public GeoCoordinate getGeoCoordinate()       { return geoCoordinate; }
    public String getLDongRegnCd()                { return lDongRegnCd; }
    public String getLDongSignguCd()              { return lDongSignguCd; }
    public String getLclsSystm1()                 { return lclsSystm1; }
    public String getLclsSystm2()                 { return lclsSystm2; }
    public String getLclsSystm3()                 { return lclsSystm3; }
    public String getFirstImage()                 { return firstImage; }
    public String getFirstImage2()                { return firstImage2; }
    public String getFirstImageCpyrhtDivCd()      { return firstImageCpyrhtDivCd; }
    public OffsetDateTime getSourceModifiedTime() { return sourceModifiedTime; }
    public OffsetDateTime getSyncedAt()           { return syncedAt; }
    public OffsetDateTime getUpdatedAt()          { return updatedAt; }
    public boolean isActive()                     { return active; }
    public OffsetDateTime getInactiveSince()      { return inactiveSince; }

    public SpotCategory getCategory() {
        return SpotCategory.fromLclsSystm1(lclsSystm1);
    }
}
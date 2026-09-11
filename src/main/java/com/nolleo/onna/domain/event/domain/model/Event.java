package com.nolleo.onna.domain.event.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** [애그리게이트 루트] 행사 — 조회 전용 데이터 모델. */
public class Event {

    private final String contentId;
    private final String title;
    private final LocalDate eventStartDate;
    private final LocalDate eventEndDate;
    private final BigDecimal mapX;
    private final BigDecimal mapY;
    private final String lDongRegnCd;
    private final String lDongSignguCd;
    private final String lclsSystm1;
    private final String lclsSystm2;
    private final String lclsSystm3;
    private final String tel;
    private final String addr1;
    private final String addr2;
    private final String firstImage;
    private final String firstImage2;
    private final String firstImageCpyrhtDivCd;
    private final String eventPlace;
    private final String playTime;
    private final String useTimeFestival;
    private final String sponsor1;
    private final String sponsor1Tel;
    private final String sponsor2;
    private final String sponsor2Tel;
    private final String ageLimit;
    private final String eventHomepage;
    private final OffsetDateTime sourceModifiedTime;
    private final OffsetDateTime syncedAt;
    private final OffsetDateTime updatedAt;
    private final boolean active;
    private final OffsetDateTime inactiveSince;

    public Event(String contentId, String title, LocalDate eventStartDate, LocalDate eventEndDate,
                 BigDecimal mapX, BigDecimal mapY,
                 String lDongRegnCd, String lDongSignguCd,
                 String lclsSystm1, String lclsSystm2, String lclsSystm3,
                 String tel, String addr1, String addr2,
                 String firstImage, String firstImage2, String firstImageCpyrhtDivCd,
                 String eventPlace, String playTime, String useTimeFestival,
                 String sponsor1, String sponsor1Tel, String sponsor2, String sponsor2Tel,
                 String ageLimit, String eventHomepage,
                 OffsetDateTime sourceModifiedTime, OffsetDateTime syncedAt,
                 OffsetDateTime updatedAt, boolean active, OffsetDateTime inactiveSince) {
        this.contentId = contentId;
        this.title = title;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.mapX = mapX;
        this.mapY = mapY;
        this.lDongRegnCd = lDongRegnCd;
        this.lDongSignguCd = lDongSignguCd;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.tel = tel;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.firstImage = firstImage;
        this.firstImage2 = firstImage2;
        this.firstImageCpyrhtDivCd = firstImageCpyrhtDivCd;
        this.eventPlace = eventPlace;
        this.playTime = playTime;
        this.useTimeFestival = useTimeFestival;
        this.sponsor1 = sponsor1;
        this.sponsor1Tel = sponsor1Tel;
        this.sponsor2 = sponsor2;
        this.sponsor2Tel = sponsor2Tel;
        this.ageLimit = ageLimit;
        this.eventHomepage = eventHomepage;
        this.sourceModifiedTime = sourceModifiedTime;
        this.syncedAt = syncedAt;
        this.updatedAt = updatedAt;
        this.active = active;
        this.inactiveSince = inactiveSince;
    }

    public String getContentId()                    { return contentId; }
    public String getTitle()                        { return title; }
    public LocalDate getEventStartDate()            { return eventStartDate; }
    public LocalDate getEventEndDate()              { return eventEndDate; }
    public BigDecimal getMapX()                     { return mapX; }
    public BigDecimal getMapY()                     { return mapY; }
    public String getLDongRegnCd()                  { return lDongRegnCd; }
    public String getLDongSignguCd()                { return lDongSignguCd; }
    public String getLclsSystm1()                   { return lclsSystm1; }
    public String getLclsSystm2()                   { return lclsSystm2; }
    public String getLclsSystm3()                   { return lclsSystm3; }
    public String getTel()                          { return tel; }
    public String getAddr1()                        { return addr1; }
    public String getAddr2()                        { return addr2; }
    public String getFirstImage()                   { return firstImage; }
    public String getFirstImage2()                  { return firstImage2; }
    public String getFirstImageCpyrhtDivCd()        { return firstImageCpyrhtDivCd; }
    public String getEventPlace()                   { return eventPlace; }
    public String getPlayTime()                     { return playTime; }
    public String getUseTimeFestival()              { return useTimeFestival; }
    public String getSponsor1()                     { return sponsor1; }
    public String getSponsor1Tel()                  { return sponsor1Tel; }
    public String getSponsor2()                     { return sponsor2; }
    public String getSponsor2Tel()                  { return sponsor2Tel; }
    public String getAgeLimit()                     { return ageLimit; }
    public String getEventHomepage()                { return eventHomepage; }
    public OffsetDateTime getSourceModifiedTime()   { return sourceModifiedTime; }
    public OffsetDateTime getSyncedAt()             { return syncedAt; }
    public OffsetDateTime getUpdatedAt()            { return updatedAt; }
    public boolean isActive()                       { return active; }
    public OffsetDateTime getInactiveSince()        { return inactiveSince; }
}

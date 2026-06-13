package com.nolleo.onna.domain.food.domain.model;

import com.nolleo.onna.domain.food.domain.model.vo.GeoCoordinate;

import java.time.OffsetDateTime;
import java.util.List;

/** [애그리게이트 루트] 음식 장소 — 조회 전용 데이터 모델. */
public class FoodPlace {

    /** 내부 식별자 */
    private final Long id;

    /** 업소명 */
    private final String name;

    /** 원천 데이터의 업종명 */
    private final String businessCategory;

    /** 서비스 표준 업종 분류 */
    private final String normalizedCategory;

    /** 코스 식사 후보 여부 */
    private final boolean courseFoodCandidate;

    /** 업소 주소 */
    private final String address;

    /** 업소 연락처 */
    private final String tel;

    /** 원천 데이터의 영업시간 문자열 */
    private final String businessHoursRaw;

    /** 업소 소개 텍스트 */
    private final String description;

    /** 대표 메뉴 문자열 (원천 raw 값) */
    private final String representativeMenu;

    /** 배달 가능 여부 (null = 정보 없음) */
    private final Boolean deliveryAvailable;

    /** 주차 가능 여부 (null = 정보 없음) */
    private final Boolean parkingAvailable;

    /** 원천 지역/구군명 */
    private final String sourceRegion;

    /** 지리 좌표 (경도 + 위도) */
    private final GeoCoordinate geoCoordinate;

    /** 서비스 노출 활성 여부 */
    private final boolean active;

    /** 비활성 전환 시각 */
    private final OffsetDateTime inactiveSince;

    /** 메뉴 목록 */
    private final List<FoodPlaceMenu> menus;

    public FoodPlace(Long id, String name, String businessCategory, String normalizedCategory,
                     boolean courseFoodCandidate, String address, String tel,
                     String businessHoursRaw, String description, String representativeMenu,
                     Boolean deliveryAvailable, Boolean parkingAvailable, String sourceRegion,
                     GeoCoordinate geoCoordinate, boolean active, OffsetDateTime inactiveSince,
                     List<FoodPlaceMenu> menus) {
        this.id = id;
        this.name = name;
        this.businessCategory = businessCategory;
        this.normalizedCategory = normalizedCategory;
        this.courseFoodCandidate = courseFoodCandidate;
        this.address = address;
        this.tel = tel;
        this.businessHoursRaw = businessHoursRaw;
        this.description = description;
        this.representativeMenu = representativeMenu;
        this.deliveryAvailable = deliveryAvailable;
        this.parkingAvailable = parkingAvailable;
        this.sourceRegion = sourceRegion;
        this.geoCoordinate = geoCoordinate;
        this.active = active;
        this.inactiveSince = inactiveSince;
        this.menus = menus != null ? menus : List.of();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBusinessCategory() { return businessCategory; }
    public String getNormalizedCategory() { return normalizedCategory; }
    public boolean isCourseFoodCandidate() { return courseFoodCandidate; }
    public String getAddress() { return address; }
    public String getTel() { return tel; }
    public String getBusinessHoursRaw() { return businessHoursRaw; }
    public String getDescription() { return description; }
    public String getRepresentativeMenu() { return representativeMenu; }
    public Boolean getDeliveryAvailable() { return deliveryAvailable; }
    public Boolean getParkingAvailable() { return parkingAvailable; }
    public String getSourceRegion() { return sourceRegion; }
    public GeoCoordinate getGeoCoordinate() { return geoCoordinate; }
    public boolean isActive() { return active; }
    public OffsetDateTime getInactiveSince() { return inactiveSince; }
    public List<FoodPlaceMenu> getMenus() { return menus; }
}

package com.nolleo.onna.domain.external.congestion.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionApiClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B551011/TatsCnctrRateService/tatsCnctrRatedList";

    @Value("${external.congestion.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public List<Map<String, String>> fetchCongestion(String areaCd, String signguCd) {
        log.info("혼잡도 API 호출 areaCd={} signguCd={}", areaCd, signguCd);
        // numOfRows=1000: 구당 관광지가 1000개를 넘을 수 없으므로 단일 호출로 전체 수집
        String xml = callApi(areaCd, signguCd, 1, 1000);
        try {
            return parseXml(xml);
        } catch (Exception e) {
            log.error("혼잡도 XML 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("혼잡도 XML 파싱 실패", e);
        }
    }

    private String callApi(String areaCd, String signguCd, int pageNo, int numOfRows) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "nolleo")
                .queryParam("areaCd", areaCd);
        if (signguCd != null && !signguCd.isBlank()) {
            uriBuilder.queryParam("signguCd", signguCd);
        }
        String url = uriBuilder.build(false).toUriString();

        try {
            String body = restTemplate.getForObject(url, String.class);
            return body != null ? body : "";
        } catch (HttpStatusCodeException e) {
            log.error("혼잡도 API HTTP {} 오류 areaCd={} signguCd={} | 응답: {}",
                    e.getStatusCode(), areaCd, signguCd, e.getResponseBodyAsString());
            throw new RuntimeException("혼잡도 API 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("혼잡도 API 호출 실패 areaCd={} signguCd={}: {}", areaCd, signguCd, e.getMessage());
            throw new RuntimeException("혼잡도 API 호출 실패", e);
        }
    }

    private List<Map<String, String>> parseXml(String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList items = doc.getElementsByTagName("item");

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            Map<String, String> map = new HashMap<>();
            map.put("baseYmd",   getTagValue(item, "baseYmd"));
            map.put("areaCd",    getTagValue(item, "areaCd"));
            map.put("areaNm",    getTagValue(item, "areaNm"));
            map.put("signguCd",  getTagValue(item, "signguCd"));
            map.put("signguNm",  getTagValue(item, "signguNm"));
            map.put("tAtsNm",    getTagValue(item, "tAtsNm"));
            map.put("cnctrRate", getTagValue(item, "cnctrRate"));
            result.add(map);
        }
        return result;
    }

    private String getTagValue(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent();
    }
}

## ① Spring Boot 기동 성공 — 로그에 이게 보이면 성공:
Started OnnaApplication in X.XXX seconds

## ② DB + Redis 연결 상태 — 브라우저에서:
http://localhost:8080/actuator/health
정상이면:
```
{
"status": "UP",
"components": {
"db": { "status": "UP" },
"redis": { "status": "UP" }
}
}
```
## ③ Swagger UI — 아직 API는 없지만 페이지 자체가 열리면 성공:
http://localhost:8080/swagger-ui.html

## ④ JPA 테이블 자동 생성 — postgres 컨테이너에 접속해서 확인:
docker exec -it onna-postgres psql -U onna -d onna -c "\dt"
users, spots, generated_courses 등 테이블이 생성되어 있어야 합니다.
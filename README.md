# Bozor (Spring Boot 3 + PostgreSQL + JWT) — backend + sayt

Sayt (src/main/resources/static/index.html) backend bilan birga ishlaydi: http://localhost:8080

## 1. Kerakli dasturlar
JDK 21, Maven 3.9+, Docker Desktop (yoki PostgreSQL 16), IDE (IntelliJ IDEA) ixtiyoriy.

## 2. PostgreSQL
A) Docker (oson): `docker compose up -d db` — baza, foydalanuvchi va parol avtomatik yaratiladi (bozor/bozor/bozor).
B) O'zingizda o'rnatilgan PostgreSQL bo'lsa, psql da:
   CREATE USER bozor WITH PASSWORD 'bozor';
   CREATE DATABASE bozor OWNER bozor;
Jadvallarni qo'lda ochish shart emas: dastur birinchi ishga tushganda o'zi yaratadi.

## 3. Sozlash
OWNER_PHONE (+998XXXXXXXXX) va JWT_SECRET (kamida 32 belgi) ni application.yml yoki muhit o'zgaruvchisida o'zgartiring.

## 4. Ishga tushirish
mvn spring-boot:run     (yoki hammasi Docker'da: docker compose up --build)

## 5. Sayt
Brauzerda http://localhost:8080 -> "Ro'yxatdan o'tish" -> OWNER_PHONE dagi raqam bilan kiring: siz EGA bo'lasiz.

## Frontend
`src/main/resources/static/index.html` backend bilan birga ishlaydi: server ishga tushgach http://localhost:8080 ni oching.

# Ask API

Spring Boot tabanli bu servis, bir soruyu alip kod tabaninda ilgili dosyalari tarar ve kisa bir aciklama ile kaynak dosya adlarini dondurur.

## Icerik

- [Proje Ozeti](#proje-ozeti)
- [Teknoloji Yigini](#teknoloji-yigini)
- [Mimari ve Calisma Mantigi](#mimari-ve-calisma-mantigi)
- [Gereksinimler](#gereksinimler)
- [Kurulum ve Calistirma](#kurulum-ve-calistirma)
- [Konfigurasyon](#konfigurasyon)
- [API Sozlesmesi](#api-sozlesmesi)
- [Ornekler](#ornekler)
- [Hata Yonetimi](#hata-yonetimi)
- [Proje Yapisi](#proje-yapisi)
- [Gelisim Notlari](#gelisim-notlari)

## Proje Ozeti

`ask-api`, bir `question` parametresi alir ve konfige edilen klasorde dosya aramasi yapar.
Sonucta:

- Sorunun kendisini,
- Uretilen bir `answer` metnini,
- Eslesen kaynak dosya adlarini (`sources`)

dondurur.

Servis, ozellikle repo icinde "ilgili dosyalar nerede" turu sorulara hizli cevap vermek icin tasarlanmistir.

## Teknoloji Yigini

- Java 17
- Spring Boot 3.3.4
- Spring Web
- Spring Validation
- Spring Actuator
- Maven

## Mimari ve Calisma Mantigi

Uygulama katmanlari:

1. `AskController`: HTTP isteklerini alir (`GET /api/ask`).
2. `AskService` / `AskServiceImpl`: is mantigini calistirir.
3. `SearchProperties`: dosya arama ayarlarini (`base-directory`, `max-results`) yonetir.
4. `GlobalExceptionHandler`: tum hatalari standart JSON formatinda doner.

Dosya arama akisi (ozet):

1. `question` bos ise `400 Bad Request` doner.
2. Arama kok dizini `ask.search.base-directory` ile belirlenir.
3. Soru icerigine gore birincil anahtar kelime secilir:
   - `mfa` geciyorsa `Mfa`
   - `login` geciyorsa `Login`
4. Birincil anahtar kelime ile dosya aranir.
5. Sonuc yoksa varsayilan anahtar kelimelere geri donulur:
   - `Application`, `Controller`, `Service`
6. Hala sonuc yoksa dizindeki ilk dosyalar (sirali) alinarak fallback yapilir.
7. Cevap olarak `AskResponse(question, answer, sources)` dondurulur.

Notlar:

- Arama buyuk/kucuk harf duyarli degildir.
- Sonuc sayisi `ask.search.max-results` ile sinirlanir.
- `sources` alaninda tam path degil, dosya adlari bulunur.

## Gereksinimler

- JDK 17+
- Maven 3.9+ (onerilir)

## Kurulum ve Calistirma

### 1) Projeyi klonla

```bash
git clone <repo-url>
cd copilotbackend
```

### 2) Uygulamayi gelisim modunda calistir

```bash
mvn spring-boot:run
```

### 3) Jar olustur ve calistir

```bash
mvn clean package
java -jar target/ask-api-0.0.1-SNAPSHOT.jar
```

Varsayilan port: `8080`

## Konfigurasyon

`src/main/resources/application.properties`:

```properties
server.port=8080

ask.search.base-directory=${ASK_SEARCH_BASE_DIRECTORY:.}
ask.search.max-results=${ASK_SEARCH_MAX_RESULTS:20}

logging.level.root=INFO
logging.level.com.copilotbackend.askapi=DEBUG
```

### Ortam degiskenleri

- `ASK_SEARCH_BASE_DIRECTORY`
  - Aciklama: Aramanin yapilacagi kok dizin
  - Varsayilan: `.`
- `ASK_SEARCH_MAX_RESULTS`
  - Aciklama: Maksimum sonuc sayisi
  - Varsayilan: `20`
  - Gecerli aralik: `1..200`

Ornek (PowerShell):

```powershell
$env:ASK_SEARCH_BASE_DIRECTORY = "C:\dev\vscode\copilotbackend\src"
$env:ASK_SEARCH_MAX_RESULTS = "30"
mvn spring-boot:run
```

## API Sozlesmesi

### Endpoint

- Method: `GET`
- Path: `/api/ask`
- Query Param:
  - `question` (zorunlu, bos olamaz)

### Basarili Donus

- Status: `200 OK`
- Body:

```json
{
  "question": "string",
  "answer": "string",
  "sources": ["string"]
}
```

### Hata Donus Modeli

Tum hatalarda ortak format:

```json
{
  "timestamp": "2026-03-19T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "question must not be blank",
  "path": "/api/ask",
  "details": []
}
```

## Ornekler

### 1) Basarili istek

```bash
curl -G "http://localhost:8080/api/ask" --data-urlencode "question=api endpoint nerede"
```

Ornek yanit:

```json
{
  "question": "api endpoint nerede",
  "answer": "API giris noktasi AskController uzerinden /api/ask endpointidir. Incelenen kaynaklar: AskController.java, AskServiceImpl.java, AskApiApplication.java.",
  "sources": [
    "AskController.java",
    "AskServiceImpl.java",
    "AskApiApplication.java"
  ]
}
```

### 2) `question` eksik veya bos

```bash
curl "http://localhost:8080/api/ask"
```

Beklenen: `400 Bad Request`

### 3) Yanlis HTTP methodu

```bash
curl -X POST "http://localhost:8080/api/ask"
```

Beklenen: `405 Method Not Allowed`

### 4) Yanlis endpoint

```bash
curl "http://localhost:8080/api/not-found"
```

Beklenen: `404 Not Found`

## Hata Yonetimi

`GlobalExceptionHandler` asagidaki durumlari merkezi yonetir:

- `IllegalArgumentException` -> `400 Bad Request`
- `MethodArgumentNotValidException` -> `400 Bad Request`
- `HttpMessageNotReadableException` -> `400 Bad Request`
- `HttpRequestMethodNotSupportedException` -> `405 Method Not Allowed`
- `NoResourceFoundException` -> `404 Not Found`
- `InvalidBaseDirectoryException` -> `500 Internal Server Error`
- `FileSearchException` -> `500 Internal Server Error`
- Beklenmeyen tum hatalar -> `500 Internal Server Error`

## Proje Yapisi

```text
src/main/java/com/copilotbackend/askapi
  AskApiApplication.java
  config/
    SearchProperties.java
  controller/
    AskController.java
  dto/
    AskRequest.java
    AskResponse.java
  exception/
    ApiErrorResponse.java
    FileSearchException.java
    GlobalExceptionHandler.java
    InvalidBaseDirectoryException.java
  service/
    AskService.java
    impl/
      AskServiceImpl.java

src/main/resources
  application.properties
```

## Gelisim Notlari

- `AskRequest` su an kod tabaninda tanimli olsa da aktif endpoint `GET /api/ask` oldugu icin request body kullanilmiyor.
- Dosya arama mutlak path uzerinden calisir, fakat API cevabinda yalnizca dosya adlari dondurulur.
- Actuator bagimliligi eklidir; endpoint davranisi Spring Boot varsayilanlarina gore belirlenir.

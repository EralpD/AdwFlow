# AdwFlow — Redis implementasyon raporu

Tarih: 27 Ağustos 2026

## 1. Kapsam ve mevcut durum

Redis, e-posta doğrulama ve şifre sıfırlama otomasyonlarının geçici güvenlik deposu olarak eklendi. Kalıcı veri modeli değişmedi: `app_users`, `history`, `archive` ve Flyway'in teknik takip tablosu korunuyor. Yeni SQL tablosu veya migration yok.

Bu aşama **backend servisleri ve yerel Redis altyapısını** tamamlar. n8n webhook istemcisi, e-posta gönderimi, yeni HTTP endpoint'leri, doğrulama/şifremi unuttum ekranları ve kayıt sonrası otomatik gönderim henüz bağlı değildir. Mevcut login/register davranışı korunur; doğrulanmamış hesapların girişine yeni bir engel konulmadı. Redis, Java bağımlılıklarını yönetmez; geçici çalışma verilerini saklar.

## 2. Eklenen dosyalar ve sorumlulukları

| Dosya | Görev |
| --- | --- |
| `pom.xml` | Spring Boot sürüm yönetimini kullanan `spring-boot-starter-data-redis` ve Jedis istemcisi; Lettuce hariç tutuldu |
| `compose.yaml` | `adwflow-redis` adlı, parola korumalı yerel Redis servisi |
| `scripts/redis.ps1` | Eksik sırları üretme, başlatma, durum, durdurma, hızlı bağlantı kontrolü ve gerçek Redis testleri |
| `security/temporary/RedisSecurityProperties.java` | TTL, deneme/istek sınırları, namespace ve HMAC anahtarı doğrulaması |
| `security/temporary/RedisSecurityConfiguration.java` | Başlangıçta Redis bağlantı kontrolü |
| `security/temporary/SecurityDigests.java` | Güvenli rastgele kod/token üretimi ve HMAC-SHA256 |
| `security/temporary/RedisChallengeStore.java` | Redis üzerinden oluşturma, tüketme, iptal ve istek sınırı işlemleri |
| `security/temporary/IssuedChallenge.java` | Yalnızca backend e-posta adaptörüne verilecek kod/token zarfı |
| `security/temporary/VerifiedAccount.java` | Başarılı tüketim sonrası kullanıcı kimliği ve hesap sürümü |
| `security/temporary/TemporarySecurityUnavailableException.java` | Redis başarısızlığında güvenli, sır içermeyen hata |
| `account/AccountRecoveryService.java` | Redis doğrulamasını mevcut SQL hesap güncellemelerine bağlama |
| `security/CurrentAccountSessionFilter.java` | Hesap sürümü değişmiş oturumları sonraki istekte kapatma |
| `src/main/resources/redis/*.lua` | Atomik oluşturma/tüketme/iptal/istek sayacı betikleri |

Java yolları `src/main/java/com/example/demo/` dizinine göredir. Ayrıca `AccountPrincipal`, `CurrentAccountAccess`, `UserAccountRepository`, `SecurityConfig` ve üretim öncesi/sonrası kullanıcı kontrolü için `SavedWorkGenerationService` güncellendi.

İlk bağlantı denemesinde Lettuce, bu Windows/Java ortamında `ChannelException → IOException → SocketException` ile başarısız oldu. Aynı hosttan TCP `AUTH/PING` başarılıydı ve parolalar eşleşiyordu. Senkron MVC servislerine uygun olan, Spring Boot'un resmi olarak desteklediği Jedis istemcisine geçildi; sistemin JVM/network ayarları kalıcı değiştirilmedi. Bağlantılar Spring üzerinden yönetilen havuzdan alınır; aktif bağlantı sınırı 8 ve havuz bekleme sınırı iki saniyedir.

## 3. Redis'te hangi veriler tutuluyor?

Örnek namespace: `adwflow:security:{auth}:...`

| Anahtar türü | İçerik | Ömür |
| --- | --- | --- |
| `challenge:<rastgele-id>` | Amaç, SQL kullanıcı ID'si, `auth_version`, HMAC özeti ve yanlış deneme sayısı | Doğrulama 10 dakika; sıfırlama 15 dakika |
| `active:<HMAC>` | Kullanıcı ve amaç için son geçerli challenge ID'si | İlgili challenge ile aynı |
| `active:<HMAC>:cooldown` | Yeniden gönderim kilidi | 60 saniye |
| `rate:<HMAC>` | IP veya e-posta için istek sayacı | Gönderim 1 saat; doğrulama 15 dakika |

Her yazılan güvenlik kaydının TTL'si vardır. Kodlar, sıfırlama token'ları, kullanıcı parolaları ve e-posta adresleri Redis'e açık metin olarak kaydedilmez. Challenge kaydındaki kullanıcı ID'si ve sürüm hassasiyet taşıyan metadata'dır; Redis yine erişim kontrolü gerektirir. E-posta/IP değerleri, sayaç anahtarlarında HMAC ile temsil edilir.

`{auth}` etiketi bir Lua işlemindeki anahtarların aynı Redis hash slotuna düşmesini sağlar. Mevcut kurulum standalone'dır; cluster/HA kurulmuş veya test edilmiş değildir. Ortamlar farklı `REDIS_KEY_PREFIX` kullanmalıdır. Aynı uygulamanın tüm instance'ları aynı HMAC sırrını kullanmalıdır.

## 4. Kod ve token üretimi

E-posta kodu `SecureRandom` ile üretilen tam altı rakamdır; baştaki sıfırlar korunur. Tek başına altı rakam düşük entropili olduğu için düz SHA-256 yerine sunucu sırrı ile HMAC-SHA256 kullanılır. Böylece yalnızca Redis dökümüne erişen biri, tüm bir milyon kodu sırrı bilmeden çevrimdışı deneyemez.

Şifre sıfırlama token'ı 32 rastgele byte, yani 256 bit içerir. URL-safe Base64 gösterimi 43 karakterdir. Her iki amaç için ayrıca bağımsız, rastgele 43 karakterlik challenge ID üretilir. HMAC; amaç, challenge ID ve kod/token'a bağlıdır. Bir doğrulama kodu şifre sıfırlama amacıyla kullanılamaz.

`AUTH_HMAC_SECRET`, en az 32 rastgele byte'ın Base64 karşılığı olmalıdır. Eksik veya hatalı biçimli sırla uygulama açılmaz. Kod/token yalnızca oluşturulduğunda `IssuedChallenge` içinde backend'e verilir; sonradan Redis'ten geri okunamaz. Bu nesne ve `Delivery` için `toString()` hassas alanları gizler. Yine de bu nesneler HTTP response'una veya yapılandırılmış loglara serialize edilmemelidir.

## 5. Atomik işlemler nasıl çalışıyor?

### Oluşturma — `issue-challenge.lua`

1. Kullanıcı/amaç için yeniden gönderim bekleme süresi kontrol edilir.
2. Yeni challenge hash'i ve TTL'si yazılır.
3. Aktif challenge işaretçisi yeni ID'ye çevrilir.
4. Yeniden gönderim kilidi oluşturulur.

İşlemler başka bir isteğin araya giremeyeceği tek Lua çalıştırması içindedir. Eşzamanlı yeniden gönderimlerde bir istek kazanır. Önceki challenge'ın metadata'sı TTL'sine kadar kalabilir, ancak aktif işaretçi eşleşmediğinden artık kullanılamaz.

### Doğrulama ve tüketme — `consume-challenge.lua`

1. Java tarafı challenge metadata'sını bulur; bu okuma tek başına doğrulama sayılmaz.
2. Lua, amacı ve aktif challenge ID'sini yeniden kontrol eder.
3. HMAC değerinin tüm byte'ları karşılaştırılır; eşleşen öneke göre erken çıkılmaz.
4. Doğruysa challenge ve aktif işaretçi silinir; yalnızca bu isteğe başarı döner.
5. Yanlışsa deneme sayısı artırılır. Beşinci yanlış denemede challenge iptal edilir.

Yanlış denemeler TTL'yi uzatmaz. Tüketilmiş, süresi dolmuş, değiştirilmiş veya yanlış amaçla sunulmuş credential başarılı sayılmaz. Yeniden gönderim kilidi başarılı tüketimde de kalan süresi boyunca korunur.

### İptal — `revoke-challenge.lua`

Gönderimin kesin başarısız olduğu durumda ileride n8n adaptörü bu metodu çağırabilecek. Yalnızca ilgili gönderim denemesi iptal edilir; gecikmiş bir iptal çağrısı daha yeni gönderilen kodu silemez. Belirsiz ağ timeout'u durumunda gönderimin gerçekleşip gerçekleşmediği bilinemez; n8n idempotency/delivery politikası sonraki aşamada ele alınmalı.

### İstek sınırı — `rate-limit.lua`

Sayaç artışı ve TTL'nin ilk kez verilmesi birlikte yapılır. İlk istekte başlayan sabit pencere kullanılır; engellenen istekler pencereyi uzatmaz. IP ve e-posta ayrı sınırlanır. Pencere sınırında kısa süreli yığılma sabit pencere algoritmasının bilinen özelliğidir; bu bir WAF/DDoS savunması değildir.

## 6. Varsayılan sınırlar

| Kural | Değer |
| --- | --- |
| E-posta kodu ömrü | 10 dakika |
| Sıfırlama token'ı ömrü | 15 dakika |
| Yeniden gönderim aralığı | 60 saniye |
| Challenge başına yanlış deneme | 5 |
| E-posta başına gönderim isteği | Amaç başına 5/saat |
| IP başına gönderim isteği | Amaç başına 20/saat |
| IP başına doğrulama/tüketme isteği | Amaç başına 100/15 dakika |

Gönderim sınırları gerçek e-posta teslimini değil **istekleri** sayar. Cooldown sırasında veya bilinmeyen adresle yapılan istekler de bütçe tüketebilir. Böylece kayıtlı olmayan adresler hesap aramasından önce sınırlanır. IP adresi gelecekteki HTTP katmanında sunucu tarafından belirlenmeli; kullanıcıdan gelen keyfi bir `X-Forwarded-For` başlığına güvenilmemeli. Güvenilir proxy yapılandırması ayrıca yapılmalıdır.

## 7. PostgreSQL ile bağlantı

`AccountRecoveryService` dört ana işlem sunar:

- `requestEmailVerification(email, clientAddress)`
- `requestPasswordReset(email, clientAddress)`
- `verifyEmail(challengeId, code, clientAddress)`
- `resetPassword(challengeId, token, password, confirmation, clientAddress)`

İstek metotlarının sonucu HTTP'ye dönülecek cevap değildir. `Delivery` yalnızca backend'in n8n'e iletmesi gereken bilgiyi taşır. Kayıtlı olmayan, doğrulanmış veya limitlenmiş isteklerde zarf üretilmeyebilir. Gelecekteki HTTP endpoint'i bu farklılıkları ve süre farklarını hesap varlığını açığa çıkaracak biçimde sunmamalı; genel cevap ve asenkron gönderim tasarlanmalıdır.

E-posta doğrulamada, başarılı Redis tüketiminden sonra `email_verified_at` yazılır. Role ve parolaya dokunulmaz. Şifre sıfırlamada kayıt formuyla aynı parola kuralları uygulanır: 12–64 karakter, en fazla 72 UTF-8 byte, null karakter yasağı ve eşleşen parola tekrarı. Yeni parola mevcut BCrypt encoder ile hash'lenir.

SQL güncellemesinin koşulu `id` ve challenge'daki `auth_version` eşleşmesidir. Parola güncellenirken sürüm aynı SQL işleminde bir artırılır. Böylece önceden çıkarılmış eski sürümlü bir token, Redis'e yeniden konulsa dahi tekrar parola değiştiremez. Bu koruma, SQL verisinin ayrıca eski bir yedeğe geri alınmadığını varsayar.

**Redis ve SQL arasında dağıtık transaction yoktur.** Credential önce Redis'te tüketilir, sonra kısa bir SQL transaction'ı açılır. SQL hatasında başarı dönülmez ve credential yeniden geçerli hale getirilmez. Normal rollback'te hesap değişikliği kaydedilmez; commit yanıtı kaybolursa veritabanının değişikliği kaydedip kaydetmediği belirsiz olabilir. Her iki durumda da aynı token tekrar kullanılamaz; kullanıcı yeni sıfırlama e-postası istemelidir. Bu, tekrar kullanılabilir bir sıfırlama token'ı bırakmamak için bilinçli tercihtir.

## 8. Eski oturumlar nasıl kapanıyor?

Login sırasında `AccountPrincipal`, kullanıcının `auth_version` değerini de alır. `CurrentAccountSessionFilter`, gelen oturumdaki ID/e-posta/sürüm üçlüsünü veritabanıyla karşılaştırır. Uyuşmazsa SecurityContext temizlenir ve HTTP session geçersiz kılınır.

Bu kontrol admin sayfaları dahil gerçek hesap oturumu taşıyan isteklerde çalışır. Dashboard/API yetkilendirmesi ve uzun üretim işleminin kayıt öncesi kontrolü de sürümü doğrular. Mevcut CSRF koruması kaldırılmadı. Kontrol isteğin başında yapılır: başlamış bir HTTP isteğini veya harici AI çağrısını uzaktan durdurma garantisi yoktur. Parola sıfırlama sonrası otomatik login yapılmaz.

Session verileri Redis'e taşınmadı; mevcut servlet session düzeni korunur. Çoklu instance için session paylaşımı/sticky-session ayrı bir konudur.

## 9. Docker ve çalışma davranışı

`redis:7.4-alpine` servisi `adwflow-redis` adıyla başlatılır. Bu kurulumda doğrulanan sürüm 7.4.11'dir. Host erişimi yalnızca `127.0.0.1:6379` üzerindedir; port `REDIS_PORT` ile değiştirilebilir. Redis parola ister, root olmayan kullanıcıyla çalışır, container dosya sistemi salt okunurdur; `/data` ve `/tmp` RAM dosya sistemidir. Ek Linux yetenekleri kaldırılmıştır.

RDB snapshot ve AOF kapalıdır; Redis için kalıcı volume yoktur. Yeniden başlatma tüm geçici kodları/token'ları ve sayaçları kaybettirir. Kullanıcı yeniden kod istemelidir. SQL hesapları, reklam geçmişi ve görsel dosyaları bundan etkilenmez. Sayaçların da sıfırlanması nedeniyle Redis restart'ı bir rate-limit dayanıklılık garantisi değildir.

128 MB `maxmemory` ve `noeviction` kullanılır. Bellek dolduğunda sayaçları rastgele silmek yerine yazma hatası alınır; uygulama bunu başarıya çeviremez. Bu değer kapasite testiyle üretime göre ayarlanmalıdır. Redis Lua atomik yürütme sağlar ama SQL tipi hata rollback'i sağlamaz; komut/altyapı hatalarında başarısız işlem tekrar kullanılabilir bir başarı kanıtı sayılmaz.

Başlangıçta `PING` kontrolü varsayılan olarak zorunludur. Redis kapalıysa veya kimlik bilgileri yanlışsa uygulama başlangıcı başarısız olur. Çalışırken Redis kesilirse yeni doğrulama/sıfırlama işlemleri hata verir; SQL veya RAM tabanlı alternatif başarı yolu yoktur. Mevcut login, dashboard ve üretim akışları çalışma sırasında Redis'i doğrudan kullanmaz. Actuator health Redis'i de izler. Bağlantı ve komut timeout'ları iki saniyedir.

Yerel bağlantıda TLS kapalıdır. Üretimde güvenilir özel ağ, Redis ACL, TLS (`REDIS_SSL_ENABLED`), sertifika güveni, ayrı sır yönetimi ve bellek/sağlık alarmları kurulmalıdır. `.env` Git dışında tutulur ancak bir secret vault değildir. Docker yöneticileri container ortamındaki sırlara erişebilir. Redis/Jedis protokol debug logları, request body ve n8n execution loglarında kod/token kaydı açılmamalıdır.

## 10. Yerel kullanım

PowerShell execution policy sistem genelinde değiştirilmez. Yalnızca ilgili işlem için:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\redis.ps1 setup
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\redis.ps1 start
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\redis.ps1 status
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\redis.ps1 check
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\redis.ps1 test
```

`setup`, eksik `REDIS_PASSWORD` ve `AUTH_HMAC_SECRET` değerlerini kriptografik rastgelelikle üretir. Dolu değerler, OpenAI anahtarı ve DB ayarları korunur; sırlar terminale yazılmaz. `start` aynı hazırlığı yapıp yalnızca Redis servisini açar. `stop`, yalnızca Redis'i durdurur; geçici kayıtlar kaybolur. Doğrudan başlatma: `docker compose up -d --wait redis`.

HMAC sırrını rutin restart'ta değiştirmeyin. Rotasyon mevcut kodları geçersiz kılar ve HMAC tabanlı sayaç adlarını değiştirir. Ortam değişkenleri `.env` üzerindeki değerleri ezebilir. Uygulama Docker'a taşınırsa Redis host'u container'ın kendi `127.0.0.1` adresi değil Compose servis adı `redis` olmalıdır.

## 11. Testler ve kanıtlar

Genel testler `mvn -B test` ile çalışır. `TemporarySecurityTests` kriptografik biçimleri, sır doğrulamasını, redaksiyonu, bozuk handle'ları ve bağlantı hatalarının başarıya dönüşmemesini kontrol eder.

`SecurityRedisIT` gerçek Redis gerektirir ve varsayılan Maven test deseninden ayrı tutulur. `scripts/redis.ps1 test` ile açıkça çalıştırılır. SQL tarafı varsayılan olarak H2 test veritabanıdır. Redis DB 15 içinde test başına değil çalıştırma başına rastgele bir namespace kullanır; yalnızca bu namespace temizlenir, `FLUSHDB`/`FLUSHALL` kullanılmaz.

`RedisConnectivityIT`, `scripts/redis.ps1 check` ile MVC/JPA bağlamını açmadan doğrudan Jedis bağlantısını ve kimlik doğrulamayı sınar.

Entegrasyon kapsamı: TTL, tek kullanım, amaç ayrımı, beş yanlış deneme, cooldown, son kodun geçerliliği, gecikmiş iptal, eşzamanlı tüketim/gönderim/sayaç, gerçek hesap doğrulaması, BCrypt parola değişimi, user/admin oturumlarının kapanması, eski sürümlü token reddi, SQL hata senaryosu ve bilinmeyen e-posta/IP limitleri.

Son sürümde doğrulanan sonuçlar:

| Kontrol | Sonuç |
| --- | --- |
| Genel uygulama/regresyon testleri | 61 geçti |
| Redis 7.4.11 + PostgreSQL 17.10 ile `SecurityRedisIT` | 17 geçti |
| Doğrudan Jedis bağlantı testi | 1 geçti |
| Birlikte çalıştırılan toplam | **79 test; 0 hata, 0 başarısızlık, 0 atlanan** |
| Parolasız Redis bağlantısı | `NOAUTH Authentication required` ile reddedildi |
| Parolalı host bağlantısı | `AUTH +OK`, `PING +PONG` |
| Redis runtime ayarları | AOF/RDB kapalı, 128 MB, `noeviction` doğrulandı |
| Kurulum betiği tekrar çalıştırma | Dolu `.env` dosyasının byte içeriği değişmedi |
| Maven paketleme | `BUILD SUCCESS`; JAR içinde dört Lua betiği ve Jedis 7.4.1 doğrulandı |

Son ortak çalıştırmanın kaydı `target/redis-final-verification.log`, hızlı bağlantı kaydı `target/redis-connectivity.log` dosyasındadır. SQL entegrasyonu, mevcut PostgreSQL verilerine dokunmayan ayrı `adwflow-redis-pg-test` container'ında yapıldı. SQL test adresi `redis.it.jdbc-url`, driver `redis.it.jdbc-driver`, kullanıcı `redis.it.jdbc-user` ve parola `REDIS_IT_JDBC_PASSWORD` ile bu geçici veritabanına yönlendirildi. Redis test anahtarları kendi namespace'lerinden temizlendi. Test sonunda geçici PostgreSQL container'ı durdurulup `--rm` ile kaldırıldı; `adwflow-redis` çalışır durumda bırakıldı.

İlk Lettuce denemeleri başarısızdı; yukarıdaki sayılar bunları değil, Jedis'e geçişten sonraki son sürümü ifade eder. Gerçek e-posta gönderimi, n8n workflow'u, tarayıcı arayüz testi ve ücretli AI çağrısı yapılmadı.

## 12. Sonraki n8n aşaması

Planlanan bağlantı: backend isteği → Redis challenge oluşturma → kimliği doğrulanmış n8n webhook → e-posta sağlayıcısı. Doğrulama ve parola değişimi backend'de kalır; n8n'in PostgreSQL veya Redis'e doğrudan erişmesine gerek yoktur.

Sonraki aşamada webhook kimlik doğrulaması, HTTPS ve güvenilir reset URL'si, execution log redaksiyonu, CSRF korumalı HTTP formları, genel hesap-kurtarma cevapları, delivery idempotency/yeniden deneme politikası ve parola değişti bildirim e-postası eklenecek. n8n GET link önizlemeleri token tüketmemeli; parola değişimi yalnızca açık POST işlemiyle yapılmalı. Bir gönderim kuyruğu henüz yoktur; gerekirse kullanıcı planına uygun Redis tabanlı tasarlanabilir, ancak bu geçici Redis'in restart'ta veri kaybetmesiyle kuyruğun teslim garantisi ayrıca değerlendirilmelidir.

## Kaynaklar

- [Spring Boot: Redis bağlantısı ve starter](https://docs.spring.io/spring-boot/reference/data/nosql.html)
- [Spring Boot: Lettuce yerine Jedis kullanımı](https://docs.spring.io/spring-boot/how-to/nosql.html)
- [Spring Data Redis: Lua scripting](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html)
- [Redis: Lua atomik yürütme](https://redis.io/docs/latest/develop/interact/programmability/eval-intro/)
- [OWASP: Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)

# AdwFlow PostgreSQL ve Flyway kurulumu

## 1. Bu adımın kapsamı

Üç uygulama tablosu vardır: `app_users`, `history`, `archive`. `flyway_schema_history` yalnızca migration sürüm takibidir. Doğrulama kodu, şifre sıfırlama token'ı, ayrı rol veya SQL outbox tablosu eklenmez.

Bu değişiklik şemayı ve kullanıcı modelini hazırlar. History/Archive entity, servis, sahiplik denetimi, ekran, beğenme, süre temizliği ve Redis/n8n akışları henüz uygulanmamıştır. `email_verified_at` ve `auth_version` alanlarının bulunması tek başına doğrulama veya oturum iptali sağlamaz. Mevcut giriş davranışı korunur.

Sonraki dashboard eklemesiyle `/dashboard` ve `/dashboard/generate` ekranları kullanılabilir. Bu ekranlar ve üretim API'leri oturumdaki `userId` için `app_users` tablosunda ID/e-posta eşleşmesini sorgular. Bu ekleme History/Archive işlevlerini veya Redis/n8n akışlarını etkinleştirmez; ayrıntılar `SECURITY.md` içindedir.

## 2. Kurulum ve bağlantı

Docker Desktop çalışırken proje kökünde:

```powershell
.\scripts\database.ps1 start
```

Betik sırayla:

1. Yalnızca DB ayarlarını ortamdan veya `.env` dosyasından okur; ortam önceliklidir.
2. DB parolası eksikse kriptografik rastgele 32 byte üretip Git dışında tutulan `.env` dosyasına kaydeder. Mevcut OpenAI anahtarı ve diğer ayarlar korunur. Parola ekrana veya komut argümanlarına yazılmaz.
3. `docker compose up -d --wait postgres` ile PostgreSQL'i başlatır ve healthcheck'i bekler. Mevcut observability servisine dokunmaz.
4. `mvn -B flyway:migrate` ile migration'ları uygular. Web sunucusu veya AI istemcisi başlamaz; bu adım OpenAI anahtarı gerektirmez.

| Ayar | Açıklama |
| --- | --- |
| Host | `127.0.0.1` — port yalnızca yerel bilgisayara açılır |
| Host portu | `DB_PORT`; varsayılan 5432, bu çalışma alanının `.env` ayarı **5433** |
| Container portu | 5432 |
| Container adı | `adwflow-postgres` |
| Veritabanı | `DB_NAME`, varsayılan `adwflow` |
| Kullanıcı | `DB_USERNAME`, varsayılan `adwflow` |
| Parola | `.env` içindeki `DB_PASSWORD`; paylaşılan örnekte gerçek parola yoktur |
| JDBC URL | Bu kurulumda `jdbc:postgresql://127.0.0.1:5433/adwflow` |

5432 başka bir projenin PostgreSQL konteyneri tarafından kullanıldığı için bu projede 5433 seçilmiştir. Diğer veritabanı değiştirilmemiştir. Yeni ortamda `.env.example` içindeki eksik ayarları kendi `.env` dosyana ekle; mevcut `.env` dosyanı örnekle ezme.

`.env` hem Compose hem Spring tarafından okunur. Uyum için tırnaksız `KEY=value` satırları kullan; betiğin ürettiği Base64 parola bu biçime uygundur. Özel karakter/interpolasyon içeren kendi parolan için süreç ortam değişkenlerini tercih et. `.env` dosyasını Git'e ekleme ve işletim sistemi erişim izinleriyle koru.

Uygulamayı ayrıca başlat:

```powershell
.\mvnw.cmd spring-boot:run
```

Sistemde Maven kuruluysa `mvn spring-boot:run` da kullanılabilir. Artık runtime PostgreSQL gerektirir, H2'ye geri dönüş yoktur. Docker Compose otomatik yaşam döngüsü hâlâ varsayılan kapalıdır; betik servisi önceden başlatır. Harici PostgreSQL seçmek için `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ortam değişkenlerini ayarla. `start` yanlışlıkla harici veritabanına migration uygulamamak için yerel URL dışını reddeder; harici hedef için `migrate` açıkça kullanılmalıdır.

## 3. Migration'lar

| Sürüm | İşlem |
| --- | --- |
| V1 | Önceden var olan hesap/rol şemasını oluşturur. Dosya değiştirilmedi. |
| V2 | Rolü `app_users.role` alanına taşır. Eski roller arasında ADMIN varsa ADMIN, aksi durumda USER seçilir. `email_verified_at` NULL, `auth_version` 0 olarak eklenir; eski rol tablosu kaldırılır. |
| V3 | History ve Archive tabloları, kullanıcı foreign key'leri, JSON içerik alanları ve sorgu indeksleri oluşturulur. |

V2 aynı veritabanındaki kullanıcı kimliklerini, parolaları ve diğer hesap verilerini korur. **Eski H2 dosyasındaki hesaplar yeni PostgreSQL'e otomatik kopyalanmaz.** `data/` altındaki mevcut dosyalar silinmedi; bu verileri taşımak ayrı bir veri aktarımı gerektirir.

History'de `is_liked` varsayılan false'tur. `expires_at` zorunludur ve oluşturulma zamanından sonra olmalıdır. Kullanıcı bazlı tarih sıralaması, kullanıcı/beğeni filtresi ve süre temizliği için indeksler vardır. Normal/beğenilmiş süreleri henüz sabitlenmedi; ileride servis `expires_at` değerini belirleyecek. SQL tek başına otomatik kayıt silmez.

Archive kendi başlık, brief, JSON içerik ve orijinal oluşturulma zamanını tutar. History'ye foreign key yoktur; History silinince arşiv kaybolmaz. Arşivde son kullanma tarihi yoktur. Kullanıcı foreign key'lerinde otomatik silme zinciri yoktur; bağlı kayıtlar varken kullanıcı silme reddedilir. İleride hesap silme politikası ayrıca uygulanmalıdır.

`result_json` gerçek SQL `JSON` tipidir; PostgreSQL ile H2 migration testlerinde aynı SQL kullanılır. JSON içeriği üzerinde indeksli arama ihtiyacı oluşursa ayrı migration ile PostgreSQL `JSONB` değerlendirilebilir.

## 4. Tekrar çalıştırma ve veri saklama

```powershell
.\scripts\database.ps1 info
.\scripts\database.ps1 migrate
docker compose ps
docker compose stop postgres
```

`migrate` tekrar çalıştırıldığında uygulanmış sürümler tekrar uygulanmaz; checksum denetlenir. Uygulama açılışında da aynı migration'lar kontrol edilir ve Hibernate kullanıcı modelini `validate` eder. Uygulanmış SQL dosyalarını düzenlemek yerine yeni V4, V5 migration'ları ekle.

Veri `postgres-data` adlı Compose volume'ünde kalır; bu projenin varsayılan volume adı `proje2_postgres-data` olur. Container yeniden yaratılması veriyi silmez. **`docker compose down -v` veri volume'lerini siler; sıfırlama için çalıştırma.** Flyway `clean` kapalıdır; kurulum betiği `repair`, `clean` veya volume silme yapmaz.

Mevcut volume oluşturulduktan sonra `.env` parolasını değiştirmek PostgreSQL kullanıcısının parolasını değiştirmez. Kimlik bilgisi uyuşmazlığında volume'ü silmek yerine mevcut parolayı geri yükle veya kontrollü parola değişimi yap. Resmî imajın `POSTGRES_*` başlangıç değişkenleri boş veri dizininde uygulanır. [PostgreSQL Docker imajı](https://hub.docker.com/_/postgres)

Bu Compose geliştirme içindir: `POSTGRES_USER` başlangıçta geniş yetkili kullanıcı oluşturur. Üretimde migration ve uygulama kullanıcılarını ayır, uygulamaya gereken en az yetkiyi ver; TLS, yedekleme ve parola yönetimini ayrıca kur.

## 5. Testler

```powershell
mvn -B test
.\scripts\database.ps1 test
```

İlk komut mevcut güvenlik testlerini ve migration testlerini H2 üzerinde çalıştırır. İkinci komut **aynı beş migration testini gerçek PostgreSQL üzerinde** çalıştırır. Her test rastgele isimli kendi `migration_test_*` şemasını oluşturur ve sonunda yalnızca o şemayı kaldırır; `public` veya mevcut kullanıcı kayıtlarını temizlemez. Bu komut migration testi için şema oluşturma yetkisi gerektirir.

Test kapsamı: sıfırdan kurulum ve tekrar migration, V1 verilerinin korunarak yükseltilmesi, admin rolünün korunması, doğrulanmış e-posta uydurulmaması, geçersiz rol/foreign key/tarih/JSON reddi, kullanıcıya özel beğenilmiş sorgusu ve arşiv taşımasının transaction ile geri alınabilir olması. Bunlar şema sözleşmesi testleridir; henüz yazılmamış History/Archive endpoint'lerinin güvenlik testi değildir.

### Bu çalışmadaki doğrulama

- `mvn -B test`: **36 test başarılı**, hata/atlanan test yok. Mevcut 31 test ile 5 migration testi birlikte çalıştı; tek ADMIN rolüyle gerçek form girişinden sonra hem `/admin` hem `/generate` erişimi doğrulandı.
- `.\scripts\database.ps1 test`: gerçek **PostgreSQL 17.10** üzerinde **5 migration testi başarılı**, hata/atlanan test yok.
- Testlerin ardından `mvn -B -DskipTests package` başarılı oldu; çalıştırılabilir JAR yeniden üretildi. Bu paketleme adımı testleri tekrar çalıştırmaz.
- Docker konteyneri `healthy`; yalnızca `127.0.0.1:5433` üzerinden yayımlanıyor. Diğer projenin 5432 portundaki PostgreSQL'i ve mevcut observability servisi değiştirilmedi.
- `public` şemasında SQL sorgusuyla `app_users`, `history`, `archive`, `flyway_schema_history` tabloları doğrulandı. Flyway V1, V2 ve V3 kayıtları başarılı; History/Archive indeksleri mevcut.
- Testler mevcut H2/Flyway sürüm uyarısını üretmeye devam ediyor; hata oluşmadı. Bu çalışmada web sunucusu veya tarayıcı kontrolü yapılmadı; veritabanı kurulumu uygulamayı başlatmadan doğrulandı.

Flyway Maven eklentisi uygulamadaki Boot tarafından yönetilen Flyway sürümünü kullanır. Bağlantı bilgileri `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD` üzerinden yalnızca alt süreçlere aktarılır ve betik bitiminde önceki ortam geri yüklenir. [Flyway Maven yapılandırması](https://documentation.red-gate.com/fd/maven-goal-277579365.html)

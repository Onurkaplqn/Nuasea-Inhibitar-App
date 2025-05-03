# CURSOR RULES: Android Geliştirme Kılavuzu

## 1. Kod Organizasyonu ve Mimarisi

### Mimari Prensipler
- **MVVM (Model-View-ViewModel)** mimarisini kullan
- Her bir ekran için ayrı bir ViewModel oluştur
- Tüm iş mantığını (business logic) ViewModel'larda tut
- UI ve iş mantığını kesinlikle ayır
- Bağımlılık enjeksiyonu için Dagger veya Hilt kullan

### Paket Yapısı
- Özellik bazlı paketleme yap (feature-based packaging)
- Her özellik kendi View, ViewModel, Repository ve model sınıflarını içersin
- Ortak kullanılan bileşenler için `common`, `core` veya `shared` paketleri oluştur

### Dosya Organizasyonu
- Kaynak dosyalarını amaçlarına göre grupla
- Her Java sınıfı tek bir sorumluluğa sahip olmalı (Single Responsibility Principle)
- Sınıf başına 500 satır kodu geçme, geçiyorsa sınıfı böl
- MainActivity veya Fragment'lar sadece UI işlemleri içermeli, iş mantığı içermemeli

---

## 2. Kod Yazım Kuralları

### Naming Conventions
- **Sınıflar**: PascalCase (`MainActivity`, `UserViewModel`)
- **Metotlar**: camelCase (`onCreate()`, `getUserData()`)
- **Değişkenler**: camelCase (`userName`, `isLoggedIn`)
- **Sabitler**: UPPER_SNAKE_CASE (`API_BASE_URL`, `MAX_RETRY_COUNT`)
- **Layout dosyaları**: snake_case (`activity_main.xml`, `item_user.xml`)
- **ID'ler**: snake_case (`user_name_text`, `login_button`)

### Kod Formatı
- Girinti için 4 boşluk kullan
- Her satır 100 karakteri geçmemeli
- Java dosyalarında metodlar arasında bir satır boşluk bırak
- Kapama parantezleri (`}`) her zaman kendi satırında olmalı

### Yorumlar
- Karmaşık mantık için açıklayıcı yorumlar ekle
- Sınıf ve public metotlar için Javadoc yorumları kullan
- TODO, FIXME gibi işaretler kullan, ama bunları sonradan temizle

---

## 3. Performans Optimizasyonu

### UI Performansı
- Ana thread'de ağır işlemler yapma
- Uzun süren işlemler için CoroutineScope veya RxJava kullan
- RecyclerView için ViewHolder pattern'i doğru kullan
- ViewBinding kullan, findViewById'dan kaçın
- ConstraintLayout kullanarak düz hiyerarşi oluştur

### Bellek Yönetimi
- Memory leak oluşturacak durumlardan kaçın (özellikle Activity içinde static referanslar)
- Context gerektiren durumlarda ApplicationContext kullanmayı tercih et
- Bitmap gibi büyük nesneleri doğru şekilde yönet ve geri bırak
- SparseArray ve ArrayMap gibi bellek-verimli koleksiyonları kullan

### Pil Optimizasyonu
- Location API'lerini gerektiği kadar kullan
- AlarmManager ve WorkManager için uygun aralıklar belirle
- Arka planda çalışan servisler için JobScheduler veya WorkManager kullan
- Sensör veri toplama sıklığını optimize et

---

## 4. Güvenlik Pratikleri

### Veri Güvenliği
- Kritik verileri EncryptedSharedPreferences kullanarak sakla
- API anahtarlarını native kodda (C/C++) veya BuildConfig'de sakla
- SSL pinning ile network güvenliğini sağla
- Hassas verileri loglamaktan kaçın

### Kod Güvenliği
- ProGuard/R8 kullanarak kodu karıştır (obfuscate)
- Debuggable olmayan release build'ler oluştur
- Root algılama mekanizması ekle
- Uygulama içi satın almaları güvenli şekilde doğrula

---

## 5. Test Stratejisi

### Birim Testleri
- Her ViewModel için unit test yaz
- Repository sınıfları için mock kullanarak unit test yaz
- Kritik business logic için kapsayıcı testler yaz
- JUnit ve Mockito kullan

### UI Testleri
- Temel kullanıcı yolculukları için Espresso veya UI Automator testleri yaz
- Ekran rotasyonu gibi edge case'leri test et
- Fragment'lar için izole testler yaz

### Entegrasyon Testleri
- Room veritabanı ile repository entegrasyonunu test et
- API entegrasyonunu test et (mock sunucu kullanarak)

---

## 6. Veritabanı ve Veri Yönetimi

### Room Veritabanı
- Room kullanarak SQL işlemlerini soyutla
- DAO interfaceleri için kapsamlı metotlar tanımla
- Veritabanı migrasyonlarını doğru şekilde yönet
- Kompleks sorgular için ilişkisel modelleme kullan

### SharedPreferences
- Key-value çiftleri için enum kullan
- Büyük veri kümeleri için SharedPreferences kullanma, Room tercih et
- Veri tutarlılığı için PreferenceManager veya DataStore kullan

### Network İşlemleri
- Retrofit ve OkHttp kullan
- API çağrıları için interceptor'lar ile logging ve caching mekanizmaları oluştur
- Network hataları için doğru hata yönetimi stratejisi belirle
- Pagination için Paging 3 kütüphanesini kullan

---

## 7. UI/UX Pratikleri

### Material Design
- Material Componenets kütüphanesini kullan
- Tutarlı tema ve renkler için styles.xml ve themes.xml dosyalarını organize et
- Animasyonlar için Material Motion kullan
- Erişilebilirlik için content description'ları doğru ayarla

### Responsive Design
- Farklı ekran boyutları için alternatif layoutlar oluştur
- dp ve sp birimlerini doğru kullan
- Landscape mod desteği ekle
- Tablet için özelleştirilmiş layout'lar oluştur

### Kullanıcı Deneyimi
- İşlem yapılırken kullanıcıya feedback ver (loading state, error state)
- Offline mod desteği ekle
- Hata mesajlarını kullanıcı dostu yap
- Dark mode desteği ekle

---

## 8. Dependency Management

### Gradle Yapılandırması
- Version catalog veya buildSrc ile dependency yönetimi yap
- Modüler yapı için project-level build.gradle dosyasını düzenle
- Proguard/R8 kurallarını optimize et
- Build varyantlarını ve buildTypes'ları doğru yapılandır

### Kütüphane Seçimi
- Lifecycle-aware bileşenler için Android Architecture Components kullan
- Image loading için Glide veya Coil tercih et
- Networking için Retrofit + OkHttp + Gson/Moshi kombinasyonu kullan
- Test için JUnit, Mockito, Espresso üçlüsünü kullan

---

## 9. CI/CD ve Deployment

### CI/CD Pipeline
- Her commit için otomatik build ve test işlemi çalıştır
- UI testlerini Firebase Test Lab'de çalıştır
- SonarQube ile kod kalitesini analiz et
- Release öncesi lint kontrolü yap

### Deployment
- Google Play Console'da internal test track kullan
- Automated versioning için Gradle script oluştur
- Release notlarını otomatik oluştur
- Firebase App Distribution ile dahili testleri yönet

---

## 10. Hata Yakalama ve Loglama

### Crash Reporting
- Firebase Crashlytics entegre et
- Önemli hata durumlarını raporla
- ANR'leri takip et ve optimize et
- Crashfree kullanıcı oranını ölçümle

### Loglama Stratejisi
- Production build'de hassas bilgileri loglama
- Log seviyelerini doğru kullan (DEBUG, INFO, WARN, ERROR)
- Timber kütüphanesi ile loglama yap
- Remote logging için analitik araçlar kullan

---

## 11. Geliştirici Araçları Kullanımı

### Android Studio
- Live Templates oluştur ve kullan
- Hotkey'leri öğren ve kullan
- Favorite Actions menüsünü yapılandır
- Profiler ve Memory Monitor ile performans takibi yap

### Git Kullanımı
- Feature branch workflow'unu takip et
- Commit mesajları için standardizasyon belirle
- Rebase vs. merge stratejini belirle
- Git hooks ile pre-commit kontrolleri yap

---

## 12. Hareket Sensörü Özel Optimizasyonları

### Sensör Veri İşleme
- SensorManager.SENSOR_DELAY_GAME veya SENSOR_DELAY_UI kullan (SENSOR_DELAY_FASTEST çok pil tüketir)
- Sensör verilerini batch halinde işle
- Lowpass filter implementasyonu ile sensör gürültüsünü azalt
- Kalman filtresi kullanarak sensör verilerini stabilize et

### Motion Sickness Algoritmaları
- Hız değişimlerini tespit eden algoritmaları optimize et
- Gyroscope ve accelerometer verilerini kombine et
- Cihaz oryantasyonunu doğru hesapla (rotation matrixleri kullan)
- Threshold değerleri kullanıcı geri bildirimiyle kalibre et

---

## 13. Ekran Filtreleri ve Görüntü İşleme

### Overlay Optimizasyonu
- WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY kullan
- Overlay'lerin z-index yönetimini doğru yap
- Hardware acceleration kullanarak ekran filtrelerini hızlandır
- Düşük alpha değerlerinde bile performansı korumak için GPU'yu kullan

### Beyaz Denge ve Mavi Işık Filtresi
- ColorMatrix manipulation kullanarak daha verimli filtreler oluştur
- RenderScript ile filtreleme performansını artır
- Gece/gündüz durumlarına göre otomatik filtre geçişleri yap
- Düşük ışık koşullarını otomatik algıla ve uygun filtreleri uygula

---

## 14. Sistem Optimizasyonu

- Herhangi bir kod işlemi, güncelleme veya ekleme yaparken, diğer işlevleri bozmadan veya kaldırmadan yapın. Gerekli değilse, bu işlemi yapma nedenini belirtin ve manuel olarak nasıl yapılacağını açıklayın.
- Bir işlemi başlattıktan sonra, işlem tamamlanana kadar durdurmayın veya kesmeyin. İşlem, güncelleme veya ekleme yapılamıyorsa, bu durumu belirtin ve manuel olarak nasıl yapılacağını açıklayın.
- Tüm kod işlemleri için, en büyük kullanıcı tabanına uygun şekilde kod yazın, yani en düşük SDK seviyesi. İstenen işlem bu belirli SDK/API seviyesinde hiç yapılamazsa, gerekli SDK/API veya bileşenleri yükseltmeyi önerin. Onay verirseniz, gerekli işlemleri yapın.
- Geriye uyumluluk için, gerektiğinde her Android, API ve SDK seviyesi için ilgili aktivitede kodu düzenleyin ve her telefonda sorunsuz ve optimal şekilde çalışacak şekilde ayarlayın.
- Gerekli tüm kütüphaneleri tamamen içerir.

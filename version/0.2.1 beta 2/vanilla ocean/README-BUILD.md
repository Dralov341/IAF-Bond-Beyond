# Ice And Fire: Bond Beyond 0.2.1 — Vanilla-Ocean

Source bản 0.2.1 đã lưu của lần phát hành trước. Không trộn code 0.2.2 vào bản này; toàn bộ file gốc được giữ nguyên byte, chỉ thêm wrapper và hướng dẫn build.

## Build trên Windows

1. Giải nén ZIP vào một thư mục riêng.
2. Cài **JDK 17**; kiểm tra bằng `java -version`. Nếu có nhiều Java, đặt JAVA_HOME trỏ tới JDK 17.
3. Mở PowerShell tại thư mục có `build.gradle`, chạy:

```powershell
.\gradlew.bat build --console=plain --max-workers=2
```

JAR sau khi build/reobfuscate thành công:

`build/libs/iceandfire_bond_beyond-0.2.1-vanilla-ocean.jar`

Không cần cài Gradle riêng. Wrapper đi kèm dùng Gradle 8.8. Lần build đầu cần Internet
để tải Gradle và các dependency; ZIP không chứa cache hay bản sao dependency.

## Linux / macOS

```sh
chmod +x gradlew
./gradlew build --console=plain --max-workers=2
```

## IntelliJ IDEA và chạy thử

Mở thư mục chứa `build.gradle` như một Gradle project. Chọn Gradle JVM = JDK 17,
Gradle distribution = Wrapper. Chờ import dependency hoàn tất.

```powershell
.\gradlew.bat compileJava --console=plain --max-workers=2
.\gradlew.bat runClient --console=plain --max-workers=2
```

Minecraft 1.20.1, Forge 47.3.5; dependency Ice and Fire beta-5 và Citadel đã khai báo
trong `build.gradle`. Development client có Jade; edition Tectonic còn có Tectonic.
Chỉ cài **một edition** của Bond Beyond trong game, cùng edition trên client/server.

## Nội dung và kiểm tra

- `src/`, `build.gradle`, `settings.gradle`, `gradle.properties`: source và cấu hình build.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/`: Gradle wrapper gốc, kèm checksum bản phân phối.
- `SOURCE-PROVENANCE.json`: đối chiếu source snapshot và những file thay đổi.
- `VALIDATION.md`: phạm vi kiểm thử của chính phiên bản này.

Wrapper đã chạy được với Java 17 trên Linux; chưa thử chạy script Windows trên Windows.
Source 0.2.1 giữ nguyên kết quả kiểm thử lịch sử trong VALIDATION.md, không nhận kết quả
GameTest của 0.2.2. Source tương ứng không bảo đảm JAR build lại có cùng hash vì manifest
có timestamp và build plugin dùng khoảng phiên bản/SNAPSHOT như cấu hình gốc.

Checksum wrapper và Gradle 8.8 đối chiếu tại https://gradle.org/release-checksums/.
LICENSE/NOTICE của Gradle nằm trong `gradle/wrapper/`; không thay thế giấy phép của mod.

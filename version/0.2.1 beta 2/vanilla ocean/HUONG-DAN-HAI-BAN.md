# Bond Beyond 0.2.1 — Tectonic và Vanilla Ocean

Hai ZIP là **mã nguồn đầy đủ**. Hai JAR 0.2.1 đi kèm đã compile và reobf
thành công với Forge 47.3.5 / Java 17; chưa kiểm thử gameplay trực tiếp.
Mỗi bản có toàn bộ nội dung trước, combat wild được sửa và xói mòn loang tự nhiên.

## Chọn bản

| | Bản Tectonic | Bản Vanilla Ocean |
|---|---|---|
| ZIP | Tên có 0.2.1-Tectonic-src | Tên có 0.2.1-Vanilla-Ocean-src |
| Minecraft | 1.20.1, Forge | 1.20.1, Forge |
| Ice and Fire + Citadel | Bắt buộc | Bắt buộc |
| Tectonic | Bắt buộc, nhánh 2.4.x | Không bắt buộc, không tự thêm khi runClient |
| Nước liên tục phía trên trứng để ấp | Ít nhất 15 block | Ít nhất 8 block |
| Áp suất an toàn | Đến 20 block phía trên trứng | Đến 32 block phía trên trứng |
| Nguy cơ vỡ mỗi block sâu thêm | 0,1% mỗi lần kiểm tra | 0,05% mỗi lần kiểm tra |
| Giới hạn nguy cơ mỗi lần | 15% | 5% |

Kiểm tra áp suất mỗi 100 tick (5 giây ở 20 TPS), chỉ khi trứng đủ điều kiện ấp.
Vẫn phải đặt trứng ngập nước trong biome biển. Tốc độ nở theo nhiệt độ biển,
thời gian ấp và tiến độ đang có không đổi. Các mức Vanilla Ocean là lựa chọn
cân bằng cho bản này, không phải độ sâu cố định của mọi đại dương vanilla.

## Xói mòn mới — cả hai bản

- Stage 1–3: vùng 3×3×3.
- Stage 4: vùng 5×5×5, bo góc như hơi thở IAF.
- Stage 5: vùng 7×7×7, bo góc như hơi thở IAF.
- Đất → sỏi → cát; mỗi block chỉ chuyển một bậc trong một lần bong bóng trúng.
- Loang ngẫu nhiên trên bề mặt, mạnh ở gần điểm chạm và thưa dần ở rìa.
- Chạy cho Hải Xà hoang, tự phun khi đã thuần và do người cưỡi điều khiển.
- Bubble cấp năng lượng cho lò còn xói nền đất phía dưới và quanh chân lò.
- Chỉ tác động đất/sỏi phù hợp; không đổi brick hay block entity của lò.
- Giữ mobGriefing, quyền phủ quyết theo block của Forge; không tải chunk mới.
- Không tăng damage, thời gian hiệu ứng hay tầm bay của bong bóng.

## Cài JAR đã build

Chọn một JAR 0.2.1 phù hợp với bộ mod, thay JAR Bond Beyond cũ trong thư mục
`mods`. Không cài đồng thời hai biến thể hoặc hai phiên bản Bond Beyond.
Ice and Fire beta-5 và Citadel vẫn bắt buộc; bản Tectonic cần thêm Tectonic.

## Build trên máy ông

1. Chọn **một** ZIP và giải nén vào thư mục project có `gradlew.bat`.
2. Chép đè `src`, `build.gradle`, `settings.gradle`, `gradle.properties` từ ZIP.
   Giữ Gradle wrapper của project hiện có. Cần Java 17.
3. Mở PowerShell tại thư mục đó và chạy:

```powershell
.\gradlew.bat clean build --console=plain --max-workers=2
```

4. JAR nằm trong `build/libs/`:

   - `iceandfire_bond_beyond-0.2.1-tectonic.jar`
   - hoặc `iceandfire_bond_beyond-0.2.1-vanilla-ocean.jar`

5. Kiểm thử trước khi đăng:

```powershell
.\gradlew.bat runClient --console=plain --max-workers=2
```

Hai bản có cùng mod ID: chỉ cài một bản, dùng cùng bản cho server và client.
Nếu đổi từ bản Tectonic sang bản vanilla để thử worldgen, dùng instance/thế giới
thử không có Tectonic; biến thể addon không tự chuyển địa hình world đã có.

## Dependency trên CurseForge

Có thể đăng hai JAR trong cùng project và đặt dependency riêng cho từng file.
Ở file Tectonic: đặt Ice and Fire, Citadel, Tectonic là Required Dependency.
Ở file Vanilla Ocean: đặt Ice and Fire và Citadel là Required Dependency;
không đặt Tectonic là Required. Jade chỉ là HUD tùy chọn khi phát triển.

Khai báo trên CurseForge phục vụ tải dependency. Khai báo `META-INF/mods.toml`
bên trong JAR được Forge dùng khi khởi động; cả hai đã được tách cho đúng bản.

Nguồn chính thức: [CurseForge Upload API — quan hệ dependency trong metadata của từng file](https://support.curseforge.com/support/solutions/articles/9000197321).

## Mức kiểm tra đã làm

Xem VALIDATION.md đi kèm cho kết quả kiểm tra của chính phiên bản 0.2.1.
Không dùng log build của các bản trước để xác nhận bản mới.

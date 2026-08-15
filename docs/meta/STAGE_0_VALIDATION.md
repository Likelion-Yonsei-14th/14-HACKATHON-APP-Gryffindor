# Stage 0 — Meta DAT Validation Record

## 목적

Meta DAT를 이용해 Android 앱에서 Camera Stream과 `VideoFrame` 데이터에 접근하고,
향후 실제 Meta Ray-Ban Gen2 입력을 앱 처리 파이프라인에 연결할 수 있는지 검증한 기록이다.

이 문서는 **이미 수행한 검증 결과와 아직 남은 실기기 검증 항목**을 구분해 보존한다.

---

# 1. 현재 결론

## Mock Device 기준

**GO**

다음 경계가 기술적으로 가능함을 확인했다.

```text
Meta DAT
→ Camera Stream
→ VideoFrame
→ frame bytes / timestamp / width / height 접근
→ 앱 소유 메모리로 복사
→ SDK 외부 처리 계층으로 전달 가능
```

기존 프로젝트에서는 이를 `FramePacket`으로 정규화했다.

새 Android 앱에서는 동일한 원칙을 유지하되,
이후 처리 목적은 Recognition Pipeline이 아니라 Object Detection / Attention Gate가 된다.

```text
DAT VideoFrame
→ Meta Camera Adapter
→ 앱 표준 Camera Frame
→ Object Detection
→ Attention Gate
```

---

# 2. Camera Stream / VideoFrame 접근

검증 흐름:

```text
Wearables.createSession()
→ session.start()
→ session.addCamera()
→ camera.stream.start()
→ camera.stream.videoStream.collect()
```

`VideoFrame`에서 확인한 데이터:

```text
buffer
width
height
presentationTimeUs
isCompressed
```

### 검증 결과

- [x] `DeviceSession` 생성
- [x] Session `STARTED`
- [x] `addCamera()` 성공
- [x] Camera Stream `STREAMING`
- [x] `videoStream.collect` 반복 호출
- [x] frame width 접근
- [x] frame height 접근
- [x] `presentationTimeUs` 접근
- [x] `ByteBuffer` 접근
- [x] Frame 데이터를 앱 소유 메모리로 복사
- [x] Stream 종료 동작 검증 — 이후 lifecycle 검증에서 완료

---

# 3. Frame 정규화 가능성

검증한 사항:

- [x] `VideoFrame.buffer` 안전 복사 가능
- [x] timestamp 전달 가능
- [x] width / height 유지 가능
- [x] compressed / uncompressed 구분 가능
- [x] Meta SDK 타입을 Adapter 경계 안에 가둘 수 있음
- [x] frame callback에서 네트워크/무거운 작업을 직접 수행하지 않는 구조 가능

기존 검증에서 고려한 변환 경로:

### Uncompressed

```text
YUV
→ JPEG / WebP
```

### Compressed

```text
HEVC
→ MediaCodec decode
→ JPEG / WebP
```

새 앱에서는 실제 downstream 요구에 따라 필요한 image representation만 선택한다.

---

# 4. Streaming 설정 검증

DAT 요청 가능 설정으로 검토한 값:

| Quality | Resolution |
|---|---:|
| LOW | 360 × 640 |
| MEDIUM | 504 × 896 |
| HIGH | 720 × 1280 |

요청 가능한 FPS로 검토한 값:

```text
2 / 7 / 15 / 24 / 30
```

실시간 처리 초기 후보:

```text
MEDIUM
7 or 15 FPS
```

### 검증 결과

- [x] MEDIUM / 7 FPS stream 동작
- [x] MEDIUM / 15 FPS stream 동작
- [x] 요청값과 실제 frame resolution이 다를 수 있음을 확인
- [x] frame별 width / height 확인 가능
- [x] 앱에서 일부 frame만 선택적으로 처리 가능
- [x] 처리 지연 시 오래된 frame을 버리는 구조 가능

새 앱에서도 모든 frame을 Backend에 보내지 않는다.

```text
모든 DAT frame
→ local Object Detection / Attention

관심 trigger frame만
→ Backend Recognition
```

---

# 5. Stream Lifecycle 검증

목표 lifecycle:

```text
쇼핑 시작
→ DeviceSession 생성
→ Camera Stream START

쇼핑 종료
→ Camera Stream STOP
→ Camera capability 정리
→ DeviceSession 종료
→ 리소스 해제
```

Mock Device에서 검증한 흐름:

```text
Session START
→ Stream START
→ Stream / Session STOP
→ 새로운 DeviceSession 생성
→ Stream 재시작
→ Frame 재수신
```

### 검증 완료

- [x] Stream start
- [x] Stream stop
- [x] Camera capability 제거
- [x] DeviceSession 종료
- [x] 동일 앱 실행 중 Stream / Session 재시작
- [x] `STOPPED` 이후 새 DeviceSession 생성
- [x] 새 Session에서 Stream 재시작
- [x] 재시작 후 Frame 재수신
- [x] Mock device 상태 변경 시 crash 없음
- [x] Stream error를 별도 error flow로 감지 가능한 구조 확인

### 실제 Gen2에서 남은 항목

- [ ] 안경 착용 해제 시 Stream 상태 변화
- [ ] 재착용 후 Stream 복구
- [ ] Bluetooth 연결 중단 / 재연결 후 복구

Mock의 Don OFF / ON 변경에서는 앱 crash나 UI 오류는 없었으나,
Camera Stream의 명확한 중단/자동 복구 동작은 관찰되지 않았다.

---

# 6. 종료 시 관찰한 Lifecycle

Session 종료 과정에서 다음 순서를 확인했다.

```text
Session state=STOPPING
→ Session state=STOPPED
→ Stream state=STOPPED
→ CAPABILITY_STREAM deactivated
→ MediaCodec release
→ StreamingService stopping
→ WakeLock released
→ StreamingService destroyed
```

종료 후 새로운 DeviceSession을 생성해 Camera Stream을 다시 시작했을 때
Frame 수신이 정상적으로 재개됐다.

---

# 7. 알려진 Warning

종료 과정에서 일시적으로 다음 MediaCodec warning을 관찰했다.

```text
Handler (android.media.MediaCodec$EventHandler)
sending message to a Handler on a dead thread
```

이후 codec은 `RELEASED` 상태로 정상 종료됐고,
앱 crash 또는 Stream lifecycle 실패는 발생하지 않았다.

Mock 검증에서는 차단 이슈로 판단하지 않았다.

실제 Gen2에서 반복될 경우 decoder teardown 순서를 추가 점검한다.

---

# 8. 아직 수행하지 않은 검증

## 장시간 Stream

- [ ] 5분 연속 Streaming
- [ ] 15분 연속 Streaming
- [ ] 시간 경과 후 frame 지속 수신
- [ ] 앱 crash 없음
- [ ] 메모리 지속 증가 없음
- [ ] 종료 후 리소스 정상 해제

목표 기준:

```text
15분 연속 Stream
→ crash / stream 중단 없음
```

## Background / 화면 잠금

- [ ] Background 진입 후 stream 상태
- [ ] 화면 잠금 후 stream 상태
- [ ] Foreground 복귀 후 stream 상태
- [ ] Foreground Service 동작
- [ ] 지속 Notification
- [ ] Wake Lock 사용 여부

이 항목은 MVP 필수 Go 조건은 아니다.

---

# 9. 실제 Gen2 검증

실제 기기에서 아직 확인해야 한다.

```text
Gen2
↓
Meta AI pairing
↓
DAT registration
↓
Camera permission
↓
DeviceSession
↓
Camera Stream
↓
VideoFrame
```

### 체크 항목

- [ ] DAT에서 실제 Gen2 표시
- [ ] Device compatibility 정상
- [ ] Camera permission
- [ ] DeviceSession `STARTED`
- [ ] Stream `STREAMING`
- [ ] VideoFrame 지속 수신
- [ ] MEDIUM / 7 FPS
- [ ] MEDIUM / 15 FPS
- [ ] 15분 연속 Stream
- [ ] 안경 벗음 / 재착용
- [ ] 안경 접음 / 다시 펼침
- [ ] 연결 중단 후 새 Session 복구
- [ ] 대상 휴대폰 frame 색상 / layout 이상 없음

실제 Gen2에서도 현재 Adapter 경계를 큰 구조 변경 없이 사용할 수 있으면 PASS로 본다.

---

# 10. 새 APP 개발에서 재사용할 결론

새 앱의 A1 단계는 Stage 0을 처음부터 재검증하는 작업이 아니다.

재사용할 사실:

1. DAT dependency와 CameraAccess sample은 Android 휴대폰에서 실행된 바 있다.
2. Mock Device Camera Stream을 수신할 수 있다.
3. `VideoFrame`의 bytes / timestamp / dimensions에 접근할 수 있다.
4. SDK frame을 앱 소유 데이터로 복사할 수 있다.
5. Stream과 DeviceSession의 start / stop / recreate가 Mock 기준 동작한다.
6. Frame callback 밖으로 처리 작업을 분리할 수 있다.

따라서 새 앱 A1의 목표는:

```text
기존 Stage 0 Camera 코드 이식
→ 새 앱 Camera adapter 경계에 연결
→ Object Detection 입력까지 전달
```

이다.

실제 Gen2 관련 미검증 항목은 별도의 실기기 체크리스트로 계속 추적한다.

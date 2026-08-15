# Stage 0 — Meta DAT Setup

## 목적

Meta Wearables Device Access Toolkit(DAT)의 Android 개발 환경을 재현하고,
CameraAccess sample 및 Mock Device Kit을 실행하기 위한 준비 절차를 기록한다.

이 문서는 기능 요구사항이 아니라 **개발 환경 재현 문서**다.

---

## 1. 검증 당시 SDK 구성

검증 당시 기준:

- Meta Wearables DAT `0.9.0`
- Developer Preview
- Android
- `mwdat-core`
- `mwdat-camera`
- debug: `mwdat-mockdevice`

---

## 2. GitHub Package 인증

DAT dependency resolve를 위해 GitHub classic PAT에 `read:packages` 권한을 사용했다.

PAT는 다음 중 하나로 관리한다.

- 환경 변수
- `local.properties`

### 보안 원칙

- PAT를 source code에 하드코딩하지 않는다.
- PAT를 Git에 commit하지 않는다.

---

## 3. CameraAccess Sample 준비

기본 절차:

1. 공식 Android DAT 저장소 또는 CameraAccess sample을 clone한다.
2. GitHub classic PAT을 준비한다.
3. PAT를 환경 변수 또는 `local.properties`에 등록한다.
4. Gradle dependency를 resolve한다.
5. CameraAccess sample을 build한다.
6. Android 휴대폰에 설치하고 실행한다.

### 검증 완료

- [x] Android Studio에서 프로젝트 정상 open
- [x] `mwdat-core` resolve
- [x] `mwdat-camera` resolve
- [x] debug 환경 `mwdat-mockdevice` resolve
- [x] PAT가 source/Git에 포함되지 않음
- [x] CameraAccess sample APK build
- [x] Android 휴대폰에 설치
- [x] SDK initialization crash 없음

---

## 4. Mock Device Kit 준비

실제 Gen2 없이 Camera pipeline을 개발할 때 사용한다.

목표:

```text
테스트 영상
→ Mock Ray-Ban Meta
→ DAT
→ Android Camera Stream
```

Mock device 상태:

```text
Pair
→ Power On
→ Unfold
→ Don
```

테스트 영상은 필요 시 H.265 / HEVC로 변환할 수 있다.

예:

```bash
ffmpeg -i mouse_test.mp4 \
  -vf "scale=540:960" \
  -c:v libx265 -tag:v hvc1 -an \
  mouse_test_hevc.mov
```

### 검증 완료

- [x] MockDeviceKit 활성화
- [x] 가상 Ray-Ban Meta pair
- [x] Power On
- [x] Unfold
- [x] Don
- [x] Camera Feed에 테스트 video 지정
- [x] Camera session에서 Mock device 인식
- [x] Camera stream 시작
- [x] CameraAccess preview에서 테스트 영상 표시

---

## 5. 실제 Gen2 사용 전 준비

아래 항목은 기존 Mock 검증과 별개이며,
**실제 Meta Ray-Ban Gen2 대여 후 수행해야 하는 준비 항목**이다.

- [ ] 테스트 Android 휴대폰 준비
- [ ] Meta AI 앱 최신 버전 설치
- [ ] Gen2 firmware 최신화
- [ ] Gen2와 Meta AI 앱 pairing
- [ ] Developer Mode 활성화
- [ ] DAT sample APK 사전 build
- [ ] GitHub dependency 사전 download
- [ ] Camera permission 흐름 확인

실제 기기 연결 목표:

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

> 기존 문서에는 별도의 "휴대폰 인증" 절차가 검증 완료 항목으로 기록되어 있지 않다.
> 실제 Gen2 단계에서는 Meta AI pairing, DAT registration, Camera permission 흐름을 확인하도록 되어 있다.

---

## 6. 이 문서에서 다루지 않는 것

- Object Detection
- Attention Policy
- Backend Recognition
- OpenAI API
- 상품 Catalog
- 환급/환율
- Android UI

새 앱의 실제 Meta 연동 구조는 `docs/META_DAT_INTEGRATION.md`를 따른다.

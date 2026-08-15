# Meta DAT Integration

## 1. 목적

이 프로젝트에서는 Meta Ray-Ban Gen 2를 Android 앱의 카메라 입력 장치로 사용한다.

기존 Stage 0 검증 프로젝트에서 확인한 Meta Device Access Toolkit / CameraAccess 코드를 새 앱에 이식하고, 그 위에 Object Detection과 Backend Integration을 추가한다.

---

## 2. 기존 검증 자산에서 가져올 것

새 앱으로 이식할 대상:

- DAT SDK initialization
- DeviceSession 생성/종료
- Camera access 요청
- Stream start / stop
- frame callback
- mock device 연결 코드가 유용하면 debug용으로 유지
- Don / connection lifecycle 대응 중 검증된 부분

기존 Web Camera / OpenCLIP backend 코드는 이 앱으로 가져오지 않는다.

---

## 3. 목표 경계

Meta SDK frame callback 이후 앱 내부 표준 표현으로 변환한다.

예시 개념:

```text
DAT Frame
↓
App CameraFrame
- timestamp
- width
- height
- image / buffer
↓
ObjectDetectorAdapter
```

Object Detection과 관심 행동 정책이 Meta SDK의 구체적인 frame type에 직접 결합되지 않도록 adapter 경계를 둔다.

---

## 4. Lifecycle

목표 상태:

```text
쇼핑 시작
→ DeviceSession ready
→ Camera Stream START

쇼핑 종료
→ Camera Stream STOP
→ 필요 시 Session 정리
```

앱 lifecycle 변화:

```text
Foreground
→ 필요한 경우 stream 유지/복구

Background / Activity stop
→ SDK 요구사항에 맞춰 stream 정리
```

실제 SDK lifecycle 요구사항을 우선하며, 추측으로 session을 재사용하지 않는다.

---

## 5. Error State

최소 구분:

```text
NOT_CONNECTED
CONNECTING
READY
STREAMING
RECOVERABLE_ERROR
BLOCKING_ERROR
```

UI는 연결 실패 시 앱 crash 대신 사용자가 재시도할 수 있는 상태를 보여준다.

---

## 6. Frame 처리

Camera stream의 모든 frame을 서버에 보내지 않는다.

```text
DAT frame
→ local object detection
→ attention gate
→ trigger 순간 crop
→ backend
```

Object Detection 처리 속도가 Camera fps를 따라가지 못할 경우 최신 frame 우선 또는 throttling을 적용한다.

frame queue가 무한히 쌓여서는 안 된다.

---

## 7. 실기기 성공 기준

1. Gen 2 연결 후 앱에서 Camera frame을 수신한다.
2. 쇼핑 시작/종료에 따라 stream을 시작/중지한다.
3. 반복 start/stop 이후에도 재시작할 수 있다.
4. frame이 Object Detection adapter로 전달된다.
5. 네트워크/Recognition 오류가 Camera lifecycle을 깨뜨리지 않는다.

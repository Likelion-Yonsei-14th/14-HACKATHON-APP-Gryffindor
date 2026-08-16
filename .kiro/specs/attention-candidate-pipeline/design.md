# Design Document: Attention Candidate Pipeline (A3)

## Overview

기존 Overview 내용 유지.

## Architecture

기존 `# Architecture` 이하 내용 전체 유지.

### Technology Decisions

### New File Structure

## Components and Interfaces

기존 Components / Interfaces 관련 내용을 모두 이 섹션 아래로 이동.

### Component Responsibilities

### AttentionCandidateProvider

### AttentionPipeline

### ObjectTracker

### AttentionEvaluator

### SourceFrameCache

### CropGenerator

### JpegEncoder

## Data Models

기존 Data Models 내용을 모두 이 섹션 아래로 이동.

### AttentionCandidate

### TriggerType

### TrackedObject

### ActiveTrack

### DwellState

### AppConfig Extensions

## Detailed Data Flow

### Tracking Algorithm

### Attention Evaluation Flow

### Source Frame Cache Mechanism

### Source Frame Collection

### Bounded Cache

### Crop Generation

### JPEG Encoding

## Pipeline Lifecycle

### State Machine

### Lifecycle Integration

### AppContainer Wiring

### ShoppingViewModel Integration

## Core Processing Logic

### processDetectionFrame

### Duplicate Trigger Suppression

### Parallel Collection Architecture

## Coroutine and Threading Structure

기존 coroutine/background processing 설계 유지.

## Error Handling

기존 Error Handling 표와 정책 유지.

특히 다음 실패는 suppression을 commit하지 않는다.

* source frame miss
* crop failure
* JPEG failure
* candidate emission failure

## Correctness Properties

기존 Correctness Properties 전체 유지.

특히 다음 수정된 property를 유지한다.

* one-to-one track assignment
* exact source-frame timestamp match
* source-frame collector에서 A3-side `conflate()` 금지
* suppression은 AttentionCandidate 성공 emission 이후에만 commit
* candidate 생성 실패 후 retry 가능
* continuous attention event당 candidate 1회

## Testing Strategy

기존 Unit / Integration / Architecture Verification 내용을 유지.

반드시 검증:

* 동일 ActiveTrack에 한 frame의 detection 2개가 배정되지 않음
* `DetectionFrameResult.frameTimestampUs`와 동일한 `CameraFrame.timestampUs` 사용
* source frame miss 시 candidate 미생성 + suppression 미commit
* crop/JPEG 실패 후 다음 qualifying frame에서 retry 가능
* Bitmap width/height를 recycle 전에 확보
* stop/restart 시 tracking/dwell/suppression/frame cache 초기화

## Requirements Traceability

기존 R1~R12 traceability 표 유지.

## A3 Completion Boundary

```text
DetectionFrameResult
+ exact original CameraFrame
→ Lightweight Tracking
→ Center ROI
→ Occupancy
→ Dwell
→ Attention Trigger
→ Crop
→ JPEG
→ AttentionCandidate
```

A3 output:

```text
jpegBytes
capturedAt
triggerType
occupancyRatio
dwellMs
trackingId?
```

다음은 A4 범위이며 A3에서 구현하지 않는다.

```text
RemoteSessionRepository
RemoteShoppingRepository
AppContainer Fake → Real repository switch
Retrofit
multipart /recognize
Backend DTO
MATCHED → UI
UNKNOWN / AMBIGUOUS 처리
product/pricing mapping
```

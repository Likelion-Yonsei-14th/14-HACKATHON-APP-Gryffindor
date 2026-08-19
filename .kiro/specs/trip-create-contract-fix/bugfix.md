# Bugfix Requirements Document

## Introduction

Trip 생성 시 Android 앱이 사용자 입력을 그대로 Backend에 전송하여 HTTP 422 Unprocessable Entity 응답을 받는 버그 수정. Backend의 `TripCreateRequest` contract는 `destinationCountry`에 ISO 3166-1 alpha-2 코드(예: `KR`)를, `startsAt`/`endsAt`에 timezone offset 포함 ISO 8601 datetime(예: `2026-08-20T00:00:00+09:00`)을 요구하지만, 앱은 한글 국가명(`한국`)과 날짜만(`2026-08-20`) 전송하고 있다.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN user enters a Korean country name (e.g., "한국") in the country field THEN the system sends "한국" as destinationCountry to Backend and receives HTTP 422 because the value does not match `^[A-Z]{2}$`

1.2 WHEN user enters a lowercase country code (e.g., "kr") in the country field THEN the system sends "kr" as destinationCountry to Backend and receives HTTP 422 because the value does not match `^[A-Z]{2}$`

1.3 WHEN user enters a date string without timezone offset (e.g., "2026-08-20") in the startsAt field THEN the system sends "2026-08-20" as startsAt to Backend and receives HTTP 422 because the value lacks timezone offset

1.4 WHEN user enters a date string without timezone offset (e.g., "2026-08-23") in the endsAt field THEN the system sends "2026-08-23" as endsAt to Backend and receives HTTP 422 because the value lacks timezone offset

1.5 WHEN user enters endsAt earlier than startsAt (e.g., startsAt="2026-08-23", endsAt="2026-08-20") THEN the system sends both to Backend without validation and receives HTTP 422

1.6 WHEN user enters an invalid date format (e.g., "08-20-2026" or "2026/08/20") THEN the system sends the malformed string to Backend and receives HTTP 422

1.7 WHEN a 422 error occurs, RemoteTripRepository logs only the title field, making it difficult to diagnose which field caused the rejection

### Expected Behavior (Correct)

2.1 WHEN user enters a recognized Korean country name ("한국") in the country field THEN the system SHALL normalize it to "KR" before sending to Backend

2.2 WHEN user enters a lowercase country code ("kr") in the country field THEN the system SHALL normalize it to uppercase "KR" before sending to Backend

2.3 WHEN user enters a valid date (e.g., "2026-08-20") in the startsAt field THEN the system SHALL convert it to timezone-aware ISO 8601 "2026-08-20T00:00:00+09:00" before sending to Backend

2.4 WHEN user enters a valid date (e.g., "2026-08-23") in the endsAt field THEN the system SHALL convert it to timezone-aware ISO 8601 "2026-08-23T23:59:59+09:00" before sending to Backend

2.5 WHEN user enters endsAt earlier than startsAt THEN the system SHALL show a validation error and SHALL NOT call Backend

2.6 WHEN user enters an invalid date format (not parseable as YYYY-MM-DD) THEN the system SHALL show a validation error and SHALL NOT call Backend

2.7 WHEN user enters a country value that cannot be normalized to a valid 2-letter ISO code THEN the system SHALL show a validation error and SHALL NOT call Backend

2.8 WHEN createTrip is called, RemoteTripRepository SHALL log all request fields (title, destinationCity, destinationCountry, startsAt, endsAt)

2.9 WHEN country field placeholder is displayed THEN the system SHALL show "예: KR" instead of "예: 한국"

### Unchanged Behavior (Regression Prevention)

3.1 WHEN title is blank THEN the system SHALL CONTINUE TO show "여행 이름을 입력해주세요." validation error without calling Backend

3.2 WHEN all fields are blank except title THEN the system SHALL CONTINUE TO send null for destinationCity, destinationCountry, startsAt, endsAt (null values are accepted by Backend)

3.3 WHEN user enters a valid uppercase 2-letter country code (e.g., "KR", "JP", "US") THEN the system SHALL CONTINUE TO send it as-is to Backend

3.4 WHEN user enters both startsAt and endsAt with endsAt >= startsAt (both valid) THEN the system SHALL CONTINUE TO create trip successfully

3.5 WHEN trip creation succeeds THEN the system SHALL CONTINUE TO navigate to the created trip detail via onTripCreated callback

3.6 WHEN Backend returns an error other than 422 validation THEN the system SHALL CONTINUE TO display the error message in the UI

3.7 WHEN city field is filled THEN the system SHALL CONTINUE TO send it as destinationCity without transformation

3.8 WHEN only startsAt is provided without endsAt (or vice versa) THEN the system SHALL CONTINUE TO send the single date (converted to ISO 8601) and null for the other

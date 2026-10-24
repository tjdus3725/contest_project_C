# 동행 (Dongheng)

> 평소에는 가족에게 필요한 정차 장소를 찾고, 문제가 생기면 상황에 맞는 다음 행동과 도움 연결을 지원하는 **차량용 이동 케어 앱**
> Pleos Connect(현대차그룹 차량용 플랫폼) 대상 · Kotlin · Jetpack Compose · 가로 화면

## 화면 구성

좌측 레일: **홈 · 장소 · SOS(가운데 큰 빨간 버튼) · 가족 · MY**. 기존 '대처법' 탭은 SOS 안으로 합쳤고, 홈에서 미리 읽어볼 수 있게 연결했습니다.

| 탭 | 구현 내용 |
|---|---|
| 홈 | 동승자·목적지 선택으로 이동 시작, 남은 거리·예상 시간, 다음 정차, 휴식 간격 알림, 빠른 요청(화장실·기저귀·식사·쉬기), 출발·도착 점검표, 안전 가이드 바로가기 |
| 장소 찾기 | 동승자 조건 자동 적용, 필수/선호 시설 분리, 진행 방향 앞쪽 후보 3곳, 시설별 확인됨/미확인/없음, 출처·확인일, 경유지 연결 |
| SOS | 119·112·보험사·도로공사 발신 화면, 문장 입력 상황 분류, 위험 신호 질문(한 번에 하나), 한 화면 한 단계 안내, 신고용 요약, 동승자 응급 참고 정보 |
| 가족 | 동승자 프로필, 시설 조건, 휴식 간격, 준비물, 선택적 응급 정보·연락처, 정보 최신성 경고 |
| MY | 차량·보험 연락처, 내비 연결 상태, 위치 권한, 전체 삭제 |

## 설계 원칙 (기획안에서 지킨 것)

- 필수 시설 정보가 없으면 **충족으로 처리하지 않음**. 후보가 없어도 조건을 임의로 풀지 않고 범위 확대/조건 변경을 사용자가 선택.
- 추가 주행 시간은 데이터가 있을 때만 표시, 없으면 "계산 불가".
- 전화 버튼은 `ACTION_DIAL`로 발신 화면만 열고 "신고 완료"로 표시하지 않음("연결 여부 미확인" 명시).
- 위험 신호 키워드가 있으면 질문 없이 119 우선. 화재·침수는 항상 즉시 도움 권고.
- AI가 원인을 진단하거나 새 처치법을 만들지 않도록 **규칙 기반 + 고정 가이드**만 사용(오프라인 동작).
- 주행 중에는 가족·MY 편집 잠금, 큰 터치 영역(72dp 이상).
- 센서 연동 없이 아이 상태를 자동 감지하는 것처럼 표시하지 않음.

## 기획안에서 보완·삭제한 부분과 이유

| 변경 | 이유 |
|---|---|
| 홈의 '출발 준비 점검'과 '도착 준비'를 점검표 하나로 통합 | 두 기능이 같은 목록이라 중복 |
| '이용 후 수정 제보', '이용 기록', '도움말·오류 신고' 제외 | 서버가 있어야 의미가 있음. 시제품 범위 밖 → 후속 |
| '공유 범위 설정', 계정 동기화 제외 | 동의·서버 설계가 필요. 시제품은 기기 내 저장만 |
| '음성 설정', '알림 설정' 제외 | 음성 입력(SpeechToText SDK)을 붙일 때 함께 설계. 현재는 홈 화면 내 휴식 알림만 |
| '오프라인 자료' 설정 화면 제외 | 가이드가 앱에 내장되어 항상 오프라인 동작 → 설정이 불필요 |
| SOS의 '도로공사 콜센터(1588-2504)' 추가 | 고속도로 고장·사고 시 보험사와 별개로 필요한 연락처 |
| 후보를 '확인된 후보 3곳'과 '정보 미확인 후보'로 분리 | 미확인 시설을 추천 상위에 섞으면 오해 소지 |
| 필수 시설에서 '없음 확인'된 장소는 후보에서 제외 | 가는 길에 허탕치는 상황 방지 |

## 개발 환경

- Android API 28+ (compileSdk 34), Gradle 8.9, Kotlin 2.0.20, AGP 8.5.2, JDK 17
- Pleos Connect Emulator 2.1.0 (Automotive AVD, API 34) — [설정 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- NaviHelper SDK 3.0.5 (`ai.pleos.playground:NaviHelper`, Pleos Nexus 저장소)

```
gradle wrapper        # 최초 1회 (wrapper jar는 저장소에 포함하지 않음) 또는 Android Studio로 열기
./gradlew :app:assembleDebug
./gradlew :app:assembleDebug -PpleosNavi=false   # SDK 없이 가상 내비로 빌드
```

## 구조

```
app/src/main/kotlin/com/contest/dongheng
├─ data/   모델, 시연용 장소 데이터·추천 로직(PlaceRepository), 로컬 저장소(Store)
├─ sos/    상황 분류·가이드(Triage)
├─ navi/   NaviGateway 추상화 + 가상 내비
├─ ui/     AppState, 테마, 5개 화면
app/src/pleos/kotlin/.../PleosNaviGateway.kt   Pleos NaviHelper 연동(pleosNavi=true일 때만 컴파일)
```

## 현재 한계 (솔직하게)

- **이 코드는 작성 환경에 Android SDK/JDK 17이 없어 빌드·실행 검증을 하지 못했습니다.** Android Studio에서 열어 컴파일 오류를 먼저 확인해 주세요.
- `PleosNaviGateway`의 import 경로(`ai.pleos.playground.navi.data/constants`)는 공식 문서 예제에 명시되지 않아 추정값입니다. API Reference로 확인이 필요합니다. 문제가 있으면 `-PpleosNavi=false`로 나머지 앱을 먼저 확인할 수 있습니다.
- 경유지 반영 결과는 `NaviHelperEventListener` 콜백으로 확정되지만 아직 연결하지 않아 "요청 접수"까지만 표시합니다.
- 장소 데이터(좌표·거리·시설)는 **시연용 가상/근사 값**입니다. 실제 서비스는 공공데이터·운영사 API로 교체해야 합니다.
- 현재 위치 진행률은 시연용 슬라이더입니다(FusedLocation·Vehicle SDK 연동 후속). 위치는 Android LocationManager로 SOS 요약에만 사용합니다.
- 음성 입력(SpeechToText), 음성 안내(TextToSpeech)는 미구현. 대처 가이드는 전문가 검토 전 시안입니다.

<img text-align="center" width="800" height="220" alt="FreeBridgeLogo" src="https://github.com/user-attachments/assets/8517ed0b-c483-481d-af2e-56ca2bf14b53" />


> **프리랜서와 고용주의 '신뢰'를 잇는 올인원 프로젝트 매칭 플랫폼**
>
> 단순히 구인구직에서 끝나는 것이 아니라, **탐색 > 계약 > 프로젝트 수행 > 평가 > 포트폴리오 관리**까지 이어지는 프리랜서 비즈니스의 전체 생애주기를 체계적으로 지원합니다.

---
## 🔗Github Links
### <a href="https://github.com/20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-FE">FrontEnd Repository Link</a>
### <a href="https://github.com/20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file">Manifest Repository Link</a>
---

## 🚀 프로젝트 개요

### 1. 배경 및 문제 의식
현재 프리랜서 시장은 빠르게 성장하고 있지만, 구인구직 플랫폼에서 탐색한 이후의 **계약서 작성, 프로젝트 진행 관리, 정산 및 평가** 등의 과정은 여전히 파편화되어 진행되고 있습니다.

- **고용주:** 검증되고 적합한 인재를 찾기 어렵고, 프로젝트 진행 상황 파악 및 후속 정산 절차가 번거롭습니다.
- **프리랜서:** 자신의 경력, 포트폴리오, 이전 평가 이력을 체계적으로 관리하고 증명할 방법이 부족합니다.

### 2. 해결 방안 (차별화 포인트)
FreeBridge는 이 문제를 해결하기 위해 **계약 기반의 통합 관리 서비스**를 제공합니다.

- **생애주기 중심 UX:** 사용자의 현재 상태에 맞는 다음 행동을 안내하는 역할별 운영 허브(마이페이지) 제공
- **신뢰도 시스템:** 고용주의 솔직한 평가 점수와 이를 분석한 AI 리포트 제공
- **안전한 프로세스:** 계약서 생성/조회/서명부터 S3 기반의 안전한 포트폴리오 파일 관리 및 법률 AI 가이드 연계

---

## 🛠 기술 스택 (Tech Stack)

| 구분 | 기술 스택 |
| :--- | :--- |
| **Frontend** | <img src="https://img.shields.io/badge/Vue.js-4FC08D?style=flat&logo=vuedotjs&logoColor=white"/> <img src="https://img.shields.io/badge/TypeScript-3178C6?style=flat&logo=typescript&logoColor=white"/> <img src="https://img.shields.io/badge/Pinia-yellow?style=flat&logo=pinia&logoColor=white"/> <img src="https://img.shields.io/badge/Vite-646CFF?style=flat&logo=vite&logoColor=white"/> <img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat&logo=tailwindcss&logoColor=white"/> |
| **Backend** | <img src="https://img.shields.io/badge/Java-007396?style=flat&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Data_JPA-green?style=flat"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white"/> <img src="https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white"/> |
| **Tools** | <img src="https://img.shields.io/badge/GitHub-181717?style=flat&logo=github&logoColor=white"/> <img src="https://img.shields.io/badge/Jira-0052CC?style=flat&logo=jirasoftware&logoColor=white"/> <img src="https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white"/> |

---

## 🌟 핵심 기능 (Key Features)

| 공통 | 고용주 (Employer) | 프리랜서 (Freelancer) |
| :--- | :--- | :--- |
| **회원가입/온보딩** <br> (역할별 최적화) | **회사 프로필 관리** | **프로필/이력서/포트폴리오 관리** <br> (S3 연동) |
| **실시간 채팅** | **인재 탐색 & 지원 제안** | **계약 기반 프로젝트 관리** |
| **전자 계약 프로세스** <br> (생성, 조회, 서명, 완료) | **계약 & 정산 프로세스 관리** | **AI 기반 평가 분석 확인** <br> (고용주 리뷰 기반) |
| | **구독 관리 & 알림 배너** | |

---

## 🖥 화면 구성 (UI/UX)

| 페이지 | 화면 | 
| :---: | :---: |
| **회원가입** |<img width="600" height="550" alt="image" src="https://github.com/user-attachments/assets/fe125eb6-e1ee-4b2c-9cb6-be0560b5cc5d" />|
| 마이페이지 | *(여기에 이미지 또는 GIF를 넣어주세요)*|
| *(여기에페이지이름을넣어주세요)*| *(여기에 이미지 또는 GIF를 넣어주세요)*|
| *(여기에페이지이름을넣어주세요)*| *(여기에 이미지 또는 GIF를 넣어주세요)*|

---

## 🏗 시스템 아키텍처 (System Architecture)

- **Frontend (Vue 3)**: 마이페이지, 채팅, 계약/평가/포트폴리오 UI 및 클라이언트 사이드 로직 처리
- **Backend (Spring Boot)**: 사용자 관리, 계약, 리뷰, 구독, 파일 관리 등 각 도메인별 API 서버 및 Shared Layer
- **Storage**: Amazon S3를 이용한 프로필 이미지 및 대용량 포트폴리오 파일의 안전한 저장 및 관리

<img width="4409" height="2384" alt="image" src="https://github.com/user-attachments/assets/4586d765-3d1c-42f7-8ec2-a6b30c5d1d6c" />

---

## ERD
<img width="1607" height="1012" alt="image" src="https://github.com/user-attachments/assets/dee6f299-198a-431e-b86b-4707ded687ad" />


---

## 프로젝트 구조

```
Backend

freebridge
├─ app-main        # 실행 진입점
├─ common          # 공통 코드
├─ infra-common    # 인프라 연동
├─ user            # 사용자/마이페이지
├─ recruitment     # 채용/공고
├─ matchs          # 매칭
├─ contract        # 계약
├─ payment         # 결제
├─ settlement      # 정산
├─ review          # 리뷰
├─ chatting        # 채팅
├─ email           # 이메일
└─ subscription    # 구독

Front

src
├─ api             # API 통신
├─ assets          # 정적 파일
├─ components      # 공통 UI
├─ composables     # 재사용 로직
├─ constants       # 상수
├─ layouts         # 레이아웃
├─ router          # 라우팅
├─ stores          # 상태 관리
├─ tour            # 가이드 기능
├─ types           # 타입 정의
├─ utils           # 유틸
└─ views           # 페이지
   ├─ auth         # 인증
   ├─ domain       # 핵심 서비스 화면
   ├─ Guide        # 안내 페이지
   └─ onboarding   # 초기 안내

```
---
##  📄 프로젝트 문서
> [해당 프로젝트 문서 시트는 여기에서 확인하실 수 있습니다.](https://docs.google.com/spreadsheets/d/1bubX_mo95sQpQ4RLZaK4I_UPfS5rEU7S/edit?gid=15752498#gid=15752498)

## 📋 요구사항 정의 (Requirements)

프로젝트의 안정적인 구현을 위해 기능 및 비기능 요구사항을 상세히 정의하였습니다. 

[![Requirements](./asset/backend_requirements.png)](./assets/4팀_docs.xlsx - 요구사항 정의서.pdf)

---

## 🏗️ 빌드 및 배포

### 🔹 Architecture 다이어그램
<img src="./asset/architecture-diagram-team3.png" alt="Architecture-diagram"/>

### 🔹 Pipeline 빌드 과정
<img src="./asset/gif/1.pipe-line.gif">

### 🔹 ArgoCD 배포 동기화
<img src="./asset/gif/2.argocd.gif">

### 🔹 프론트엔드 화면
<img src="./asset/gif/3.todo.gif">

---

## 회고록

### 📝 정재우
이번 프로젝트를 하면서 느낀 점이 한두 가지가 아니지만, 가장 크게 와닿았던 건 역시 시간이었습니다.

처음 기획 단계에서는 2달이라는 시간이면 충분할 것이라 예상했다. 하지만 막상 프로젝트가 시작되고 나니 명절 연휴가 끼어들었고, 무언가를 진행할 때마다 크고 작은 딜레이가 발생했습니다.

CodeRabbit에게 코드 리뷰를 받는 시간, 기능이 예상대로 작동하지 않아 디버깅하는 시간, 혼자서는 진행할 수 없어 팀원을 기다려야 하는 시간까지. 어디서든, 어떤 방식으로든 장애물이 나타났고, 그때마다 일정은 
조금씩 뒤로 밀렸습니다. 결국 데드라인을 하루도 채 남기지 않고서야 프로젝트를 완성했고, 그 마지막 순간까지 꽤 오랜 시간 동안 압박감과 스트레스를 안고 달려야 했습니다. 

이 경험을 통해 일정 관리가 단순히 계획을 세우는 것이 아니라, 예측하지 못한 변수들을 얼마나 유연하게 흡수하느냐의 문제라는 것을 몸소 깨달았습니다.
기술적인 측면에서도 도전의 연속이었습니다. AI를 독립적인 서버로 띄우고 Spring에서 이를 호출하는 방식은 처음 시도해보는 구조였기에 낯설고 어려운 부분이 많았습니다. 데이터 동기화 문제로 예상치 못한 오류가 
발생하기도 했고, Timeout이 발생해 AI 결과값을 제때 불러오지 못하는 상황도 반복되었으며, 이런 기술적인 난관들이 겹치면서, 결국 초기 기획보다 서비스 규모를 일부 축소할 수밖에 없었습니다. 
처음 머릿속에 그렸던 기능들을 온전히 구현하지 못했다는 아쉬움은 지금도 남습니다.

그럼에도 불구하고, 이번 프로젝트는 분명히 값진 경험이었습니다. AI를 실제 서비스에 도입하는 것이 얼마나 많은 고민과 노력을 필요로 하는지 직접 체감했고, 그 과정에서 쌓은 경험은 앞으로 비슷한 작업을 할 때 
분명히 큰 자산이 될 것입니다. 힘들었지만, 그만큼 성장했다고 느끼는 프로젝트였습니다.

### 📝 이용우
이번 프로젝트를 하면서 여러 가지를 느끼고 배울 수 있었다.

초기에는 모든 일이 순조로웠고 시간도 많이 남아 그 전에 프로젝트에서 느꼈던 대로 생애주기에 대해 집중했고  “나중에 시간이 남으면 성능 개선에 더 집중해야지”라고 생각했다.

하지만 초반 세팅과 소통, 그리고 프로젝트 진행 중 역할에 따른 협업과 요청들이 계속 생기면서 해야 할 일이 갑자기 많아졌다. 시간이 부족해지자 AI의 도움을 받으며 개발을 진행했는데, 하나를 고치면 잘 되던 것이 안 되고, 다시 그것을 고치면 다른 부분이 문제가 되는 상황이 반복됐다.

마치 여름철 에어컨 밑에서 작은 이불을 서로 끌어당기며 자는 것처럼, 한 사람의 오류를 수정하면 다른 사람에게 영향을 주는 일이 비일비재했다.

그럼에도 불구하고 결국 프로젝트를 완성했다는 점에서 큰 기쁨을 느낀다. 다만, 성능 개선에 대해 깊이 고민해보고 싶은 부분이 많았는데 이를 충분히 해보지 못한 점은 아쉬움으로 남는다.

하지만 아쉬움이 남는다는 것은 아직 더 성장할 수 있다는 의미라고 생각한다. 부트캠프가 끝난 이후에도 계속 발전할 수 있도록 노력해야겠다.
6개월 동안 모두 수고 많으셨습니다.
다들 꼭 잘 돼서 저 밥 사주세요


### 📝 이형욱
회고록

### 📝 임재열
회고록

### 📝 윤홍석
이번 프로젝트에서는 여러 가지를 배울 수 있는 귀중한 경험이었다. 

초반 회의 단계에서 팀원들과 무한 회의 지옥에 빠져서 출구 없는 미로에 빠진 기분이 들었다. 또한 결국 극적인 합의를 통해 무언가에 동의하더라도 문서로 만들지 않으면 다음 회의에서 또다시 회의 지옥에 빠지기에 문서화의 중요함을 이번 기회에 배울 수 있었다. 

이 모든 난관을 거치고 개발을 시작한 후에는 이번에는 수많은 회사의 공고들에 어째서 협업 경험이 필수 요소 혹은 우대 사항으로 적혀있는지 뼈저리게 체감할 수 있었다. 분명 같은 언어로 같은 단어를 얘기했지만, 서로가 이해한 것이 다른 경우는 비일비재했고 서로 이런 기능은 당연히 있겠지라고 생각을 해서 의존성 문제가 생기거나 서로 본인 담당이라 생각해서 중복 개발이 되는 등 다양한 소통 문제가 있었고 이것이 협업이라는 것을 배울 훌륭한 기회였다. 

기술적으로는 이번에 결제와 계약서를 담당하게 되었는데, 결제를 가상 결제창을 띄우고 DB에서 숫자를 추가하고 빼기만 하기보다는 실제 결제 화면을 보여줄 수 있는 포트원 API를 사용하기로 했다. 다행히 포트원에서 MCP를 지원해 줘서 이번에 클로드 코드의 MCP에이전트 기능을 활용할 수 있었다. 에이전트가 포트원 관련 코드를 프론트에서 백엔드까지 다 이해하고 있었기에 어떤 형식으로 정보를 보내주고 받아오는지부터, 지금 있는 코드의 로직의 확인까지 가능해서 매우 편리했다. 

초반 기획보다는 규모가 축소되었지만 결국 완성을 했다는 사실에 만족감을 느낀다. 추후 시간이 있다면 꾸준히 개선을 해보고 싶다. 


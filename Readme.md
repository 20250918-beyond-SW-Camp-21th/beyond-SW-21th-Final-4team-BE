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
<img width="600" height="200" alt="image" src="https://github.com/user-attachments/assets/3cd3833d-f08f-4f9b-92dc-cb7c41aec67e" />


### 2. 해결 방안 (차별화 포인트)
FreeBridge는 이 문제를 해결하기 위해 **계약 기반의 통합 관리 서비스**를 제공합니다.

<img width="600" height="280" alt="image" src="https://github.com/user-attachments/assets/90de07de-58cf-45f3-a927-1799f1ca981c" />


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
| **공고** | **프로젝트 등록 및 AI ㅊ추천** | **기술스택, AI 후기를 통한 인재 서치 및 지원/제안** |
| **실시간 채팅** | **인재 탐색 & 지원 제안** | **계약 기반 프로젝트 관리** |
| **전자 계약 프로세스** <br> (생성, 조회, 서명, 완료) | **계약 & 정산 프로세스 관리** | **AI 기반 평가 분석 확인** <br> (고용주 리뷰 기반) |
| **생애주기 및 CRM** | **구독 관리 & 알림 배너** | **사용자 상태관리 및 행동 유도** |

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

![4팀_docs xlsx - 요구사항 정의서](https://github.com/user-attachments/assets/6d6402ec-a01a-4b6a-9c56-35d98c739eba)

---

## 🏗️ 빌드 및 배포

### 🔹 Architecture 다이어그램
<img width="4409" height="2384" alt="image" src="https://github.com/user-attachments/assets/4586d765-3d1c-42f7-8ec2-a6b30c5d1d6c" />

### 🔹 CICD 계획 흐름도

<img width="1513" height="830" alt="image" src="https://github.com/user-attachments/assets/45ade217-c092-4e15-a492-99154b977e6c" />

---

## WBS
![4팀_docs xlsx - WBS_page-0001](https://github.com/user-attachments/assets/a6f2ae99-6376-4475-ae54-514162614cd6)

## 🖥️ 운영 및 모니터링 전략

<img width="1099" height="468" alt="image" src="https://github.com/user-attachments/assets/32782c9b-67ef-45da-8060-aea02d7e0954" />
모니터링 체계는 배포 성공 여부를 단순히 Jenkins 결과로만 판단하지 않고, 실제 운영 중인 서비스 상태까지 함께 검증할 수 있게 해준다.
즉, FreeBridge의 CI/CD는 배포 자동화와 운영 모니터링이 결합된 형태로 구성되어 있다.

---
## ✅ 고객관리 시스템

### 프리랜서

<img width="1457" height="516" alt="image" src="https://github.com/user-attachments/assets/80c10116-9122-4290-8b0d-6d94a77b9c88" />

<img width="1392" height="990" alt="image" src="https://github.com/user-attachments/assets/b4163acd-c74c-44d4-9541-5e908826763c" />

### 고용주

<img width="1454" height="525" alt="image" src="https://github.com/user-attachments/assets/593c3d2b-07d0-4ae0-808b-085e1f035379" />

<img width="1394" height="988" alt="image" src="https://github.com/user-attachments/assets/a13c9db3-1b16-4544-96a7-03d7ee9adf4d" />

---

## 🔥 트러블슈팅

<img width="1370" height="566" alt="image" src="https://github.com/user-attachments/assets/101d4170-0de2-4230-b6ea-750b45cf8397" />

<img width="1369" height="553" alt="image" src="https://github.com/user-attachments/assets/d4b4c6e0-c6ff-4d07-b48e-2ce0a056fb57" />



## 🖥 화면 구성 (UI/UX)

| 페이지 | 화면 | 
| :---: | :---: |
| **회원가입** | <img width="600" height="550" alt="image" src="https://github.com/user-attachments/assets/fe125eb6-e1ee-4b2c-9cb6-be0560b5cc5d" />
| **마이페이지** | <img width="1896" height="939" alt="image" src="https://github.com/user-attachments/assets/0b212824-919e-4db4-ade8-029c75ab3603" /><img width="1896" height="937" alt="image" src="https://github.com/user-attachments/assets/2a271fb2-1201-4193-b05a-02e14c35b814" />||
| **공고**| <img width="1902" height="942" alt="image" src="https://github.com/user-attachments/assets/8eab244d-a945-45ac-8979-4505665694eb" />|
| **채팅** | <img width="1901" height="939" alt="image" src="https://github.com/user-attachments/assets/771b2039-5164-4fd8-8c91-ca4aa599fde6" />|
| **계약서**| <img width="1915" height="942" alt="image" src="https://github.com/user-attachments/assets/f4e77575-7e30-4d72-a87c-cab0113e27b1" />|
| **정산**| <img width="1914" height="938" alt="image" src="https://github.com/user-attachments/assets/b00e1b60-88ad-45d9-adeb-3de88c314ee0" />|
| **내 리뷰**| <img width="1901" height="940" alt="image" src="https://github.com/user-attachments/assets/cbadb717-a673-4564-a797-fbfb26044aeb" />|


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
프로젝트를 진행하며 일정관리의 중요성과 협업의 중요성을 배울 수 있는 시간이었습니다.
MyPage 구현과, CRM 생애주기를 담당하며 마이페이지 대시보드 서비스 제공과 CRM 플렛폼을 제공하는 개발자의 역할을 병행하며, 사용자 관점에서의 서비스 완성도를 높이는 경험을 할 수 있었습니다. 
또한 구현 과정에서 API 연동, 예외처리, 그리고 상태관리까지 고려하며 많은 요소를 세심하게 다루는 과정을 통해 사용자 입장에서 웹 서비스를 이용할 때 자연스러운 화면을 보이기까지 얼마나 많은 노력이 들어가는지와 설계 과정이 필요한지 배웠습니다.

다만 아쉬운 점은 기능 구현에 집중하다보니 일정 관리의 어려움을 느꼈습니다. 초반에는 절차 지향적인 방법으로 개발과 문서 정리를 여유롭게 진행하였다면, 후반부로 올 수록 시간에 쫓기어 일정이 많이 몰리고 마음이 급해지는 것을 경험했습니다. 일정을 타이트하게 잡자고 팀원들과 합의한 상태로 프로젝트에 임했음에도 불구하고, 막상 개발을 진행하다보니 갑작스럽게 발생하는 오류와 몇 가지의 기능 장애로 인해 일정이 밀리고 늦추어지는 상황에 초기에 얼마나 일정을 잘 맞추고 그 때 그 때 필요한 마감을 잘 지켜야한다는 것을 배웠습니다.
 
또한 개발보다 훨씬 더 중요한 것은 팀원과의 소통이라는 것을 느꼈습니다. 5명과 함께하는 프로젝트인 만큼 서로 다른 도메인 간 API 소통과 DB 공유, 로직연동과 도메인간 기능이 이어지는 흐름 정책 등 팀원 간 소통이 정말 중요하다는 것을 깨달았습니다. 특별히 다른 도메인의 기능과 결과를 읽어와 사용자에게 대시보드 기능을 제공하는 서비스를 만들어야 하는 입장으로 팀원들과 프로젝트 정책과 흐름에 대하여 정말 많은 시간을 회의하는데 사용했는데, 이를 통해 문서 정리의 중요성과 유용함을 배웠던 것 같습니다.

앞으로도 이번 경험을 바탕으로 더 완성도 높은 서비스와 더 나은 사용자 경험을 만들 수 있는 개발자가 되도록 더 노력해야겠다는 생각이 들었습니다.

### 📝 임재열
약 2개월 동안 최종 프로젝트를 진행하면서 정말 많은 것을 느꼈다. 일정 관리 계획도 나름 탄탄하게 세우고 실행했다고 생각했지만, 타이슨의 명언처럼 직접 해보기 전까지는 알 수 없는 것들이 많았다. 
시간은 항상 부족했고, 문제는 꼭 예상하지 못한 순간에 갑자기 생겼다. 초반에 너무 여유롭게 진행한 탓도 있었던 것 같다. 그래서 이번 프로젝트를 통해 마감일에 맞출 생각만 하지 말고, 할 수 있을 때 최대한 빨리 끝낼 생각부터 해야 한다는 교훈을 얻었다.


개발적으로도 깨닫고 느낀 점이 많았지만, 협업을 하면서 가장 크게 다가왔던 문제는 결국 소통이었다. 나는 사소한 수정사항이라고 생각해서 따로 공유하지 않고 사소한 api 라고 생각하여 혼자 수정하고 넘어간 적이 있었는데, 
그것이 나중에는 우리 팀원의 중요 로직이 엮여있어서 에러를 터트린적이 있다.
소통이 잘되지 않으니 자연스럽게 일정 관리에도 어려움이 생겼다. 일정 관리의 중요성을 다시금 크게 느꼈다. 


또 이번 프로젝트에서 인프라와 채팅 기능 등 담당하면서, 겉으로 보기에는 단순해 보여도 실제로 구현하고 안정적으로 운영하는 과정은 전혀 단순하지 않다는 것을 느꼈다. 인프라는 환경 설정이나 배포 과정에서 예상하지 못한 오류가 자주 발생했고, 채팅 기능 역시 실시간으로 데이터를 처리하다 보니 생각보다 더 많은 예외 상황과 세심한 관리가 필요했다. 

처음에는 금방 될 줄 알았던 부분들이 오히려 오래 걸리면서, 눈에 보이는 기능 구현만큼이나 그 기반을 안정적으로 만드는 일이 중요하다는 것을 배웠다.

아쉬운 점도 있다. 백엔드에서 성능 지표를 개선하는 작업을 꼭 해보고 싶었는데, 끝내 해보지 못한 것이 가장 아쉽게 남는다. 
지금 생각하니 회고를 쓰다 보면 다들 비슷한 이야기를 하는 것 같기도 하다. 
코드, 기술 구현상의 어려움, 협업 이야기 같은 것들 말이다. 나 역시 그런 부분에서 느낀 점이 많았지만, 
결국 가장 중요하다고 느낀 것은 건강이었다. 몸이 아프니까 정말 아무것도 할 수 없었고, 진행 상황을 따라가는 것조차 버거웠다. 프로젝트를 끝까지 해보니 실력도 중요하지만, 건강이 받쳐주지 않으면 아무것도 할 수 없다는 걸 많이 느꼈다.


마지막으로 프로젝트를 진행하는 동안 많은 도움을 주신 강사님, 매니저님, 멘토님 , 특히 부족한 나를 품어주며 협업이 뭔지 알려주고 , 프로젝트 진행하느냐 정말 많이 고생한 우리팀에게 감사를 표한다.
### 📝 윤홍석
이번 프로젝트에서는 여러 가지를 배울 수 있는 귀중한 경험이었다. 

초반 회의 단계에서 팀원들과 무한 회의 지옥에 빠져서 출구 없는 미로에 빠진 기분이 들었다. 또한 결국 극적인 합의를 통해 무언가에 동의하더라도 문서로 만들지 않으면 다음 회의에서 또다시 회의 지옥에 빠지기에 문서화의 중요함을 이번 기회에 배울 수 있었다. 

이 모든 난관을 거치고 개발을 시작한 후에는 이번에는 수많은 회사의 공고들에 어째서 협업 경험이 필수 요소 혹은 우대 사항으로 적혀있는지 뼈저리게 체감할 수 있었다. 분명 같은 언어로 같은 단어를 얘기했지만, 서로가 이해한 것이 다른 경우는 비일비재했고 서로 이런 기능은 당연히 있겠지라고 생각을 해서 의존성 문제가 생기거나 서로 본인 담당이라 생각해서 중복 개발이 되는 등 다양한 소통 문제가 있었고 이것이 협업이라는 것을 배울 훌륭한 기회였다. 

기술적으로는 이번에 결제와 계약서를 담당하게 되었는데, 결제를 가상 결제창을 띄우고 DB에서 숫자를 추가하고 빼기만 하기보다는 실제 결제 화면을 보여줄 수 있는 포트원 API를 사용하기로 했다. 다행히 포트원에서 MCP를 지원해 줘서 이번에 클로드 코드의 MCP에이전트 기능을 활용할 수 있었다. 에이전트가 포트원 관련 코드를 프론트에서 백엔드까지 다 이해하고 있었기에 어떤 형식으로 정보를 보내주고 받아오는지부터, 지금 있는 코드의 로직의 확인까지 가능해서 매우 편리했다. 

초반 기획보다는 규모가 축소되었지만 결국 완성을 했다는 사실에 만족감을 느낀다. 추후 시간이 있다면 꾸준히 개선을 해보고 싶다. 


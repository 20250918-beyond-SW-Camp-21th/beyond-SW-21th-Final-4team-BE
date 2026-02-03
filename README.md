## 📜 Convention

---

### 1. Git Flow (JIRA 연동 기준)

작업 시작 시 선행되어야 할 작업은 다음과 같습니다.

> JIRA Issue를 생성합니다. (Issue Type, Assignee, Sprint 설정)생성된 JIRA Issue Key를 기준으로 branch를 생성합니다. add → commit → push → pull request 를 진행합니다.pull request를 develop branch로 merge 합니다.이전에 merge된 작업이 있을 경우 다른 branch에서 진행하던 작업에 merge된 작업을 pull 받아옵니다.종료된 JIRA Issue의 상태를 Done으로 변경합니다.
>

### 2. Etc

준수해야 할 규칙은 다음과 같습니다.

> 🚫develop branch에서의 작업은 원칙적으로 금지합니다. 단, README 작성은 develop branch에서 수행합니다. commit, push, merge, pull request 등 모든 작업은 오류 없이 정상적으로 실행되는 지 확인 후 수행합니다.모든 commit / PR은 JIRA Issue Key와 반드시 연결되어야 합니다.
>

---

<details>
<summary style = " font-size:1.3em;">Branch</summary>
<div markdown="1">

### 1. Branch

branch는 작업 단위 & 기능 단위로 생성하며 JIRA Issue 네이밍을 기반으로 생성합니다. 네이밍 규칙을 따르되, 이슈는 Github에서 진행됩니다.

### 2. Branch Naming Rule (JIRA)

branch를 생성하기 전 Issue를 먼저 생성합니다.

생성된 **Issue** 와 domain 명을 조합하여 branch 이름을 결정합니다.

```
<Prefix>/<JIRA-Issue-Key>-<Domain>-<Description>
```

### 3. Prefix

- `main` : 개발이 완료된 산출물이 저장될 공간입니다.
- `develop`: feature branch에서 구현된 기능들이 merge될 default branch 입니다.
- `feature`: 기능을 개발하는 branch 입니다. 이슈 별 & 작업 별로 branch를 생성 후 기능을 개발하며 naming은 소문자를 사용합니다.

### 4. Etc

- `feature/PL-7-user-profile`
- `feature/PL-12-project-matching`

</div>
</details>

---

<details>
<summary style = " font-size:1.3em;">Issue</summary>
<div markdown="1">

### 1. Issue (JIRA)

작업 시작 전 **JIRA Issue 생성이 선행**되어야 합니다.

issue 는 Epic, Task 중 하나를 선택하여 생성하며, Task를 선택할 시, **Epic Issue 내에서 Sub-issue로 생성합니다.**

issue 제목에는 기능의 대표적인 설명을 적고,

description에는 세부적인 작업 내용과 완료 조건(DoD)을 작성합니다.

issue 생성 시 다음 항목을 필수로 설정합니다.

### 2. Issue Naming Rule

```
<JIRA-Issue-Key> [<Prefix>] <Domain>
```

- EPIC: 도메인을 기준으로 잡습니다.

  EPIC의 prefix는 아래와 같습니다.

    - BE : 백엔드
    - FE : 프론트엔드
    - DATA : 데이터처리
    - INFRA: 인프라
    - AI : Ai 도메인
    - 공통 : 공통 도메인

  예시)

  `JR-30 [공통] Common-Domain`

  `JR-18 [BE] user`

  `JR-3 [FE] userpage`

- TASK: Service/페이지 단위 issue

  TASK의 prefix는 아래와 같습니다.

    - **feat** : 새로운 기능 구현
    - **fix** : 코드 오류 수정
    - **del** : 쓸모없는 코드 삭제
    - **docs** : 문서 개정
    - **refactor** : 리팩터링
    - **chore** : 설정, 의존성, 환경 변경
    - **test** : 테스트 코드 작성
    - **style** : 코드 포맷, 주석, 줄바꿈

### 3. Etc

</div>
</details>

---

<details>
<summary style = " font-size:1.3em;">Commit</summary>
<div markdown="1">

### 1. Commit Message Convention (JIRA 연동)

```
<JIRA-Issue-Key> [<Prefix>] <Description>
```

- JIRA Issue Key를 포함하여 **자동 이슈 연동**을 활성화합니다.

### 예시

- **feat** : 새로운 기능 구현

  `PL-11 [feat] 구글 로그인 API 기능 구현`

- **fix** : 코드 오류 수정

  `PL-10 [fix] 회원가입 비즈니스 로직 오류 수정`

- **del** : 쓸모없는 코드 삭제

  `PL-12 [del] 불필요한 import 제거`

- **docs** : 문서 개정

  `PL-14 [docs] README 수정`

- **refactor** : 리팩터링

  `PL-15 [refactor] 코드 로직 개선`

- **chore** : 설정, 의존성, 환경 변경

  `PL-21 [chore] yml 수정`

  `PL-22 [chore] lombok 의존성 추가`

- **test** : 테스트 코드 작성

  `PL-20 [test] 로그인 API 테스트 코드 작성`

- **style** : 코드 포맷, 주석, 줄바꿈

</div>
</details>

---

<details>
<summary style = " font-size:1.3em;">Naming</summary>
<div markdown="1">

### 1. Pull Request

develop & main branch로 merge할 때에는 pull request가 필요합니다.

pull request는 반드시 **JIRA Issue와 연결**되어야 합니다.

### 2. Pull Request Naming Rule (JIRA)

```
<JIRA-Issue-Key> [<Prefix>] <Description>
```

### 3. Etc

`PL-3 [feat] 약속 잡기 API 구현`

`PL-5 [chore] Spring Data JPA 의존성 추가`

</div>
</details>

## 📜 Code Convention

---

<details>
<summary style = " font-size:1.3em;">Naming</summary>
<div markdown="1">

- 패키지 : 언더스코어(`_`)나 대문자를 섞지 않고 소문자를 사용하여 작성합니다.
- 클래스 : 클래스 이름은 명사나 명사절로 지으며, 대문자 카멜표기법(Upper camel case)을 사용합니다.
- 메서드 : 메서드 이름은 동사/전치사로 시작하며, 소문자 카멜표기법(Lower camel case)를 사용합니다. 의도가 전달되도록 최대한 간결하게 표현합니다.
- 변수 : 소문자 카멜표기법(Lower camel case)를 사용합니다.
- ENUM, 상수 : 상태를 가지지 않는 자료형이면서 `static final`로 선언되어 있는 필드일 때를 상수로 간주하며, 대문자와 언더스코어(Upper_snake_case)로 구성합니다.
- DB 테이블: 소문자와 언더스코어로(lower_snake_case) 구성합니다.
- 컬렉션(Collection): **복수형**을 사용하거나 **컬렉션을 명시합니다**. (Ex. userList, users, userMap)
- LocalDateTime: 접미사에 **Date**를 붙입니다.

</div>
</details>

---

<details>
<summary style = " font-size:1.3em;">Comment</summary>
<div markdown="1">

### 1. 한줄 주석은 // 를 사용한다.

```java
// 하이~

```

### 2. Bracket 사용 시 내부에 주석을 작성한다.

```java
/*
   하이~!
*/

```

### 3. 주요 함수에 대한 주석

```java
/*
 * 입력 : 인덱스:Long
 * 기능 : 유저 인덱스로 db에 접근해 유저 객체를 반환한다
 * 출력 : 유저:User
 */
public User getUser(Long idx)

```

</div>
</details>


---

<details>
<summary style = " font-size:1.3em;">Import</summary>
<div markdown="1">

### 1. 소스파일당 1개의 탑레벨 클래스를 담기

> 탑레벨 클래스(Top level class)는 소스 파일에 1개만 존재해야 한다. ( 탑레벨 클래스 선언의 컴파일타임 에러 체크에 대해서는 Java Language Specification 7.6 참조 )
>

### 2. static import에만 와일드 카드 허용

> 클래스를 import할때는 와일드카드(*) 없이 모든 클래스명을 다 쓴다. static import에서는 와일드카드를 허용한다.
>

### 3. 애너테이션 선언 후 새줄 사용

> 클래스, 인터페이스, 메서드, 생성자에 붙는 애너테이션은 선언 후 새줄을 사용한다. 이 위치에서도 파라미터가 없는 애너테이션 1개는 같은 줄에 선언할 수 있다.
>

### 4. 배열에서 대괄호는 타입 뒤에 선언

> 배열 선언에 오는 대괄호([])는 타입의 바로 뒤에 붙인다. 변수명 뒤에 붙이지 않는다.
>

### 5. `long`형 값의 마지막에 `L`붙이기

> long형의 숫자에는 마지막에 대문자 'L’을 붙인다. 소문자 'l’보다 숫자 '1’과의 차이가 커서 가독성이 높아진다.
>

</div>
</details>

---

<details>
<summary style = " font-size:1.3em;">URL</summary>
<div markdown="1">

### URL

URL은 RESTful API 설계 가이드에 따라 작성합니다.

- HTTP Method로 구분할 수 있는 get, put 등의 행위는 url에 표현하지 않습니다.
- 마지막에 `/` 를 포함하지 않습니다.
- `_` 대신 를 사용합니다.
- 소문자를 사용합니다.
- 확장자는 포함하지 않습니다.

</div>
</details>
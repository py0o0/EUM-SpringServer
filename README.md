# EUM : 외국인 맞춤형 플랫폼
## 프로젝트 소개
EUM-SpringServer는 외국인 맞춤형 플랫폼 EUM의 백엔드 서비스 중 제가 기여한 서비스들 입니다.

저는 **Community, Debate, Post 서비스(100%)** 를 단독으로 개발하였으며,
Gateway, Eureka, User 서비스에도 일부 기여했습니다.
<br></br>

## 프로젝트 목적
MSA(Microservice Architecture) 구조 학습 및 적용

JPA와 JPQL을 활용한 객체지향 쿼리 경험

비동기 처리(@Async, 스레드 활용) 경험

Spring 기반 외부 API 연동 경험
<br></br>

## 기술 스택
- **Backend**: Java, Spring Boot, Spring Security, Spring Data JPA
- **Database**: MySQL, Redis
- **Tooling**: JMeter, IntelliJ, Docker
<br></br>

## 백엔드 아키텍쳐
<img width="1498" height="567" alt="image" src="https://github.com/user-attachments/assets/4e785be7-f7a4-4b04-8ed7-70631eae2be8" />
<br></br>

## 시스템 구조

```
src
└── main
    ├── java
    |   ├── config             # 설정 클래스(Sequrity 등)
    |   ├── util               # 유틸성 클래스 (JWT 등)
    │   └── com.{project 명}
    │       ├── controller     # API 요청을 처리하는 컨트롤러
    │       ├── dto            # 요청/응답용 DTO
    │       ├── entity         # JPA 엔티티
    │       ├── repository     # DB 접근 레포지토리 (JPA 등)
    │       └── service        # 비즈니스 로직
    └── resources
        └── application.yml    # 애플리케이션 설정
.env                           # 민감정보 설정 (Git에는 업로드 하지 않음)
.gitignore                     # .env 등 업로드 제외 목록 설정
```
<br></br>

## 보안 및 인증

- JWT 기반 인증 시스템 구현
- 액세스 토큰과 리프레시 토큰 분리
- 비밀번호 암호화 저장 (BCrypt)
- CORS 설정을 통한 허용된 출처만 접근 가능
- Spring Security를 통한 엔드포인트 보호

## 얻은 점
- **환경 설정 보안성 강화**
    - 기존에는 `yml` 파일 전체를 업로드하지 않았으나, 보안 처리가 필요한 부분만 `secret 변수`로 분리하여 관리하는 방법을 익혔습니다.
    - 이를 통해 협업 시 환경 설정 공유가 가능해지고, 동시에 중요한 정보(계정, API Key 등)는 안전하게 보호할 수 있게 되었습니다.
- **성능 개선 경험**
    - JMeter를 활용하여 부하 테스트를 수행하고, 서비스 응답 시간 및 처리량을 측정하였습니다.
    - 비동기 처리 및 스레드를 적용하여 응답 속도를 개선하였습니다.

# Bonapetit

## 프로젝트 개요

### 같은 동네에 거주하거나 동일한 관심사와 취미를 가진 사람들을 효과적으로 연결하여 정보 교류와 공동 활동을 촉진하고, 이를 통해 개인의 사회적 네트워크를 확장하며 지역 사회 내에서의 소통과 공동체 활동을 활성화하는 것을 목표로 하는 커뮤니케이션 서비스.
### 사용자는 Kakao, Naver, Google 계정 등으로 간편하게 로그인할 수 있습니다.

## 기술 스택

- 프론트엔드 :: HTML, JavaScript, Thymeleaf, Vue, BootStrap
- 백엔드 :: Java, Spring Boot, Spring Data JPA, MyBatis
- 데이터베이스 :: MySql, Redis
- 기타 :: OAuth2 (Kakao, Naver, Google), WebSocket

## 기능 설명
- **팀 생성** :: 사용자는 자신이 원하는 주제와 지역을 바탕으로 팀을 생성할 수 있습니다.
- **팀 검색**: 팀 이름이나 지역을 바탕으로 다른 팀을 검색하고 가입할 수 있습니다.
- **OAuth2 소셜 로그인**: Kakao, Naver, Google 계정을 통해 간편하게 로그인할 수 있습니다.
- **실시간 채팅**: 팀원들과 실시간으로 대화를 주고받을 수 있습니다.
- **JWT 인증**: JWT를 사용하여 안전한 인증과 WebSocket 연결을 관리합니다.

## 설치 및 실행

### 요구 사항
- Java 17 이상
- Gradle
- MySQL
- Redis

### 환경 설정
application.yml 파일에서 데이터베이스와 Redis 등의 설정을 맞춰야 합니다.

### 사용 방법
1. **소셜 로그인을 통한 회원가입**
![20240912_171752](https://github.com/user-attachments/assets/6658ce89-d053-4c23-85a2-9df21066bf9d)
- 카카오, 네이버, 구글계정을 사용해 간편하게 회원가입을 할 수 있습니다.

2. **팀 생성 또는 가입**
![20240912_172724](https://github.com/user-attachments/assets/7d79c4e3-06cd-4bb6-a13f-1c423fe3c810)
- 새로운 팀을 생성하거나, 지역 기반으로 다른 팀을 검색하여 가입할 수 있습니다.

3. **가입된 팀 리스트 및 개설한 팀 리스트 조회**
   ![20240912_172932](https://github.com/user-attachments/assets/ff9a8988-e78a-49a5-9a0e-09d761edf98e)
- 프로필 조회로 가입된 팀 리스트를 확인합니다.

4. **실시간 채팅방 기능**
- ![20240912_173329](https://github.com/user-attachments/assets/f4b19a4b-076a-4bd9-bf89-75c7fad8d59e)


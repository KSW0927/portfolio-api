<h1 align="center">👋4년차 Java 개발자 김석원입니다.</h1>
<div align= "center">
    <h2 align="center" style="border-bottom: 1px solid #d8dee4; color: #282d33;">🛠️Tech Stacks</h2><br> 
    <div style="margin: 0 auto; align="center"> <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=Git&logoColor=white">
          <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=HTML5&logoColor=white">
          <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=Java&logoColor=white">
          <img src="https://img.shields.io/badge/Javascript-F7DF1E?style=for-the-badge&logo=Javascript&logoColor=white">
          <br/><img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=MariaDB&logoColor=white">
          <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=React&logoColor=white">
          <img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=Spring&logoColor=white">
    </div>
</div>

<h3 align="center">Spring Boot + React + JPA를 활용한 리디(RIDI) 클론코딩 토이 프로젝트입니다.</h3>

<br><br>

<h3 align="left">프로젝트 목표 및 개발 중점 사항</h3><br>
<p align="left">
    1. Back-end로는 서비스 기업의 표준인 <strong>Spring Boot</strong> 와 <strong>JPA</strong>를 활용하여 <strong>RESTful API</strong> 설계 및 ORM 기반의 개발 방식을 숙련하는 데 집중.<br>
    2. Front-end로는 React를 통해 컴포넌트 기반 UI 개발과 SPA 환경에서의 비동기 통신 경험.
</p><br>

<h3 align="left">개선 사항</h3><br>
<p align="left">
    1. 기존 코드의 리팩토링 및 보완(상시)<br>
    2. Swagger 연동(2025.10.25 완료)<br>
    3. 회원가입 및 Docker + Redis + JWT 를 활용한 로그인 및 로그아웃 기능(10.26 ~ 11.05 완료(기능 보완중))<br>
    4. 콘텐별 업로드 경로 분류 처리<br>
    5. 결제 모듈 적용(예정)
</p><br>

<h3 align="left">1. 전역 예외 처리 구현</h3><br>
이 프로젝트의 전역 예외 처리 구조는 저의 공공 SI 프로젝트 구축 및 운영 경험에서 비롯된 필요성에 의해 구현하였습니다.<br><br>

<strong>문제점:</strong><br>
다수의 개발자가 참여하는 SI 환경에서 개별적인 try-catch 블록 사용은 예외 처리 방식의 일관성을 크게 저하시켰습니다.<br><br>
이로 인해 개발 및 유지보수 과정에서 오류 디버깅 시간이 지연되고, 코드의 응집성이 저하되어 시스템 파악 및 수정에 어려움이 있었습니다.<br><br>

<strong>해결방안:</strong><br>
'예외 처리도 공통화 했으면 좋겠다' 라는 생각에서 시작해 설계하였으며 유지보수성과 확장성을 최우선으로 고려하여<br><br>
핸들러를 사용한 전역 예외 처리를 토이 프로젝트의 핵심 설계 목표 중 하나로 설정했습니다.

<br><br>

<h3 align="left">2. API 공통 응답 구조</h3><br>
전역 예외 처리 도입 후, API 응답에 대한 일관성을 확보하는 것이 다음 목표였습니다.<br><br>

<strong>문제점:</strong><br>
SI 프로젝트에서 API를 개발할 때, 응답 데이터 필드가 API마다 제각각이었습니다.<br><br>
예를 들어, 어떤 API는 핵심 데이터를 response.result로 반환했지만, 다른 API는 response.data로 반환하는 등 속성명 자체가 통일되지 않았습니다.<br><br>
이로 인해 클라이언트 개발 시 API를 호출할 때마다 응답 구조를 확인하고 그에 맞게 수정해야 하는 불편함과 비효율성이 발생했습니다.<br><br>

<strong>해결방안:</strong><br>
응답 구조 내의 code와 message 필드는 Enum을 활용하여 관리했습니다. <br>
이로써 클라이언트 개발 시 예측 가능한 통일된 응답 구조를 확보하여 프론트엔드 연동 효율성을 극대화했습니다.

<br><br>

<h3 align="left">3. Docker + Redis + JWT를 활용한 로그인/로그아웃 처리</h3><br>
이 구조는 기존에 느꼈던 서버 기반 세션의 확장성 및 성능 한계를 극복하고, 서비스 환경에 적합한 상태 비저장 기반 인증 시스템을 구축하고자 설계했습니다.<br><br>

<strong>문제점:</strong><br>
서버 기반 세션 방식은 로그인 시 빈번한 DB 접근 등으로 인증 처리 시간이 오래 걸리고, 서버 부하가 커서 확장에 불리했습니다.<br><br>

<strong>해결방안:</strong><br>
JWT를 도입하면서 발생할 수 있는 세션 무효화 문제(로그아웃)를 해결하고 성능을 최적화하기 위해 Redis를 통합 설계했습니다.<br><br>

Refresh Token 관리 : Refresh Token은 DB가 아닌 Redis에 저장했습니다. 이 구조를 통해 기존 DB 기반 세션 방식에서 발생할 수 있는 성능 병목 현상을 미연에 방지하고자 했습니다.<br><br>

로그아웃 처리: 프론트엔드가 세션 스토리지를 비우고 로그아웃을 요청하면, 백엔드에서는 RedisTemplate을 활용하여 Redis에 저장된 해당 Refresh Token을 명시적으로 삭제했습니다.<br><br>

Docker 통합: 개인 프로젝트이지만 실제 서비스 환경의 유연성과 확장성 기반을 마련하기 위해 Redis 서버는 Docker를 활용하여 컨테이너 환경에서 분리 구동했습니다.







    
    
    

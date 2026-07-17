<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="여행 일정과 방문 장소를 한곳에서 계획하는 Travel Planner">
    <title>${applicationName}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/layout.css">
</head>
<body>
<a class="skip-link" href="#main-content">본문 바로가기</a>

<%@ include file="common/header.jspf" %>

<main id="main-content" class="site-main">
    <section class="intro" id="service-introduction" aria-labelledby="intro-title">
        <div class="content-container intro__content">
            <p class="intro__eyebrow">TRAVEL PLANNER</p>
            <h1 id="intro-title">여행의 시작부터 마지막 일정까지</h1>
            <p class="intro__description">
                가고 싶은 장소를 찾고, 일정을 정리하고, 함께 여행할 사람과 계획을 나누는
                여행 플래닝 서비스를 준비하고 있습니다.
            </p>
        </div>
    </section>
</main>

<%@ include file="common/footer.jspf" %>
</body>
</html>

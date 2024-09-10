  function showSection(sectionId) {
      // 모든 섹션 숨김 처리
      document.getElementById('profileSection').style.display = 'none';
      document.getElementById('teamSection').style.display = 'none';
      document.getElementById('teamSectionOpen').style.display = 'none';

      // 선택한 섹션만 표시
      document.getElementById(sectionId).style.display = 'block';

      // 모든 사이드바 링크에서 'active' 클래스 제거
      const navLinks = document.querySelectorAll('.nav-link');
      navLinks.forEach(link => {
          link.classList.remove('active');
      });

      // 클릭된 링크에만 'active' 클래스 추가
      const clickedLink = document.querySelector(`[onclick="showSection('${sectionId}')"]`);
      if (clickedLink) {
          clickedLink.classList.add('active');
      }
  }

  document.getElementById('logoutButton').addEventListener('click', function() {
      // 세션에서 provider 정보 가져오기
      fetch('/get-provider')
          .then(response => response.json())
          .then(data => {
              const provider = data.provider;
              if (provider === 'kakao') {
                  window.location.href = '/kakao/logout';
              } else if (provider === 'naver') {
                  window.location.href = '/naver/logout';
              } else if (provider === 'google') {
                  window.location.href = '/google/logout';
              } else if (provider === 'local') {
                  window.location.href = '/logout';  // 일반 로그아웃 처리
              } else {
                  console.error('Unknown provider');
              }
          })
          .catch(error => console.error('Error fetching provider:', error));
  });
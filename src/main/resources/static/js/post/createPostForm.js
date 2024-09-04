//파일 폼 이미지 생성
document.getElementById('imageFiles').addEventListener('change', function(event) {
    const fileInput = event.target;
    const files = fileInput.files;
    const buttonsContainer = document.getElementById('buttonsContainer');
    buttonsContainer.innerHTML = ''; // 기존 버튼 제거

    // 선택된 파일 수만큼 버튼 및 미리보기 이미지 추가
    for (let i = 0; i < files.length; i++) {
        const file = files[i];

        // 파일 이름으로 버튼 생성
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = `파일 ${i + 1}: ${file.name}`;
        button.onclick = function() {
            alert(`파일 ${i + 1} 클릭됨: ${file.name}`);
        };

        // 파일 미리보기 이미지 생성
        const img = document.createElement('img');
        img.style.maxWidth = '100px'; // 미리보기 이미지 크기 조정
        img.style.maxHeight = '100px';
        const reader = new FileReader();
        reader.onload = function(e) {
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);

        // 버튼과 미리보기 이미지를 컨테이너에 추가
        buttonsContainer.appendChild(button);
        buttonsContainer.appendChild(img);
    }
});

function addFileInput() {
        // Check if the last file input is empty, don't add new input if it is
        const fileInputs = document.querySelectorAll('.file-input');
        if (fileInputs[fileInputs.length - 1].files.length === 0) {
            return;
        }

        // Create a new file input element
        const newFileInput = document.createElement('input');
        const newId = `imageFiles${fileInputs.length}`;
        newFileInput.type = 'file';
        newFileInput.name = 'imageFiles';
        newFileInput.className = 'file-input';
        newFileInput.id = newId;
        newFileInput.multiple = true;

        // Create a new label for the file input
        const newLabel = document.createElement('label');
        newLabel.htmlFor = newId;
        newLabel.textContent = '파일 업로드:';

        // Append the new label and file input to the container
        fileInputsContainer.appendChild(newLabel);
        fileInputsContainer.appendChild(newFileInput);
    }

    // Initially add one file input if there is only one present and empty
    if (document.querySelectorAll('.file-input').length === 1) {
        addFileInput();
    }
});

//해시태그 추가
function addHashtag() {
    var input = document.getElementById('hashtagInput');
    var hashtag = input.value.trim();

    if (!hashtag.startsWith('#')) {
        alert('해시태그는 #으로 시작해야 합니다.');
        return;
    }
    if (hashtag.length < 2) {
        alert('해시태그는 최소 한 글자 이상이어야 합니다.');
        return;
    }

    var li = document.createElement('li');
    li.textContent = hashtag;
    document.getElementById('hashtagList').appendChild(li);
    input.value = ''; // 입력 필드 초기화
    updateHiddenHashtagsField();
}

//추가할 때마다 리스트 하나씩 늘어나도록
function updateHiddenHashtagsField() {
    var hashtags = [];
    document.querySelectorAll('#hashtagList li').forEach(function(li) {
        hashtags.push(li.textContent);
    });
    document.getElementById('hashtagsField').value = hashtags.join(',');
}
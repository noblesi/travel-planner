// 모바일 화면에서 공통 사이드바를 열고 닫습니다.
document.querySelector('[data-sidebar-toggle]')?.addEventListener('click', () => {
    document.querySelector('.admin-sidebar')?.classList.toggle('open');
});

// API 연결 전 화면 동작과 저장 결과를 사용자에게 공통 토스트로 안내합니다.
const showAdminToast = (message) => {
    let toast = document.querySelector('.admin-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'admin-toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('show');
    window.setTimeout(() => toast.classList.remove('show'), 2600);
};

document.querySelectorAll('[data-pending-action]').forEach((button) => {
    button.addEventListener('click', () => {
        showAdminToast(`${button.dataset.pendingAction} 기능은 백엔드 저장 API 연결 후 사용할 수 있습니다.`);
    });
});

// 추천 점수 모달에서 사용하는 기본 가중치이며 합계는 100이어야 합니다.
const recommendationModal = document.getElementById('recommendation-rule-modal');
const recommendationForm = document.querySelector('[data-recommendation-form]');
const weightTotal = document.querySelector('[data-weight-total]');
const defaultWeights = { likes: 40, views: 20, saves: 40 };

const closeRecommendationModal = () => {
    if (!recommendationModal) return;
    recommendationModal.hidden = true;
    document.body.classList.remove('modal-open');
};

document.querySelector('[data-modal-open="recommendation-rule-modal"]')?.addEventListener('click', () => {
    recommendationModal.hidden = false;
    document.body.classList.add('modal-open');
    recommendationModal.querySelector('input')?.focus();
});

// 모달 바깥 영역과 ESC 키도 닫기 동작으로 처리해 접근성을 보완합니다.
recommendationModal?.querySelector('[data-modal-close]')?.addEventListener('click', closeRecommendationModal);
recommendationModal?.addEventListener('click', (event) => {
    if (event.target === recommendationModal) closeRecommendationModal();
});
window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && recommendationModal && !recommendationModal.hidden) closeRecommendationModal();
});

// 세 항목의 합계를 즉시 계산하고 유효하지 않은 경우 저장을 막습니다.
const updateWeightTotal = () => {
    if (!recommendationForm || !weightTotal) return 0;
    const total = [...recommendationForm.querySelectorAll('input[type="number"]')]
        .reduce((sum, input) => sum + (Number(input.value) || 0), 0);
    weightTotal.textContent = `합계 ${total}%${total === 100 ? '' : ' · 합계는 100%여야 합니다.'}`;
    weightTotal.classList.toggle('invalid', total !== 100);
    return total;
};

recommendationForm?.addEventListener('input', updateWeightTotal);
document.querySelector('[data-rule-reset]')?.addEventListener('click', () => {
    Object.entries(defaultWeights).forEach(([name, value]) => {
        recommendationForm.elements[name].value = value;
    });
    updateWeightTotal();
    showAdminToast('추천 점수 규칙을 기본값으로 초기화했습니다.');
});
recommendationForm?.addEventListener('submit', (event) => {
    event.preventDefault();
    if (updateWeightTotal() !== 100) return;
    closeRecommendationModal();
    showAdminToast('추천 점수 규칙이 저장되었습니다.');
});

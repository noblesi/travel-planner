import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PublicPlanDetailHeader from '@/components/plan/PublicPlanDetailHeader.vue'

const plan = {
  title: '서울 여행',
  authorName: '여행자',
  periodLabel: '2026.08.10 - 2026.08.11',
  viewCount: 10,
  likeCount: 3,
  liked: false,
}

describe('PublicPlanDetailHeader', () => {
  it('좋아요 요청 중 버튼을 비활성화하고 현재 상태를 접근성 속성으로 제공한다', async () => {
    const wrapper = mount(PublicPlanDetailHeader, {
      props: { plan, likePending: true },
    })

    const likeButton = wrapper.get('.like-stat')
    expect(likeButton.attributes()).toMatchObject({
      disabled: '',
      'aria-busy': 'true',
      'aria-pressed': 'false',
      'aria-label': '좋아요',
    })

    await likeButton.trigger('click')
    expect(wrapper.emitted('toggle-like')).toBeUndefined()
  })
})

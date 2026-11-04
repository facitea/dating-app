import { ref } from 'vue';
export const creditPromptOpen = ref(false);
export function showCreditPrompt() { creditPromptOpen.value = true; }
export function isInsufficientCredit(error) {
  const message = error?.response?.data?.message || '';
  return error?.response?.status === 409 && message.includes('크레딧') && message.includes('부족');
}

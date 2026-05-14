export function splitAnswer(value) {
  if (!value) return []
  return String(value)
    .split(/[,，、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function isAnswerCorrect(correctAnswerValue, userAnswerValue) {
  const correctAnswer = splitAnswer(correctAnswerValue).sort().join(',')
  const userAnswer = splitAnswer(userAnswerValue).sort().join(',')
  if (!correctAnswer) return null
  return correctAnswer === userAnswer
}

export function shouldAutoSubmitMultiple(correctAnswerValue, selectedAnswers) {
  const userAnswer = Array.isArray(selectedAnswers) ? selectedAnswers.join(',') : selectedAnswers
  return isAnswerCorrect(correctAnswerValue, userAnswer) === true
}

export function answerLabel(answer) {
  if (answer === 'TRUE') return '正确'
  if (answer === 'FALSE') return '错误'
  return answer || '-'
}
